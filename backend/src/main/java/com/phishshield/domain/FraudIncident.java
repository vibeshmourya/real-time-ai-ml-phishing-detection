package com.phishshield.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "fraud_incidents",
    indexes = {
        @Index(name = "idx_incidents_user", columnList = "user_id"),
        @Index(name = "idx_incidents_type", columnList = "incident_type"),
        @Index(name = "idx_incidents_status", columnList = "status"),
        @Index(name = "idx_incidents_severity", columnList = "severity"),
        @Index(name = "idx_incidents_date", columnList = "incident_date_time")
    }
)
public class FraudIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", length = 50, nullable = false)
    private IncidentType incidentType;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "incident_date_time", nullable = false)
    private LocalDateTime incidentDateTime;

    @Column(name = "amount_involved", precision = 12, scale = 2)
    private BigDecimal amountInvolved;

    @Column(name = "currency", length = 10)
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 30, nullable = false)
    private IncidentSeverity severity = IncidentSeverity.MEDIUM;

    @Column(name = "source_scan_id")
    private Long sourceScanId;

    @Column(name = "related_transaction_ids", length = 255)
    private String relatedTransactionIds;

    @Column(name = "bank_or_provider_contacted", length = 100)
    private String bankOrProviderContacted;

    @Column(name = "complaint_reference", length = 100)
    private String complaintReference;

    @Column(name = "bank_ticket_reference", length = 100)
    private String bankTicketReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 40, nullable = false)
    private IncidentStatus status = IncidentStatus.ACTION_REQUIRED;

    @Column(name = "actions_checklist_json", columnDefinition = "TEXT")
    private String actionsChecklistJson;

    @Column(name = "timeline_json", columnDefinition = "TEXT")
    private String timelineJson;

    @Column(name = "evidence_notes", columnDefinition = "TEXT")
    private String evidenceNotes;

    @Column(name = "next_follow_up_date")
    private LocalDate nextFollowUpDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public FraudIncident() {}

    public FraudIncident(Long id, User user, IncidentType incidentType, String title, String description,
                         LocalDateTime incidentDateTime, BigDecimal amountInvolved, String currency,
                         IncidentSeverity severity, Long sourceScanId,
                         String relatedTransactionIds, String bankOrProviderContacted, String complaintReference,
                         String bankTicketReference, IncidentStatus status, String actionsChecklistJson,
                         String timelineJson, String evidenceNotes, LocalDate nextFollowUpDate,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.incidentType = incidentType;
        this.title = title;
        this.description = description;
        this.incidentDateTime = incidentDateTime;
        this.amountInvolved = amountInvolved;
        this.currency = currency != null ? currency : "INR";
        this.severity = severity != null ? severity : IncidentSeverity.MEDIUM;
        this.sourceScanId = sourceScanId;
        this.relatedTransactionIds = relatedTransactionIds;
        this.bankOrProviderContacted = bankOrProviderContacted;
        this.complaintReference = complaintReference;
        this.bankTicketReference = bankTicketReference;
        this.status = status != null ? status : IncidentStatus.ACTION_REQUIRED;
        this.actionsChecklistJson = actionsChecklistJson;
        this.timelineJson = timelineJson;
        this.evidenceNotes = evidenceNotes;
        this.nextFollowUpDate = nextFollowUpDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private User user;
        private IncidentType incidentType;
        private String title;
        private String description;
        private LocalDateTime incidentDateTime;
        private BigDecimal amountInvolved;
        private String currency = "INR";
        private IncidentSeverity severity = IncidentSeverity.MEDIUM;
        private Long sourceScanId;
        private String relatedTransactionIds;
        private String bankOrProviderContacted;
        private String complaintReference;
        private String bankTicketReference;
        private IncidentStatus status = IncidentStatus.ACTION_REQUIRED;
        private String actionsChecklistJson;
        private String timelineJson;
        private String evidenceNotes;
        private LocalDate nextFollowUpDate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder incidentType(IncidentType type) { this.incidentType = type; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder incidentDateTime(LocalDateTime dt) { this.incidentDateTime = dt; return this; }
        public Builder amountInvolved(BigDecimal amount) { this.amountInvolved = amount; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder severity(IncidentSeverity severity) { this.severity = severity; return this; }
        public Builder sourceScanId(Long sourceScanId) { this.sourceScanId = sourceScanId; return this; }
        public Builder relatedTransactionIds(String ids) { this.relatedTransactionIds = ids; return this; }
        public Builder bankOrProviderContacted(String bank) { this.bankOrProviderContacted = bank; return this; }
        public Builder complaintReference(String ref) { this.complaintReference = ref; return this; }
        public Builder bankTicketReference(String ref) { this.bankTicketReference = ref; return this; }
        public Builder status(IncidentStatus status) { this.status = status; return this; }
        public Builder actionsChecklistJson(String json) { this.actionsChecklistJson = json; return this; }
        public Builder timelineJson(String timeline) { this.timelineJson = timeline; return this; }
        public Builder evidenceNotes(String notes) { this.evidenceNotes = notes; return this; }
        public Builder nextFollowUpDate(LocalDate date) { this.nextFollowUpDate = date; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public FraudIncident build() {
            return new FraudIncident(id, user, incidentType, title, description, incidentDateTime,
                    amountInvolved, currency, severity, sourceScanId, relatedTransactionIds, bankOrProviderContacted,
                    complaintReference, bankTicketReference, status, actionsChecklistJson, timelineJson,
                    evidenceNotes, nextFollowUpDate, createdAt, updatedAt);
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public IncidentType getIncidentType() { return incidentType; }
    public void setIncidentType(IncidentType incidentType) { this.incidentType = incidentType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getIncidentDateTime() { return incidentDateTime; }
    public void setIncidentDateTime(LocalDateTime incidentDateTime) { this.incidentDateTime = incidentDateTime; }
    public BigDecimal getAmountInvolved() { return amountInvolved; }
    public void setAmountInvolved(BigDecimal amountInvolved) { this.amountInvolved = amountInvolved; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public IncidentSeverity getSeverity() { return severity; }
    public void setSeverity(IncidentSeverity severity) { this.severity = severity; }
    public Long getSourceScanId() { return sourceScanId; }
    public void setSourceScanId(Long sourceScanId) { this.sourceScanId = sourceScanId; }
    public String getRelatedTransactionIds() { return relatedTransactionIds; }
    public void setRelatedTransactionIds(String relatedTransactionIds) { this.relatedTransactionIds = relatedTransactionIds; }
    public String getBankOrProviderContacted() { return bankOrProviderContacted; }
    public void setBankOrProviderContacted(String bankOrProviderContacted) { this.bankOrProviderContacted = bankOrProviderContacted; }
    public String getComplaintReference() { return complaintReference; }
    public void setComplaintReference(String complaintReference) { this.complaintReference = complaintReference; }
    public String getBankTicketReference() { return bankTicketReference; }
    public void setBankTicketReference(String bankTicketReference) { this.bankTicketReference = bankTicketReference; }
    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }
    public String getActionsChecklistJson() { return actionsChecklistJson; }
    public void setActionsChecklistJson(String actionsChecklistJson) { this.actionsChecklistJson = actionsChecklistJson; }
    public String getTimelineJson() { return timelineJson; }
    public void setTimelineJson(String timelineJson) { this.timelineJson = timelineJson; }
    public String getEvidenceNotes() { return evidenceNotes; }
    public void setEvidenceNotes(String evidenceNotes) { this.evidenceNotes = evidenceNotes; }
    public LocalDate getNextFollowUpDate() { return nextFollowUpDate; }
    public void setNextFollowUpDate(LocalDate nextFollowUpDate) { this.nextFollowUpDate = nextFollowUpDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
