package com.phishshield.security.image;

import java.awt.image.BufferedImage;

/**
 * Encapsulates a securely validated, decoded image along with its verified metadata.
 */
public class ValidatedImage {

    private final BufferedImage image;
    private final ImageFormat format;
    private final int width;
    private final int height;
    private final long sizeBytes;
    private final String originalFilename;

    public ValidatedImage(BufferedImage image, ImageFormat format, int width, int height, long sizeBytes, String originalFilename) {
        this.image = image;
        this.format = format;
        this.width = width;
        this.height = height;
        this.sizeBytes = sizeBytes;
        this.originalFilename = originalFilename != null ? originalFilename : "unknown";
    }

    public BufferedImage getImage() {
        return image;
    }

    public ImageFormat getFormat() {
        return format;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public long getTotalPixels() {
        return (long) width * height;
    }

    @Override
    public String toString() {
        return "ValidatedImage{" +
                "format=" + format +
                ", width=" + width +
                ", height=" + height +
                ", sizeBytes=" + sizeBytes +
                '}';
    }
}
