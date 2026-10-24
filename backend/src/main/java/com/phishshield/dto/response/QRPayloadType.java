package com.phishshield.dto.response;

public enum QRPayloadType {
    URL_HTTP_HTTPS("Web URL (Scannable)", true),
    URL_DANGEROUS_SCHEME("Dangerous URI Scheme (Blocked)", false),
    URL_UNSUPPORTED_SCHEME("Unsupported URI Scheme", false),
    MALFORMED_URL("Malformed or Deceptive URL", false),
    UPI_PAYMENT("UPI Payment Intent", false),
    VCARD("Electronic Contact Card (vCard)", false),
    PLAIN_TEXT("Plain Text / Informational Data", false);

    private final String description;
    private final boolean scannable;

    QRPayloadType(String description, boolean scannable) {
        this.description = description;
        this.scannable = scannable;
    }

    public String getDescription() {
        return description;
    }

    public boolean isScannable() {
        return scannable;
    }
}
