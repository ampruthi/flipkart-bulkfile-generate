package com.amit.flipkart.service;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleDriveService {

    private final Drive drive;
    private final String folderId;

    public GoogleDriveService(
            Drive drive,
            @Value("${app.google-drive.folder-id:}") String folderId) {
        this.drive = drive;
        this.folderId = folderId;
    }

    public List<String> uploadImages(String sku, MultipartFile[] images) {
        try {
            List<String> urls = new ArrayList<>();

            for (int i = 0; i < images.length; i++) {
                MultipartFile image = images[i];
                String original = image.getOriginalFilename() == null
                        ? "image.jpg" : image.getOriginalFilename();
                String extension = extension(original);
                String fileName = sku + "_" + (i + 1) + extension;

                Path temp = Files.createTempFile("flipkart-", extension);
                image.transferTo(temp);

                try {
                    com.google.api.services.drive.model.File metadata =
                            new com.google.api.services.drive.model.File()
                                    .setName(fileName);

                    if (folderId != null && !folderId.isBlank()) {
                        metadata.setParents(List.of(folderId));
                    }

                    FileContent media = new FileContent(
                            image.getContentType() == null ? "application/octet-stream" : image.getContentType(),
                            temp.toFile());

                    com.google.api.services.drive.model.File uploaded =
                            drive.files().create(metadata, media)
                                    .setFields("id,name,webViewLink,webContentLink")
                                    .execute();

                    // Flipkart needs a URL that can be fetched without the uploader's
                    // Google credentials. Make POC-created files publicly readable.
                    try {
                        drive.permissions().create(
                                        uploaded.getId(),
                                        new com.google.api.services.drive.model.Permission()
                                                .setType("anyone")
                                                .setRole("reader"))
                                .execute();
                    } catch (Exception ignored) {
                        // Some Google Workspace accounts prohibit public sharing.
                        // In that case the Drive URL is still returned, but Flipkart
                        // may not be able to fetch it until sharing is enabled.
                    }

                    urls.add("https://drive.google.com/uc?export=view&id=" + uploaded.getId());
                } finally {
                    Files.deleteIfExists(temp);
                }
            }

            return urls;
        } catch (Exception e) {
            throw new IllegalStateException("Google Drive image upload failed: " + e.getMessage(), e);
        }
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".jpg";
    }
}
