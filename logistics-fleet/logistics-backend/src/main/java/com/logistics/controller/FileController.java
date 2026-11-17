package com.logistics.controller;

import com.logistics.dto.response.ApiResponse;
import com.logistics.dto.response.FileUploadResponse;
import com.logistics.service.impl.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /** category=photo for JPG/PNG up to 2 MB, otherwise PDF/JPG/PNG up to 5 MB. */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileUploadResponse> upload(@RequestParam("file") MultipartFile file,
                                                  @RequestParam(defaultValue = "document") String category) {
        return ApiResponse.ok("File uploaded", fileStorageService.store(file, category));
    }

    @GetMapping("/{name:.+}")
    public ResponseEntity<Resource> download(@PathVariable String name) {
        Resource resource = fileStorageService.load(name);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileStorageService.contentType(name)))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS))
                .body(resource);
    }
}
