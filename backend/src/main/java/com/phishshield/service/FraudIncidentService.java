package com.phishshield.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phishshield.domain.*;
import com.phishshield.dto.request.FraudIncidentRequest;
import com.phishshield.dto.response.ComplaintDraftResponse;
import com.phishshield.dto.response.FraudIncidentDto;
import com.phishshield.exception.BadRequestException;
import com.phishshield.exception.ForbiddenException;
import com.phishshield.exception.ResourceNotFoundException;
import com.phishshield.repository.FraudIncidentRepository;
import com.phishshield.repository.URLScanRepository;
import com.phishshield.repository.UserRepository;
import com.phishshield.util.SensitiveDataScrubber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class FraudIncidentService {

    private static final Logger log = LoggerFactory.getLogger(FraudIncidentService.class);

    private final FraudIncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final URLScanRepository urlScanRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FraudIncidentService(FraudIncidentRepository incidentRepository,
                                UserRepository userRepository,
                                URLScanRepository urlScanRepository,
                                NotificationService notificationService) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.urlScanRepository = urlScanRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public FraudIncidentDto createIncident(FraudIncidentRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        LocalDateTime incidentDt = request.getIncidentDateTime() != null 
                ? request.getIncidentDateTime() 
                : LocalDateTime.now();

        // 1. Validate & Link Source URL Scan if provided
        Long sourceScanId = request.getSourceScanId();
        String initialScanNotes = "";
        if (sourceScanId != null) {
            URLScan scan = urlScanRepository.findById(sourceScanId)
                    .orElseThrow(() -> new BadRequestException("Referenced source scan not found: " + sourceScanId));
            if (scan.getUser() != null && !scan.getUser().getId().equals(userId)) {
                throw new ForbiddenException("Cannot link an incident to another user's private scan.");
            }
            initialScanNotes = "Linked to URL Scan #" + scan.getId() + " (" + scan.getUrl() + ", Risk Score: " + scan.getRiskScore() + ")";
        }

        // 2. Compute severity if not specified
        IncidentSeverity severity = request.getSeverity();
        if (severity == null) {
            severity = computeSeverity(request.getIncidentType(), request.getAmountInvolved());
        }

        // 3. Scrub sensitive secrets, OTPs, PINs, card numbers from description & notes
        String cleanDescription = SensitiveDataScrubber.scrub(request.getDescription());
        String cleanEvidence = SensitiveDataScrubber.scrub(request.getEvidenceNotes());

        String checklistJson = request.getActionsChecklistJson();
        if (checklistJson == null || checklistJson.isBlank()) {
            checklistJson = generateDefaultChecklist(request.getIncidentType());
        }

        // 4. Initial Timeline Setup
        List<Map<String, String>> timeline = new ArrayList<>();
        Map<String, String> initEvent = new LinkedHashMap<>();
        initEvent.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        initEvent.put("author", user.getUsername());
        initEvent.put("action", "INCIDENT_CREATED");
        initEvent.put("notes", initialScanNotes.isEmpty() ? "Incident reported by citizen." : initialScanNotes);
        timeline.add(initEvent);

        String initialTimelineJson = writeTimelineJson(timeline);

        FraudIncident incident = FraudIncident.builder()
                .user(user)
                .incidentType(request.getIncidentType())
                .title(request.getTitle())
                .description(cleanDescription)
                .incidentDateTime(incidentDt)
                .amountInvolved(request.getAmountInvolved())
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .severity(severity)
                .sourceScanId(sourceScanId)
                .relatedTransactionIds(request.getRelatedTransactionIds())
                .bankOrProviderContacted(request.getBankOrProviderContacted())
                .complaintReference(request.getComplaintReference())
                .bankTicketReference(request.getBankTicketReference())
                .status(request.getStatus() != null ? request.getStatus() : IncidentStatus.ACTION_REQUIRED)
                .actionsChecklistJson(checklistJson)
                .timelineJson(initialTimelineJson)
                .evidenceNotes(cleanEvidence)
                .nextFollowUpDate(request.getNextFollowUpDate())
                .build();

        FraudIncident saved = incidentRepository.save(incident);
        log.info("Registered fraud incident #{} (severity: {}) for user {}", saved.getId(), saved.getSeverity(), userId);

        if (notificationService != null) {
            try {
                notificationService.createIncidentNotification(user, saved);
            } catch (Exception e) {
                log.warn("Failed to create incident notification for user {}: {}", userId, e.getMessage());
            }
        }

        return FraudIncidentDto.fromEntity(saved);
    }

    public Page<FraudIncidentDto> getUserIncidents(Long userId, Pageable pageable, IncidentStatus status) {
        Page<FraudIncident> page;
        if (status != null) {
            page = incidentRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            page = incidentRepository.findByUserId(userId, pageable);
        }
        return page.map(FraudIncidentDto::fromEntity);
    }

    public Page<FraudIncidentDto> getAllIncidentsAdmin(Pageable pageable, IncidentStatus status, IncidentSeverity severity) {
        Page<FraudIncident> page;
        if (status != null && severity != null) {
            page = incidentRepository.findByStatusAndSeverity(status, severity, pageable);
        } else if (status != null) {
            page = incidentRepository.findByStatus(status, pageable);
        } else if (severity != null) {
            page = incidentRepository.findBySeverity(severity, pageable);
        } else {
            page = incidentRepository.findAll(pageable);
        }
        return page.map(FraudIncidentDto::fromEntity);
    }

    public FraudIncidentDto getIncidentById(Long id, Long userId) {
        FraudIncident incident = incidentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
        return FraudIncidentDto.fromEntity(incident);
    }

    @Transactional
    public FraudIncidentDto updateIncident(Long id, FraudIncidentRequest request, Long userId) {
        FraudIncident incident = incidentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));

        if (request.getTitle() != null) incident.setTitle(request.getTitle());
        if (request.getDescription() != null) {
            incident.setDescription(SensitiveDataScrubber.scrub(request.getDescription()));
        }
        if (request.getAmountInvolved() != null) incident.setAmountInvolved(request.getAmountInvolved());
        if (request.getSeverity() != null) incident.setSeverity(request.getSeverity());
        if (request.getBankOrProviderContacted() != null) incident.setBankOrProviderContacted(request.getBankOrProviderContacted());
        if (request.getComplaintReference() != null) incident.setComplaintReference(request.getComplaintReference());
        if (request.getBankTicketReference() != null) incident.setBankTicketReference(request.getBankTicketReference());
        if (request.getStatus() != null && request.getStatus() != incident.getStatus()) {
            appendTimelineEvent(incident, "user", "STATUS_CHANGED", "Status changed to " + request.getStatus());
            incident.setStatus(request.getStatus());
        }
        if (request.getActionsChecklistJson() != null) incident.setActionsChecklistJson(request.getActionsChecklistJson());
        if (request.getEvidenceNotes() != null) {
            incident.setEvidenceNotes(SensitiveDataScrubber.scrub(request.getEvidenceNotes()));
        }
        if (request.getNextFollowUpDate() != null) incident.setNextFollowUpDate(request.getNextFollowUpDate());

        FraudIncident saved = incidentRepository.save(incident);
        return FraudIncidentDto.fromEntity(saved);
    }

    @Transactional
    public FraudIncidentDto updateChecklist(Long id, String checklistJson, Long userId) {
        FraudIncident incident = incidentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));

        incident.setActionsChecklistJson(checklistJson);
        appendTimelineEvent(incident, "user", "CHECKLIST_UPDATED", "Containment checklist progress updated");
        return FraudIncidentDto.fromEntity(incidentRepository.save(incident));
    }

    @Transactional
    public FraudIncidentDto addIncidentNote(Long id, String note, Long userId) {
        FraudIncident incident = incidentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));

        String cleanNote = SensitiveDataScrubber.scrub(note);
        appendTimelineEvent(incident, "user", "NOTE_ADDED", cleanNote);
        return FraudIncidentDto.fromEntity(incidentRepository.save(incident));
    }

    @Transactional
    public FraudIncidentDto adminUpdateStatus(Long id, IncidentStatus newStatus, IncidentSeverity newSeverity, String adminNotes, String adminUsername) {
        FraudIncident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));

        if (newStatus != null) {
            incident.setStatus(newStatus);
        }
        if (newSeverity != null) {
            incident.setSeverity(newSeverity);
        }

        String noteDesc = "Admin triage update: status=" + incident.getStatus() + ", severity=" + incident.getSeverity();
        if (adminNotes != null && !adminNotes.isBlank()) {
            noteDesc += ". Notes: " + SensitiveDataScrubber.scrub(adminNotes);
        }
        appendTimelineEvent(incident, adminUsername, "ADMIN_TRIAGE_UPDATE", noteDesc);

        FraudIncident saved = incidentRepository.save(incident);
        log.info("Admin {} updated incident #{} status to {}", adminUsername, id, saved.getStatus());

        if (notificationService != null && saved.getUser() != null) {
            try {
                notificationService.createIncidentNotification(saved.getUser(), saved);
            } catch (Exception e) {
                log.warn("Failed to create incident update notification for user {}: {}", saved.getUser().getId(), e.getMessage());
            }
        }

        return FraudIncidentDto.fromEntity(saved);
    }

    @Transactional
    public void deleteIncident(Long id, Long userId) {
        FraudIncident incident = incidentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
        incidentRepository.delete(incident);
        log.info("User {} safely removed incident record #{}", userId, id);
    }

    public ComplaintDraftResponse generateComplaintDraft(Long id, Long userId) {
        FraudIncident incident = incidentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));

        User user = incident.getUser();
        String citizenName = user.getFullName() != null ? user.getFullName() : user.getUsername();
        String dateStr = incident.getIncidentDateTime().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"));
        String amountStr = incident.getAmountInvolved() != null 
                ? "INR " + incident.getAmountInvolved().toPlainString() + " " + incident.getCurrency()
                : "Not specified";

        StringBuilder draft = new StringBuilder();
        draft.append("TO:\n");
        draft.append("The Officer In-Charge / Cyber Crime Cell\n");
        draft.append("National Cyber Crime Reporting Portal (cybercrime.gov.in) / Bank Fraud Department\n\n");
        draft.append("SUBJECT: Formal Complaint Regarding ").append(formatIncidentTypeName(incident.getIncidentType()))
             .append(" — Incident Ref: #PRH-INC-").append(incident.getId()).append("\n\n");
        draft.append("Respected Sir/Madam,\n\n");
        draft.append("I, ").append(citizenName).append(" (Username: ").append(user.getUsername())
             .append(", Email: ").append(user.getEmail()).append("), wish to lodge a formal cyber fraud complaint regarding an incident that occurred on ")
             .append(dateStr).append(".\n\n");

        draft.append("INCIDENT PARTICULARS:\n");
        draft.append("1. Incident Category: ").append(formatIncidentTypeName(incident.getIncidentType())).append("\n");
        draft.append("2. Severity Level: ").append(incident.getSeverity()).append("\n");
        draft.append("3. Title: ").append(incident.getTitle()).append("\n");
        draft.append("4. Approximate Amount Involved: ").append(amountStr).append("\n");
        if (incident.getSourceScanId() != null) {
            draft.append("5. Associated Prahar Scan ID: #").append(incident.getSourceScanId()).append("\n");
        }
        if (incident.getBankOrProviderContacted() != null && !incident.getBankOrProviderContacted().isBlank()) {
            draft.append("6. Bank / Service Provider Contacted: ").append(incident.getBankOrProviderContacted()).append("\n");
        }
        if (incident.getBankTicketReference() != null && !incident.getBankTicketReference().isBlank()) {
            draft.append("7. Bank Ticket / Service Ref: ").append(incident.getBankTicketReference()).append("\n");
        }
        if (incident.getRelatedTransactionIds() != null && !incident.getRelatedTransactionIds().isBlank()) {
            draft.append("8. Transaction / UTR Reference: ").append(incident.getRelatedTransactionIds()).append("\n");
        }
        draft.append("\nINCIDENT CHRONOLOGY & FACTS:\n");
        draft.append(incident.getDescription() != null && !incident.getDescription().isBlank() ? incident.getDescription() : "Details provided as per attached records.").append("\n\n");

        if (incident.getEvidenceNotes() != null && !incident.getEvidenceNotes().isBlank()) {
            draft.append("AVAILABLE EVIDENCE & ATTACHMENTS SUMMARY:\n");
            draft.append(incident.getEvidenceNotes()).append("\n\n");
        }

        draft.append("PRAYER / REQUESTED REMEDIAL ACTION:\n");
        draft.append("1. Please register this complaint under relevant sections of the Information Technology Act, 2000 and BNS/IPC.\n");
        draft.append("2. Issue a formal freeze request on the beneficiary account / UPI handle / wallet to mitigate financial loss.\n");
        draft.append("3. Direct the concerned payment intermediary to reverse unauthorized debits under RBI Limiting Customer Liability Circulars.\n\n");
        draft.append("Yours sincerely,\n");
        draft.append(citizenName).append("\nDate: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))).append("\n");

        List<String> guidanceSteps = Arrays.asList(
                "Call 1930 immediately (National Cyber Crime Reporting Helpline, India MHA) within 2-4 golden hours.",
                "Lodge online complaint at https://cybercrime.gov.in using the complaint draft generated above.",
                "Notify your issuing bank's 24x7 fraud desk to block UPI/ATM debit cards and request a lien freeze on fraudulent UTRs.",
                "If passwords or OTPs were disclosed, immediately reset online banking credentials from an uncompromised device.",
                "NOTICE: Prahar is an advisory guidance tool. Prahar does not freeze accounts or file police reports on your behalf. Official reports must be lodged directly with authorities."
        );

        return ComplaintDraftResponse.builder()
                .incidentId(incident.getId())
                .incidentType(incident.getIncidentType().name())
                .incidentTitle(incident.getTitle())
                .citizenName(citizenName)
                .complaintDraftText(draft.toString())
                .suggestedSubject("Cyber Fraud Complaint: " + formatIncidentTypeName(incident.getIncidentType()) + " - Ref #PRH-INC-" + incident.getId())
                .officialPortals(Arrays.asList("https://cybercrime.gov.in", "Toll Free: 1930 (MHA Cybercrime Helpline)"))
                .guidanceSteps(guidanceSteps)
                .build();
    }

    private void appendTimelineEvent(FraudIncident incident, String author, String action, String notes) {
        List<Map<String, String>> timeline = parseTimelineJson(incident.getTimelineJson());
        Map<String, String> event = new LinkedHashMap<>();
        event.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        event.put("author", author);
        event.put("action", action);
        event.put("notes", notes);
        timeline.add(event);
        incident.setTimelineJson(writeTimelineJson(timeline));
    }

    private List<Map<String, String>> parseTimelineJson(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse incident timeline JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String writeTimelineJson(List<Map<String, String>> timeline) {
        try {
            return objectMapper.writeValueAsString(timeline);
        } catch (Exception e) {
            log.warn("Failed to serialize incident timeline JSON: {}", e.getMessage());
            return "[]";
        }
    }

    private IncidentSeverity computeSeverity(IncidentType type, BigDecimal amount) {
        if (amount != null && amount.compareTo(new BigDecimal("50000")) >= 0) {
            return IncidentSeverity.CRITICAL;
        }
        if (type == IncidentType.SIM_SWAP || type == IncidentType.CREDENTIALS_LEAKED) {
            return IncidentSeverity.CRITICAL;
        }
        if (amount != null && amount.compareTo(new BigDecimal("10000")) >= 0) {
            return IncidentSeverity.HIGH;
        }
        if (type == IncidentType.UNAUTHORIZED_DEBIT || type == IncidentType.UPI_FRAUD || type == IncidentType.ATM_CARD_FRAUD) {
            return IncidentSeverity.HIGH;
        }
        return IncidentSeverity.MEDIUM;
    }

    private String generateDefaultChecklist(IncidentType type) {
        List<Map<String, Object>> steps = new ArrayList<>();
        switch (type) {
            case UNAUTHORIZED_DEBIT:
            case UPI_FRAUD:
            case ATM_CARD_FRAUD:
                steps.add(createStep("1", "Call 1930 Citizen Financial Cyber Fraud Helpline immediately", true));
                steps.add(createStep("2", "Lock ATM Card / Disable UPI payments via Mobile Banking App", false));
                steps.add(createStep("3", "Call issuing Bank branch & request Lien on fraudulent Beneficiary account", false));
                steps.add(createStep("4", "Download bank statement showing transaction UTR reference", false));
                steps.add(createStep("5", "Lodge formal report at cybercrime.gov.in with UTR details", false));
                break;
            case OTP_DISCLOSURE:
            case CREDENTIALS_LEAKED:
                steps.add(createStep("1", "Immediately change internet banking & UPI application passwords", true));
                steps.add(createStep("2", "Log out of all active web & mobile banking sessions", false));
                steps.add(createStep("3", "Notify bank fraud desk about the compromised credential", false));
                steps.add(createStep("4", "Monitor transaction activity closely for the next 72 hours", false));
                break;
            case PHISHING_CLICKED:
                steps.add(createStep("1", "Disconnect Wi-Fi / Mobile Data immediately", true));
                steps.add(createStep("2", "Clear browser cache, cookies, and local storage", false));
                steps.add(createStep("3", "Scan device with an authorized anti-malware security tool", false));
                steps.add(createStep("4", "Reset passwords for accounts accessed on that device", false));
                break;
            default:
                steps.add(createStep("1", "Document incident date, time, and suspect details", true));
                steps.add(createStep("2", "File an acknowledgment report on cybercrime.gov.in", false));
                steps.add(createStep("3", "Notify relevant financial or service providers", false));
                break;
        }

        try {
            return objectMapper.writeValueAsString(steps);
        } catch (Exception e) {
            return "[]";
        }
    }

    private Map<String, Object> createStep(String id, String text, boolean highPriority) {
        Map<String, Object> step = new LinkedHashMap<>();
        step.put("id", id);
        step.put("text", text);
        step.put("completed", false);
        step.put("highPriority", highPriority);
        return step;
    }

    private String formatIncidentTypeName(IncidentType type) {
        if (type == null) return "Cyber Security Incident";
        return type.name().replace('_', ' ');
    }
}
