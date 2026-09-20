package com.amit.flipkart.api;

import com.amit.flipkart.model.Listing;
import com.amit.flipkart.model.ProductUploadRequest;
import com.amit.flipkart.service.NecklaceListingUploadService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/necklaces")
public class NecklaceUploadController {
    private final NecklaceListingUploadService service;
    public NecklaceUploadController(NecklaceListingUploadService service) { this.service = service; }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(@RequestPart("product") ProductUploadRequest request,
                                      @RequestPart("images") MultipartFile[] images) throws Exception {
        Listing listing = service.create(request, images);
        return Map.of("id", listing.getId(), "sellerSkuId", listing.getSellerSkuId(),
                "category", listing.getCategory(), "status", listing.getStatus(), "imageUrls", listing.getImageUrls());
    }
}
