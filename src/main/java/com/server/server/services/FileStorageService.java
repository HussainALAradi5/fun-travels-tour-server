package com.server.server.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import org.springframework.stereotype.Service;

@Service
public class FileStorageService {

    // Define where images will be stored on your server
    private final String uploadDir = "uploads/profiles";

public String saveBase64Image(String base64Data, String preferredName) {
    try {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        String[] parts = base64Data.split(",");
        String imageString = parts.length > 1 ? parts[1] : parts[0];
        byte[] imageBytes = Base64.getDecoder().decode(imageString);

        // Use the preferred name (like user_1.jpg) instead of a random UUID
        String fileName = preferredName + ".jpg"; 
        Path filePath = uploadPath.resolve(fileName);

        Files.write(filePath, imageBytes);
        return "/uploads/profiles/" + fileName;
    } catch (IOException e) {
        throw new RuntimeException("Could not save image file: " + e.getMessage());
    }
}
}