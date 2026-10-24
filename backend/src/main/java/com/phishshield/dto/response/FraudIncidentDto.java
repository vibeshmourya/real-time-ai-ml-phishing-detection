package com.phishshield.dto.response;

import com.phishshield.domain.FraudIncident;
import com.phishshield.domain.IncidentSeverity;
import com.phishshield.domain.IncidentStatus;
import com.phishshield.domain.IncidentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FraudIncidentDto {
    private Long id;
    private IncidentType incidentType;
    private String title;
    private String description;
    private LocalDateTime incidentDateTime;
    private BigDecimal amountInvolved;
    private String currency;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public FraudIncidentDto() {}

    public static FraudIncidentDto fromEntity(FraudIncident i) {
        FraudIncidentDto dto = new FraudIncidentDto();
        dto.setId(i.getId());
        dto.setIncidentType(i.getIncidentType());
        dto.setTitle(i.getTitle());
        dto.setDescription(i.getDescription());
        dto.setIncidentDateTime(i.getIncidentDateTime());
        dto.setAmountInvolved(i.getAmountInvolved());
        dto.setCurrency(i.getCurrency());
        dto.setSeverity(i.getSeverity());
        dto.setSourceScanId(i.getSourceScanId());
        dto.setRelatedTransactionIds(i.getRelatedTransactionIds());
        dto.setBankOrProviderContacted(i.getBankOrProviderContacted());
        dto.setComplaintReference(i.getComplaintReference());
        dto.setBankTicketReference(i.getBankTicketReference());
        dto.setStatus(i.getStatus());
        dto.setActionsChecklistJson(i.getActionsChecklistJson());
        dto.setTimelineJson(i.getTimelineJson());
        dto.setEvidenceNotes(i.getEvidenceNotes());
        dto.setNextFollowUpDate(i.getNextFollowUpDate());
        dto.setCreatedAt(i.getCreatedAt());
        dto.setUpdatedAt(i.getUpdatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
