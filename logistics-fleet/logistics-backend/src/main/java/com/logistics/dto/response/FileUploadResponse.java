package com.logistics.dto.response;

public record FileUploadResponse(String fileName, String url, long size, String contentType) {
}
