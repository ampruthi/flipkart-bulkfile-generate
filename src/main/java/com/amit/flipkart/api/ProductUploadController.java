package com.amit.flipkart.api;

import com.amit.flipkart.model.Listing;
import com.amit.flipkart.model.ProductUploadRequest;
import com.amit.flipkart.service.ListingUploadService;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ProductUploadController {

    private final ObjectMapper mapper;
    private final ListingUploadService service;
    private final DbxClientV2 client;

    public ProductUploadController(ObjectMapper mapper, ListingUploadService service, DbxClientV2 client) {
        this.mapper = mapper;
        this.service = service;
        this.client = client;
    }

    @PostMapping(value = "/products/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(
            @RequestPart("product") ProductUploadRequest request,
            @RequestPart("images") MultipartFile[] images) throws Exception {

        Listing listing = service.create(request, images);

        return Map.of(
                "id", listing.getId(),
                "sellerSkuId", listing.getSellerSkuId(),
                "status", listing.getStatus(),
                "imageUrls", listing.getImageUrls()
        );
    }

    @GetMapping("/dropbox/test")
    public String testDropbox() throws Exception {

        byte[] data = "Dropbox test".getBytes();

        try (ByteArrayInputStream input =
                     new ByteArrayInputStream(data)) {

            var metadata = client.files()
                    .uploadBuilder("/Flipkart-naarinest-media/test.txt")
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(input);

            return "Uploaded: " + metadata.getPathDisplay();
        }
    }
}
