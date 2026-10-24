package com.phishshield.security.image;

public enum ImageFormat {
    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpg"),
    WEBP("image/webp", "webp"),
    GIF("image/gif", "gif"),
    BMP("image/bmp", "bmp"),
    UNKNOWN("application/octet-stream", "bin");

    private final String mimeType;
    private final String fileExtension;

    ImageFormat(String mimeType, String fileExtension) {
        this.mimeType = mimeType;
        this.fileExtension = fileExtension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileExtension() {
        return fileExtension;
    }
}
