package com.phishshield.dto.response;

import com.phishshield.domain.NotificationPreference;
import com.phishshield.domain.NotificationSeverity;

/**
 * DTO for reading and updating user notification preferences.
 */
public class NotificationPreferenceDto {

    private Boolean inAppEnabled;
    private Boolean emailEnabled;
    private Boolean browserEnabled;
    private NotificationSeverity minSeverity;
    private Boolean scanAlertsEnabled;
    private Boolean incidentAlertsEnabled;
    private Boolean systemAdvisoriesEnabled;

    public NotificationPreferenceDto() {}

    public NotificationPreferenceDto(Boolean inAppEnabled, Boolean emailEnabled, Boolean browserEnabled,
                                     NotificationSeverity minSeverity, Boolean scanAlertsEnabled,
                                     Boolean incidentAlertsEnabled, Boolean systemAdvisoriesEnabled) {
        this.inAppEnabled = inAppEnabled;
        this.emailEnabled = emailEnabled;
        this.browserEnabled = browserEnabled;
        this.minSeverity = minSeverity;
        this.scanAlertsEnabled = scanAlertsEnabled;
        this.incidentAlertsEnabled = incidentAlertsEnabled;
        this.systemAdvisoriesEnabled = systemAdvisoriesEnabled;
    }

    public static NotificationPreferenceDto fromEntity(NotificationPreference entity) {
        if (entity == null) return null;
        return new NotificationPreferenceDto(
                entity.getInAppEnabled(),
                entity.getEmailEnabled(),
                entity.getBrowserEnabled(),
                entity.getMinSeverity(),
                entity.getScanAlertsEnabled(),
                entity.getIncidentAlertsEnabled(),
                entity.getSystemAdvisoriesEnabled()
        );
    }

    public Boolean getInAppEnabled() { return inAppEnabled; }
    public void setInAppEnabled(Boolean inAppEnabled) { this.inAppEnabled = inAppEnabled; }

    public Boolean getEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(Boolean emailEnabled) { this.emailEnabled = emailEnabled; }

    public Boolean getBrowserEnabled() { return browserEnabled; }
    public void setBrowserEnabled(Boolean browserEnabled) { this.browserEnabled = browserEnabled; }

    public NotificationSeverity getMinSeverity() { return minSeverity; }
    public void setMinSeverity(NotificationSeverity minSeverity) { this.minSeverity = minSeverity; }

    public Boolean getScanAlertsEnabled() { return scanAlertsEnabled; }
    public void setScanAlertsEnabled(Boolean scanAlertsEnabled) { this.scanAlertsEnabled = scanAlertsEnabled; }

    public Boolean getIncidentAlertsEnabled() { return incidentAlertsEnabled; }
    public void setIncidentAlertsEnabled(Boolean incidentAlertsEnabled) { this.incidentAlertsEnabled = incidentAlertsEnabled; }

    public Boolean getSystemAdvisoriesEnabled() { return systemAdvisoriesEnabled; }
    public void setSystemAdvisoriesEnabled(Boolean systemAdvisoriesEnabled) { this.systemAdvisoriesEnabled = systemAdvisoriesEnabled; }
}
