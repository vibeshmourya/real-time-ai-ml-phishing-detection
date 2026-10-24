package com.phishshield.dto.request;

import com.phishshield.domain.IncidentSeverity;
import com.phishshield.domain.IncidentStatus;
import com.phishshield.domain.IncidentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FraudIncidentRequest {

    @NotNull(message = "Incident type is required")
    private IncidentType incidentType;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;
    private LocalDateTime incidentDateTime;
    private BigDecimal amountInvolved;
    private String currency = "INR";
    private IncidentSeverity severity;
    private Long sourceScanId;
    private String relatedTransactionIds;
    private String bankOrProviderContacted;
    private String complaintReference;
    private String bankTicketReference;
    private IncidentStatus status;
    private String actionsChecklistJson;
    private String timelineJson;
    private String evidenceNotes;
    private LocalDate nextFollowUpDate;

    public FraudIncidentRequest() {}

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
}
