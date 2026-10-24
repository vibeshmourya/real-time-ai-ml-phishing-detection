package com.phishshield.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

/**
 * User-configurable preferences controlling in-app, browser, and email alerts.
 */
@Entity
@Table(name = "notification_preferences")
public class NotificationPreference implements Persistable<Long> {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "in_app_enabled", nullable = false)
    private Boolean inAppEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled = false;

    @Column(name = "browser_enabled", nullable = false)
    private Boolean browserEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "min_severity", length = 20, nullable = false)
    private NotificationSeverity minSeverity = NotificationSeverity.MEDIUM;

    @Column(name = "scan_alerts_enabled", nullable = false)
    private Boolean scanAlertsEnabled = true;

    @Column(name = "incident_alerts_enabled", nullable = false)
    private Boolean incidentAlertsEnabled = true;

    @Column(name = "system_advisories_enabled", nullable = false)
    private Boolean systemAdvisoriesEnabled = true;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private boolean isNew = true;

    public NotificationPreference() {}

    public NotificationPreference(Long userId, User user, Boolean inAppEnabled, Boolean emailEnabled,
                                  Boolean browserEnabled, NotificationSeverity minSeverity,
                                  Boolean scanAlertsEnabled, Boolean incidentAlertsEnabled,
                                  Boolean systemAdvisoriesEnabled, LocalDateTime updatedAt) {
        this.userId = userId;
        this.user = user;
        this.inAppEnabled = inAppEnabled != null ? inAppEnabled : true;
        this.emailEnabled = emailEnabled != null ? emailEnabled : false;
        this.browserEnabled = browserEnabled != null ? browserEnabled : true;
        this.minSeverity = minSeverity != null ? minSeverity : NotificationSeverity.MEDIUM;
        this.scanAlertsEnabled = scanAlertsEnabled != null ? scanAlertsEnabled : true;
        this.incidentAlertsEnabled = incidentAlertsEnabled != null ? incidentAlertsEnabled : true;
        this.systemAdvisoriesEnabled = systemAdvisoriesEnabled != null ? systemAdvisoriesEnabled : true;
        this.updatedAt = updatedAt;
        this.isNew = false;
    }

    public static NotificationPreference defaultForUser(User user) {
        NotificationPreference pref = new NotificationPreference();
        pref.setUser(user);
        pref.setUserId(user.getId());
        pref.setInAppEnabled(true);
        pref.setEmailEnabled(false);
        pref.setBrowserEnabled(true);
        pref.setMinSeverity(NotificationSeverity.MEDIUM);
        pref.setScanAlertsEnabled(true);
        pref.setIncidentAlertsEnabled(true);
        pref.setSystemAdvisoriesEnabled(true);
        pref.isNew = true;
        return pref;
    }

    @Override
    public Long getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

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

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
