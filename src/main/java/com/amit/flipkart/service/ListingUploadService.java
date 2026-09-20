package com.amit.flipkart.service;

import com.amit.flipkart.model.DropboxImageUploadResult;
import com.amit.flipkart.model.Listing;
import com.amit.flipkart.model.ListingStatus;
import com.amit.flipkart.model.ProductUploadRequest;
import com.amit.flipkart.repository.ListingRepository;
import com.dropbox.core.DbxException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class ListingUploadService {

    private static final Logger log = LoggerFactory.getLogger(ListingUploadService.class);
    private final ObjectMapper mapper;
    private final ListingRepository repository;
    private final OpenAiProductService openAi;
    private final DropboxService dropboxService;

    public ListingUploadService(
            ObjectMapper mapper,
            ListingRepository repository,
            OpenAiProductService openAi,
            DropboxService dropboxService) {
        this.mapper = mapper;
        this.repository = repository;
        this.openAi = openAi;
        this.dropboxService = dropboxService;
    }

    public Listing create(ProductUploadRequest input, MultipartFile[] images) throws IOException, DbxException {
        if (images == null || images.length == 0) {
            throw new IllegalArgumentException("At least one image is required");
        }

        if (repository.findBySellerSkuId(input.getSellerSkuId()).isPresent()) {
            throw new IllegalArgumentException(
                    "Seller SKU already exists: " + input.getSellerSkuId());
        }

        // Image #1 only is sent to OpenAI, as requested.
        OpenAiProductService.AiResult ai = openAi.analyze(input, images[0]);
        log.info("AI Response: {}", ai);

        List<DropboxImageUploadResult> driveUrls =
                dropboxService.uploadProductImages(
                        input.getSellerSkuId(),
                        images
                );

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("Seller SKU ID", input.getSellerSkuId());
        data.put("MRP (INR)", input.getMrp());
        data.put("Your selling price (INR)", input.getSellingPrice());
        data.put("Fullfilment by", input.getFulfilment());
        data.put("HSN", input.getHsn());
        data.put("Country Of Origin", input.getCountryOfOrigin());
        data.put("Manufacturer Details", input.getManufacturerDetails());
        data.put("Packer Details", input.getPackerDetails());
        data.put("Tax Code", input.getTaxCode());
        data.put("Brand", input.getBrand());
        data.put("Model Number", input.getModelNumber());
        data.put("Model Name", input.getModelName());
        data.put("Plating", input.getPlating());
        data.put("Length (CM)", input.getLength());
        data.put("Breadth (CM)", input.getBreadth());
        data.put("Height (CM)", input.getHeight());
        data.put("Weight (KG)", input.getWeight());

        if (input.getAttributes() != null) {
            data.putAll(input.getAttributes());
        }

        data.putAll(ai.fields());

        // User-provided values win over AI-generated values.
        putIfPresent(data, "Seller SKU ID", input.getSellerSkuId());
        putIfPresent(data, "MRP (INR)", input.getMrp());
        putIfPresent(data, "Your selling price (INR)", input.getSellingPrice());
        putIfPresent(data, "Fullfilment by", input.getFulfilment());
        putIfPresent(data, "HSN", input.getHsn());
        putIfPresent(data, "Country Of Origin", input.getCountryOfOrigin());
        putIfPresent(data, "Manufacturer Details", input.getManufacturerDetails());
        putIfPresent(data, "Packer Details", input.getPackerDetails());
        putIfPresent(data, "Tax Code", input.getTaxCode());
        putIfPresent(data, "Brand", input.getBrand());
        putIfPresent(data, "Model Number", input.getModelNumber());
        putIfPresent(data, "Model Name", input.getModelName());
        putIfPresent(data, "Plating", input.getPlating());
        putIfPresent(data, "Length (CM)", input.getLength());
        putIfPresent(data, "Breadth (CM)", input.getBreadth());
        putIfPresent(data, "Height (CM)", input.getHeight());
        putIfPresent(data, "Weight (KG)", input.getWeight());
        putIfPresent(data, "Silver Weight (g)", "NA");
        putIfPresent(data, "Stock", input.getStock());
        putIfPresent(data, "Procurement SLA (DAY)", 2);


        data.put("Main Image URL", driveUrls.getFirst().url());
        for (int i = 1; i < driveUrls.size() && i <= 4; i++) {
            data.put("Other Image URL " + i, driveUrls.get(i).url());
        }
        data.put("Supplier Image", driveUrls.getFirst().url());

        Listing listing = new Listing();
        listing.setSellerSkuId(input.getSellerSkuId());
        listing.setStatus(ListingStatus.EXCEL_PENDING);
        listing.setListingData(data);
        listing.setImageUrls(driveUrls.stream().map(DropboxImageUploadResult::url).toList());
        listing.setAiResponse(ai.rawResponse());

        return repository.save(listing);
    }

    private void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }
}
