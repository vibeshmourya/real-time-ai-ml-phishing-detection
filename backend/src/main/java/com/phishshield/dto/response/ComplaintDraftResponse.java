package com.phishshield.dto.response;

import java.util.ArrayList;
import java.util.List;

public class ComplaintDraftResponse {

    private Long incidentId;
    private String incidentType;
    private String incidentTitle;
    private String citizenName;
    private String complaintDraftText;
    private String suggestedSubject;
    private List<String> officialPortals = new ArrayList<>();
    private List<String> guidanceSteps = new ArrayList<>();
    private String portalTarget;
    private String helpline;
    private String formalLetterText;
    private List<String> recommendedEvidence = new ArrayList<>();
    private List<String> immediateContainmentSteps = new ArrayList<>();
    private String disclaimer;

    public ComplaintDraftResponse() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ComplaintDraftResponse r = new ComplaintDraftResponse();

        public Builder incidentId(Long id) { r.incidentId = id; return this; }
        public Builder incidentType(String type) { r.incidentType = type; return this; }
        public Builder incidentTitle(String title) {
            r.incidentTitle = title;
            r.complaintTitle = title;
            return this;
        }
        public Builder citizenName(String name) { r.citizenName = name; return this; }
        public Builder complaintDraftText(String text) {
            r.complaintDraftText = text;
            r.formalLetterText = text;
            return this;
        }
        public Builder suggestedSubject(String subj) { r.suggestedSubject = subj; return this; }
        public Builder officialPortals(List<String> portals) {
            r.officialPortals = portals;
            if (portals != null && !portals.isEmpty()) r.portalTarget = portals.get(0);
            return this;
        }
        public Builder guidanceSteps(List<String> steps) {
            r.guidanceSteps = steps;
            r.immediateContainmentSteps = steps;
            return this;
        }

        public ComplaintDraftResponse build() {
            if (r.disclaimer == null) {
                r.disclaimer = "Prahar is an advisory guidance tool. Official complaints must be lodged directly at cybercrime.gov.in or via the 1930 helpline.";
            }
            if (r.helpline == null) {
                r.helpline = "1930";
            }
            return r;
        }
    }

    // Legacy field alias
    private String complaintTitle;

    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }
    public String getIncidentType() { return incidentType; }
    public void setIncidentType(String incidentType) { this.incidentType = incidentType; }
    public String getIncidentTitle() { return incidentTitle != null ? incidentTitle : complaintTitle; }
    public void setIncidentTitle(String incidentTitle) {
        this.incidentTitle = incidentTitle;
        this.complaintTitle = incidentTitle;
    }
    public String getCitizenName() { return citizenName; }
    public void setCitizenName(String citizenName) { this.citizenName = citizenName; }
    public String getComplaintDraftText() { return complaintDraftText != null ? complaintDraftText : formalLetterText; }
    public void setComplaintDraftText(String text) {
        this.complaintDraftText = text;
        this.formalLetterText = text;
    }
    public String getSuggestedSubject() { return suggestedSubject; }
    public void setSuggestedSubject(String suggestedSubject) { this.suggestedSubject = suggestedSubject; }
    public List<String> getOfficialPortals() { return officialPortals; }
    public void setOfficialPortals(List<String> officialPortals) { this.officialPortals = officialPortals; }
    public List<String> getGuidanceSteps() { return guidanceSteps != null && !guidanceSteps.isEmpty() ? guidanceSteps : immediateContainmentSteps; }
    public void setGuidanceSteps(List<String> guidanceSteps) {
        this.guidanceSteps = guidanceSteps;
        this.immediateContainmentSteps = guidanceSteps;
    }
    public String getComplaintTitle() { return complaintTitle != null ? complaintTitle : incidentTitle; }
    public void setComplaintTitle(String complaintTitle) {
        this.complaintTitle = complaintTitle;
        this.incidentTitle = complaintTitle;
    }
    public String getPortalTarget() { return portalTarget; }
    public void setPortalTarget(String portalTarget) { this.portalTarget = portalTarget; }
    public String getHelpline() { return helpline; }
    public void setHelpline(String helpline) { this.helpline = helpline; }
    public String getFormalLetterText() { return formalLetterText != null ? formalLetterText : complaintDraftText; }
    public void setFormalLetterText(String formalLetterText) {
        this.formalLetterText = formalLetterText;
        this.complaintDraftText = formalLetterText;
    }
    public List<String> getRecommendedEvidence() { return recommendedEvidence; }
    public void setRecommendedEvidence(List<String> recommendedEvidence) { this.recommendedEvidence = recommendedEvidence; }
    public List<String> getImmediateContainmentSteps() { return immediateContainmentSteps != null && !immediateContainmentSteps.isEmpty() ? immediateContainmentSteps : guidanceSteps; }
    public void setImmediateContainmentSteps(List<String> immediateContainmentSteps) {
        this.immediateContainmentSteps = immediateContainmentSteps;
        this.guidanceSteps = immediateContainmentSteps;
    }
    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }
}
