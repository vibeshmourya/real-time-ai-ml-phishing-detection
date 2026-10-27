package com.phishshield.nlp;

import java.util.ArrayList;
import java.util.List;

public class DialogueContext {

    public static class Turn {
        private final String role;
        private final String content;

        public Turn(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public String getContent() { return content; }
    }

    private NlpIntent lastIntent;
    private String awaitingSlot; // e.g. "paymentChannel", "sharedOtpOrCredentials"
    private ExtractedEntities accumulatedEntities = new ExtractedEntities();
    private final List<Turn> turns = new ArrayList<>();
    private int turnCount = 0;

    public DialogueContext() {}

    public NlpIntent getLastIntent() { return lastIntent; }
    public void setLastIntent(NlpIntent lastIntent) { this.lastIntent = lastIntent; }

    public String getAwaitingSlot() { return awaitingSlot; }
    public void setAwaitingSlot(String awaitingSlot) { this.awaitingSlot = awaitingSlot; }

    public String getPendingSlot() { return awaitingSlot; }
    public void setPendingSlot(String pendingSlot) { this.awaitingSlot = pendingSlot; }

    public ExtractedEntities getAccumulatedEntities() { return accumulatedEntities; }
    public void setAccumulatedEntities(ExtractedEntities accumulatedEntities) { this.accumulatedEntities = accumulatedEntities; }

    public int getTurnCount() { return turnCount; }
    public void incrementTurnCount() { this.turnCount++; }

    public void addTurn(String role, String content) {
        turns.add(new Turn(role, content));
        incrementTurnCount();
    }

    public List<Turn> getTurns() { return turns; }

    public String getSlot(String slotName) {
        if (slotName == null || accumulatedEntities == null) return null;
        switch (slotName.toLowerCase()) {
            case "amount":
                return accumulatedEntities.getAmount() != null ? accumulatedEntities.getAmount().toPlainString() : null;
            case "paymentchannel":
            case "channel":
                return accumulatedEntities.getPaymentChannel();
            case "institution":
            case "bank":
                return accumulatedEntities.getInstitution();
            case "direction":
                return accumulatedEntities.getDirection();
            case "ref":
            case "transactionreference":
                return accumulatedEntities.getTransactionRef();
            case "clickedlink":
                return accumulatedEntities.getClickedLink() != null ? String.valueOf(accumulatedEntities.getClickedLink()) : null;
            case "sharedotp":
            case "sharedotporcredentials":
                return accumulatedEntities.getSharedOtp() != null ? String.valueOf(accumulatedEntities.getSharedOtp()) : null;
            case "bankdetailsentered":
            case "bankdetails":
                return accumulatedEntities.getBankDetailsEntered() != null ? String.valueOf(accumulatedEntities.getBankDetailsEntered()) : null;
            case "credentialsdisclosed":
                return accumulatedEntities.getCredentialsDisclosed() != null ? String.valueOf(accumulatedEntities.getCredentialsDisclosed()) : null;
            case "moneylost":
                return accumulatedEntities.getMoneyLost() != null ? String.valueOf(accumulatedEntities.getMoneyLost()) : null;
            case "actiontaken":
            case "protectiveactiontaken":
                return accumulatedEntities.getProtectiveActionTaken() != null ? String.valueOf(accumulatedEntities.getProtectiveActionTaken()) : null;
            default:
                return null;
        }
    }

    public boolean isBankDetailsEntered() {
        return accumulatedEntities != null && accumulatedEntities.isBankDetailsEntered();
    }

    public boolean isSharedOtp() {
        return accumulatedEntities != null && Boolean.TRUE.equals(accumulatedEntities.getSharedOtp());
    }

    public boolean isMoneyLost() {
        return accumulatedEntities != null && accumulatedEntities.isMoneyLost();
    }

    public boolean isProtectiveActionTaken() {
        return accumulatedEntities != null && accumulatedEntities.isProtectiveActionTaken();
    }

    public void mergeEntities(ExtractedEntities newEntities) {
        if (newEntities == null) return;
        if (newEntities.getAmount() != null) accumulatedEntities.setAmount(newEntities.getAmount());
        if (newEntities.getPaymentChannel() != null) accumulatedEntities.setPaymentChannel(newEntities.getPaymentChannel());
        if (newEntities.getInstitution() != null) accumulatedEntities.setInstitution(newEntities.getInstitution());
        if (newEntities.getDirection() != null) accumulatedEntities.setDirection(newEntities.getDirection());
        if (newEntities.getTransactionRef() != null) accumulatedEntities.setTransactionRef(newEntities.getTransactionRef());
        if (newEntities.getClickedLink() != null) accumulatedEntities.setClickedLink(newEntities.getClickedLink());
        if (newEntities.getSharedOtp() != null) accumulatedEntities.setSharedOtp(newEntities.getSharedOtp());
        if (newEntities.getInstalledApk() != null) accumulatedEntities.setInstalledApk(newEntities.getInstalledApk());
        if (newEntities.getBankDetailsEntered() != null) accumulatedEntities.setBankDetailsEntered(newEntities.getBankDetailsEntered());
        if (newEntities.getCredentialsDisclosed() != null) accumulatedEntities.setCredentialsDisclosed(newEntities.getCredentialsDisclosed());
        if (newEntities.getMoneyLost() != null) accumulatedEntities.setMoneyLost(newEntities.getMoneyLost());
        if (newEntities.getProtectiveActionTaken() != null) accumulatedEntities.setProtectiveActionTaken(newEntities.getProtectiveActionTaken());
        if (newEntities.getUserActionTaken() != null) accumulatedEntities.setUserActionTaken(newEntities.getUserActionTaken());
    }
}
