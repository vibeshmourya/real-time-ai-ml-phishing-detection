package com.phishshield.nlp;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured incident tracking representing the active state of a security event.
 * Eliminates loss of context across multi-turn conversational flows.
 */
public class IncidentState {

    public enum IncidentType {
        NONE,
        SUSPICIOUS_LINK_CLICKED,
        CREDENTIALS_DISCLOSED,
        OTP_PIN_DISCLOSED,
        BANKING_DETAILS_ENTERED,
        UNAUTHORIZED_DEBIT,
        UPI_FRAUD,
        ATM_CARD_FRAUD,
        SUSPICIOUS_APP_INSTALLED,
        ACCOUNT_COMPROMISE,
        SIM_SWAP,
        STOLEN_PHONE,
        FAKE_SHOPPING_ORDER
    }

    private IncidentType incidentType = IncidentType.NONE;
    private Boolean linkClicked = false;
    private Boolean credentialsEntered = false;
    private Boolean otpOrPinShared = false;
    private Boolean bankDetailsEntered = false;
    private Boolean moneyLost = false;
    private Boolean appInstalled = false;
    private String paymentChannel; // UPI, CARD, NETBANKING, ATM
    private String amount;
    private String institution;
    private String transactionRef;
    private Boolean protectiveActionTaken = false;
    private final List<String> actionsCompleted = new ArrayList<>();
    private String unresolvedQuestion;

    public IncidentState() {}

    public IncidentType getIncidentType() { return incidentType; }
    public void setIncidentType(IncidentType incidentType) { this.incidentType = incidentType; }

    public boolean isLinkClicked() { return Boolean.TRUE.equals(linkClicked); }
    public void setLinkClicked(Boolean linkClicked) { this.linkClicked = linkClicked; }

    public boolean isCredentialsEntered() { return Boolean.TRUE.equals(credentialsEntered); }
    public void setCredentialsEntered(Boolean credentialsEntered) { this.credentialsEntered = credentialsEntered; }

    public boolean isOtpOrPinShared() { return Boolean.TRUE.equals(otpOrPinShared); }
    public void setOtpOrPinShared(Boolean otpOrPinShared) { this.otpOrPinShared = otpOrPinShared; }

    public boolean isBankDetailsEntered() { return Boolean.TRUE.equals(bankDetailsEntered); }
    public void setBankDetailsEntered(Boolean bankDetailsEntered) { this.bankDetailsEntered = bankDetailsEntered; }

    public boolean isMoneyLost() { return Boolean.TRUE.equals(moneyLost); }
    public void setMoneyLost(Boolean moneyLost) { this.moneyLost = moneyLost; }

    public boolean isAppInstalled() { return Boolean.TRUE.equals(appInstalled); }
    public void setAppInstalled(Boolean appInstalled) { this.appInstalled = appInstalled; }

    public String getPaymentChannel() { return paymentChannel; }
    public void setPaymentChannel(String paymentChannel) { this.paymentChannel = paymentChannel; }

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public boolean isProtectiveActionTaken() { return Boolean.TRUE.equals(protectiveActionTaken); }
    public void setProtectiveActionTaken(Boolean protectiveActionTaken) { this.protectiveActionTaken = protectiveActionTaken; }

    public List<String> getActionsCompleted() { return actionsCompleted; }
    public void addActionCompleted(String action) {
        if (action != null && !actionsCompleted.contains(action)) {
            actionsCompleted.add(action);
        }
    }

    public String getUnresolvedQuestion() { return unresolvedQuestion; }
    public void setUnresolvedQuestion(String unresolvedQuestion) { this.unresolvedQuestion = unresolvedQuestion; }

    public boolean hasActiveIncident() {
        return incidentType != IncidentType.NONE || isLinkClicked() || isCredentialsEntered() ||
                isOtpOrPinShared() || isBankDetailsEntered() || isMoneyLost() || isAppInstalled();
    }

    /**
     * Produces a clean, factual summary for inclusion in the AI prompt or diagnostic log.
     */
    public String toPromptSummary() {
        if (!hasActiveIncident()) {
            return "No active security incident in progress.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("- Type: ").append(incidentType).append("\n");
        sb.append("- Link Clicked: ").append(isLinkClicked() ? "YES" : "NO").append("\n");
        sb.append("- Credentials / Password Entered: ").append(isCredentialsEntered() ? "YES" : "NO").append("\n");
        sb.append("- OTP / PIN Disclosed: ").append(isOtpOrPinShared() ? "YES" : "NO").append("\n");
        sb.append("- Bank Account / Card Details Entered: ").append(isBankDetailsEntered() ? "YES" : "NO").append("\n");
        sb.append("- Unauthorized Debit / Financial Loss: ").append(isMoneyLost() ? (amount != null ? "YES (₹" + amount + ")" : "YES") : "NO").append("\n");
        if (paymentChannel != null) sb.append("- Payment Channel: ").append(paymentChannel).append("\n");
        if (institution != null) sb.append("- Institution: ").append(institution).append("\n");
        if (transactionRef != null) sb.append("- Transaction Reference / UTR: ").append(transactionRef).append("\n");
        if (isAppInstalled()) sb.append("- Suspicious App / APK Installed: YES\n");
        if (isProtectiveActionTaken()) sb.append("- Initial Containment Action Taken by User: YES\n");
        if (!actionsCompleted.isEmpty()) sb.append("- Actions Completed: ").append(String.join(", ", actionsCompleted)).append("\n");
        if (unresolvedQuestion != null) sb.append("- Unresolved Detail Needed: ").append(unresolvedQuestion).append("\n");
        return sb.toString().trim();
    }
}
