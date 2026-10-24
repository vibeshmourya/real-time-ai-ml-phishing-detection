package com.phishshield.nlp;

import java.math.BigDecimal;

public class ExtractedEntities {

    private BigDecimal amount;
    private String currency = "INR";
    private String paymentChannel; // UPI, CARD, ATM, NETBANKING, QR
    private String institution;    // SBI, HDFC, ICICI, etc.
    private String direction;      // DEBIT, CREDIT
    private String transactionRef;
    private Boolean clickedLink;
    private Boolean sharedOtp;
    private Boolean installedApk;
    private String userActionTaken;
    private String detectedLanguage = "en";

    public ExtractedEntities() {}

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaymentChannel() { return paymentChannel; }
    public void setPaymentChannel(String paymentChannel) { this.paymentChannel = paymentChannel; }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
    public String getTransactionReference() { return transactionRef; }

    public Boolean getClickedLink() { return clickedLink; }
    public void setClickedLink(Boolean clickedLink) { this.clickedLink = clickedLink; }
    public boolean isClickedLink() { return Boolean.TRUE.equals(clickedLink); }

    public Boolean getSharedOtp() { return sharedOtp; }
    public void setSharedOtp(Boolean sharedOtp) { this.sharedOtp = sharedOtp; }
    public boolean isSharedOtpOrCredentials() { return Boolean.TRUE.equals(sharedOtp); }

    public Boolean getInstalledApk() { return installedApk; }
    public void setInstalledApk(Boolean installedApk) { this.installedApk = installedApk; }
    public boolean isDownloadedApp() { return Boolean.TRUE.equals(installedApk); }

    public String getUserActionTaken() { return userActionTaken; }
    public void setUserActionTaken(String userActionTaken) { this.userActionTaken = userActionTaken; }

    public String getDetectedLanguage() { return detectedLanguage; }
    public void setDetectedLanguage(String detectedLanguage) { this.detectedLanguage = detectedLanguage; }

    public boolean hasAnyFinancialEntity() {
        return amount != null || paymentChannel != null || institution != null || direction != null;
    }
}
