package com.logistics.service.impl;

import com.logistics.dto.response.FileUploadResponse;
import com.logistics.exception.BadRequestException;
import com.logistics.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Stores uploads on local disk (file.upload-dir) with random names; served by GET /api/files/{name}. */
@Service
public class FileStorageService {

    private static final Map<String, String> ALLOWED = Map.of(
            "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png", "pdf", "application/pdf");
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final long MAX_IMAGE_BYTES = 2L * 1024 * 1024;

    private final Path root;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) throws IOException {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    /**
     * @param category "photo" limits to JPG/PNG up to 2 MB; anything else allows JPG/PNG/PDF up to 5 MB.
     */
    public FileUploadResponse store(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("file", "Choose a file to upload");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot >= 0 ? original.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        boolean photo = "photo".equalsIgnoreCase(category);
        if (!ALLOWED.containsKey(ext) || (photo && ext.equals("pdf"))) {
            throw new BadRequestException("file", photo ? "Only JPG or PNG images are allowed" : "Only PDF, JPG or PNG files are allowed");
        }
        long limit = photo ? MAX_IMAGE_BYTES : MAX_BYTES;
        if (file.getSize() > limit) {
            throw new BadRequestException("file", "File must be " + (limit / (1024 * 1024)) + " MB or smaller");
        }
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BadRequestException("Could not save the file: " + e.getMessage());
        }
        return new FileUploadResponse(name, "/api/files/" + name, file.getSize(), ALLOWED.get(ext));
    }

    public Resource load(String name) {
        try {
            Path path = root.resolve(name).normalize();
            if (!path.startsWith(root)) {
                throw new ResourceNotFoundException("File", name);
            }
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) {
                throw new ResourceNotFoundException("File", name);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File", name);
        }
    }

    public String contentType(String name) {
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        return ALLOWED.getOrDefault(ext, "application/octet-stream");
    }
}
