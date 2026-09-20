package com.amit.flipkart.model;

public record DropboxImageUploadResult(
        int sequence,
        String fileName,
        String path,
        String url
) {
}