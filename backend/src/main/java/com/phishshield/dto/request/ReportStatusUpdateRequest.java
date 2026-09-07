package com.phishshield.dto.request;

import com.phishshield.domain.ReportStatus;
import jakarta.validation.constraints.NotNull;

public class ReportStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private ReportStatus status;

    private String adminNotes;

    public ReportStatusUpdateRequest() {}

    public ReportStatusUpdateRequest(ReportStatus status, String adminNotes) {
        this.status = status;
        this.adminNotes = adminNotes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ReportStatus status;
        private String adminNotes;

        public Builder status(ReportStatus status) { this.status = status; return this; }
        public Builder adminNotes(String adminNotes) { this.adminNotes = adminNotes; return this; }

        public ReportStatusUpdateRequest build() {
            return new ReportStatusUpdateRequest(status, adminNotes);
        }
    }

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
}
