package com.quickbite.restaurant.service;

import com.quickbite.restaurant.exception.ImageUploadException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Stores uploaded restaurant/menu photos on local disk under a single
 * "uploads" folder next to wherever the service runs, and hands back a URL
 * path the static resource handler (see StaticResourceConfig) serves
 * directly. This intentionally avoids needing S3/Cloudinary/any external
 * account just to demo photo uploads - swap this class for a real object
 * store client when this moves beyond a local/demo deployment.
 */
@Component
public class FileStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory: " + uploadRoot, e);
        }
    }

    /** @return the public URL path (e.g. "/images/&lt;generated-name&gt;.jpg") to store on the entity. */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageUploadException("No file was uploaded");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ImageUploadException("Image must be smaller than 5 MB");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ImageUploadException("Only JPEG, PNG, WEBP or GIF images are allowed");
        }

        String extension = extensionOf(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + extension;

        try {
            Path target = uploadRoot.resolve(storedName).normalize();
            if (!target.startsWith(uploadRoot)) {
                throw new ImageUploadException("Invalid file name");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ImageUploadException("Could not save the uploaded image");
        }

        return "/images/" + storedName;
    }

    private String extensionOf(String originalFilename) {
        String cleaned = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int dot = cleaned.lastIndexOf('.');
        if (dot < 0 || dot == cleaned.length() - 1) {
            return "";
        }
        String ext = cleaned.substring(dot).toLowerCase();
        // Only allow a small, known-safe set of extensions regardless of what the client claims.
        return List.of(".jpg", ".jpeg", ".png", ".webp", ".gif").contains(ext) ? ext : "";
    }
}
