package com.amit.flipkart.service;

import com.amit.flipkart.config.DropboxProperties;
import com.amit.flipkart.model.DropboxImageUploadResult;
import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DropboxService {

    private final DbxClientV2 client;
    private final DropboxProperties properties;

    public DropboxService(
            DbxClientV2 client,
            DropboxProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public List<DropboxImageUploadResult> uploadProductImages(
            String sku,
            MultipartFile[] images) throws IOException, DbxException {

        List<DropboxImageUploadResult> results = new ArrayList<>();

        for (int i = 0; i < images.length; i++) {

            MultipartFile image = images[i];

            if (image.isEmpty()) {
                continue;
            }

            String extension = getExtension(image.getOriginalFilename());

            String fileName = sku + "_" + (i + 1) + extension;

            String dropboxPath =
                    properties.getFolder() + "/" + fileName;

            /*
             * Read the multipart file completely first.
             *
             * This avoids depending on the MultipartFile input
             * stream remaining valid while Dropbox is uploading.
             */
            byte[] bytes = image.getBytes();

            FileMetadata metadata;

            try (ByteArrayInputStream inputStream =
                         new ByteArrayInputStream(bytes)) {

                metadata = client.files()
                        .uploadBuilder(dropboxPath)
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(inputStream);
            }

            String url = createOrGetSharedLink(dropboxPath);

            results.add(
                    new DropboxImageUploadResult(
                            i + 1,
                            fileName,
                            metadata.getPathDisplay(),
                            url
                    )
            );
        }

        return results;
    }

    private String getExtension(String fileName) {

        if (fileName == null || fileName.isBlank()) {
            return ".jpg";
        }

        int index = fileName.lastIndexOf('.');

        if (index == -1) {
            return ".jpg";
        }

        return fileName.substring(index).toLowerCase();
    }

    private String createOrGetSharedLink(String path) {

        try {

            var links = client.sharing()
                    .listSharedLinksBuilder()
                    .withPath(path)
                    .withDirectOnly(true)
                    .start();

            if (!links.getLinks().isEmpty()) {
                return convertToDirectLink(
                        links.getLinks().get(0).getUrl()
                );
            }

            var sharedLink = client.sharing()
                    .createSharedLinkWithSettings(path);

            return convertToDirectLink(sharedLink.getUrl());

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create Dropbox shared link for: " + path,
                    e
            );
        }
    }

    private String convertToDirectLink(String url) {

        if (url == null) {
            return null;
        }

        if (url.contains("dl=0")) {
            return url.replace("dl=0", "dl=1");
        }

        if (url.contains("?")) {
            return url + "&dl=1";
        }

        return url + "?dl=1";
    }
}