package com.phishshield;

import com.phishshield.exception.BadRequestException;
import com.phishshield.security.image.ImageFormat;
import com.phishshield.security.image.ImageSecurityValidator;
import com.phishshield.security.image.ValidatedImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ImageSecurityValidatorTest {

    private ImageSecurityValidator validator;

    @BeforeEach
    void setUp() {
        // Construct validator with conservative test bounds: 2MB max, 1000x1000 max, 1,000,000 pixels max
        validator = new ImageSecurityValidator(
                2 * 1024 * 1024L, // 2 MB
                1000,             // 1000 px width
                1000,             // 1000 px height
                1_000_000L,       // 1 MP total
                "PNG,JPEG,JPG,WEBP,GIF,BMP"
        );
    }

    private byte[] createSyntheticImage(String format, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.WHITE);
        g.drawString("PRAHAR TEST", 10, 20);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("Should successfully validate and decode a synthetic PNG image")
    void testValidPngImage() throws IOException {
        byte[] pngBytes = createSyntheticImage("png", 320, 240);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-screenshot.png",
                "image/png",
                pngBytes
        );

        ValidatedImage validated = validator.validateAndDecode(file);

        assertNotNull(validated);
        assertEquals(ImageFormat.PNG, validated.getFormat());
        assertEquals(320, validated.getWidth());
        assertEquals(240, validated.getHeight());
        assertEquals(pngBytes.length, validated.getSizeBytes());
        assertEquals("test-screenshot.png", validated.getOriginalFilename());
        assertNotNull(validated.getImage());
    }

    @Test
    @DisplayName("Should successfully validate and decode a synthetic JPEG image")
    void testValidJpegImage() throws IOException {
        byte[] jpegBytes = createSyntheticImage("jpg", 200, 150);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                jpegBytes
        );

        ValidatedImage validated = validator.validateAndDecode(file);

        assertNotNull(validated);
        assertEquals(ImageFormat.JPEG, validated.getFormat());
        assertEquals(200, validated.getWidth());
        assertEquals(150, validated.getHeight());
    }

    @Test
    @DisplayName("Should reject empty or zero-byte file uploads")
    void testRejectEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validateAndDecode(emptyFile));
        assertTrue(ex.getMessage().contains("empty") || ex.getMessage().contains("zero bytes"));
    }

    @Test
    @DisplayName("Should reject null MultipartFile object")
    void testRejectNullFile() {
        BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validateAndDecode((MockMultipartFile) null));
        assertTrue(ex.getMessage().contains("No image file was provided"));
    }

    @Test
    @DisplayName("Should reject oversized image uploads exceeding byte threshold")
    void testRejectOversizedFile() {
        // Validator max is set to 2MB (2,097,152 bytes); create a 2.5MB payload
        byte[] oversizedBytes = new byte[2 * 1024 * 1024 + 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "huge.png",
                "image/png",
                oversizedBytes
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validateAndDecode(file));
        assertTrue(ex.getMessage().contains("exceeds maximum permitted limit"));
    }

    @Test
    @DisplayName("Should reject spoofed file with fake .png extension but text contents")
    void testRejectSpoofedMimeTextContent() {
        byte[] textPayload = "Hello, this is a plain text file pretending to be an image!".getBytes();
        MockMultipartFile fakePng = new MockMultipartFile(
                "file",
                "malicious.png",
                "image/png",
                textPayload
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validateAndDecode(fakePng));
        assertTrue(ex.getMessage().contains("Unsupported or unrecognized file format"));
    }

    @Test
    @DisplayName("Should reject spoofed executable disguised as an image")
    void testRejectSpoofedExecutable() {
        // Windows MZ executable signature
        byte[] exeBytes = new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00};
        MockMultipartFile fakePng = new MockMultipartFile(
                "file",
                "trojan.png",
                "image/png",
                exeBytes
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validateAndDecode(fakePng));
        assertTrue(ex.getMessage().contains("Unsupported or unrecognized file format"));
    }

    @Test
    @DisplayName("Should reject corrupted/truncated image with valid PNG magic header but invalid data")
    void testRejectTruncatedPng() {
        // 8-byte valid PNG header followed by 4 bytes of broken garbage
        byte[] brokenPng = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x01
        };

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "broken.png",
                "image/png",
                brokenPng
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validateAndDecode(file));
        assertTrue(ex.getMessage().contains("Corrupted or malformed image data"));
    }

    @Test
    @DisplayName("Should reject decompression bomb with width exceeding allowed bounds")
    void testRejectExcessiveWidth() throws IOException {
        // Validator width limit is 1000px; create 1200x100px image
        byte[] wideImage = createSyntheticImage("png", 1200, 100);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "wide.png",
                "image/png",
                wideImage
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validateAndDecode(file));
        assertTrue(ex.getMessage().contains("exceed the maximum allowed limits"));
    }

    @Test
    @DisplayName("Should reject decompression bomb with height exceeding allowed bounds")
    void testRejectExcessiveHeight() throws IOException {
        // Validator height limit is 1000px; create 100x1200px image
        byte[] tallImage = createSyntheticImage("png", 100, 1200);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "tall.png",
                "image/png",
                tallImage
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> validator.validateAndDecode(file));
        assertTrue(ex.getMessage().contains("exceed the maximum allowed limits"));
    }

    @Test
    @DisplayName("Should reject pixel bomb with total pixel area exceeding maximum threshold")
    void testRejectExcessiveTotalPixels() throws IOException {
        // Configure strict pixel bomb validator (max 50,000 pixels total)
        ImageSecurityValidator strictPixelValidator = new ImageSecurityValidator(
                5 * 1024 * 1024L,
                2000,
                2000,
                50_000L, // 50,000 pixels limit
                "PNG,JPEG"
        );

        // 300 x 300 = 90,000 pixels (> 50,000 threshold)
        byte[] bombImage = createSyntheticImage("png", 300, 300);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pixel-bomb.png",
                "image/png",
                bombImage
        );

        BadRequestException ex = assertThrows(BadRequestException.class, () -> strictPixelValidator.validateAndDecode(file));
        assertTrue(ex.getMessage().contains("Potential decompression bomb detected"));
    }

    @Test
    @DisplayName("Should sanitize dangerous filenames containing path traversal sequences")
    void testSanitizeFilename() throws IOException {
        byte[] bytes = createSyntheticImage("png", 50, 50);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../../../etc/passwd.png",
                "image/png",
                bytes
        );

        ValidatedImage validated = validator.validateAndDecode(file);
        assertEquals("passwd.png", validated.getOriginalFilename());
        assertFalse(validated.getOriginalFilename().contains("/"));
        assertFalse(validated.getOriginalFilename().contains(".."));
    }
}
