package com.phishshield.dto.response;

/**
 * Encapsulates the results and attributes from the pre-analysis image security validation phase.
 */
public class ImageValidationMetadata {

    private boolean valid;
    private String format;
    private Integer width;
    private Integer height;
    private Long sizeBytes;
    private String sanitizedFilename;
    private String validationError;

    public ImageValidationMetadata() {}

    public ImageValidationMetadata(boolean valid, String format, Integer width, Integer height,
                                   Long sizeBytes, String sanitizedFilename, String validationError) {
        this.valid = valid;
        this.format = format;
        this.width = width;
        this.height = height;
        this.sizeBytes = sizeBytes;
        this.sanitizedFilename = sanitizedFilename;
        this.validationError = validationError;
    }

    public static ImageValidationMetadata valid(String format, int width, int height, long sizeBytes, String filename) {
        return new ImageValidationMetadata(true, format, width, height, sizeBytes, filename, null);
    }

    public static ImageValidationMetadata invalid(String error, String filename) {
        return new ImageValidationMetadata(false, "UNKNOWN", 0, 0, 0L, filename, error);
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getSanitizedFilename() { return sanitizedFilename; }
    public void setSanitizedFilename(String sanitizedFilename) { this.sanitizedFilename = sanitizedFilename; }

    public String getValidationError() { return validationError; }
    public void setValidationError(String validationError) { this.validationError = validationError; }
}
