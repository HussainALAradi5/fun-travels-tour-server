package com.server.server.services.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import org.springframework.stereotype.Service;

import com.server.server.config.properties.AppProperties;

import lombok.RequiredArgsConstructor;

/** Stores validated uploads under {@code app.upload-dir}. Only JPEG and PNG images are accepted. */
@Service
@RequiredArgsConstructor
public class FileStorageService {

    public static final String PUBLIC_PREFIX = "/uploads/";
    private static final String PROFILE_FOLDER = "profiles";
    private static final byte[] JPEG_MAGIC = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
    private static final byte[] PNG_MAGIC = { (byte) 0x89, 0x50, 0x4E, 0x47 };

    private final AppProperties appProperties;

    /** Saves the image as {@code profiles/user_<id>.<ext>} and returns its public path. */
    public String saveProfileImage(String base64Data, Integer userId) {
        byte[] bytes = decode(base64Data);
        if (bytes.length > appProperties.maxImageBytes()) {
            throw new IllegalArgumentException("Images must be at most " + appProperties.maxImageBytes() / 1024 + " KB.");
        }
        String extension = startsWith(bytes, JPEG_MAGIC) ? "jpg" : startsWith(bytes, PNG_MAGIC) ? "png" : null;
        if (extension == null) {
            throw new IllegalArgumentException("Only JPEG and PNG images are supported.");
        }
        String fileName = "user_" + userId + "." + extension;
        try {
            Path folder = Paths.get(appProperties.uploadDir(), PROFILE_FOLDER).toAbsolutePath().normalize();
            Files.createDirectories(folder);
            Files.write(folder.resolve(fileName), bytes);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save image file.", e);
        }
        return PUBLIC_PREFIX + PROFILE_FOLDER + "/" + fileName;
    }

    private static byte[] decode(String base64Data) {
        String payload = base64Data.contains(",") ? base64Data.substring(base64Data.indexOf(',') + 1) : base64Data;
        try {
            return Base64.getDecoder().decode(payload.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("The image data is not valid base64.");
        }
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
