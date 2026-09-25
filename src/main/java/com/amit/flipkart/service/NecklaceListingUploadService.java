package com.amit.flipkart.service;

import com.amit.flipkart.NaariNestConstants;
import com.amit.flipkart.model.*;
import com.amit.flipkart.repository.ListingRepository;
import com.dropbox.core.DbxException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class NecklaceListingUploadService {
    private final ListingRepository repository;
    private final NecklaceOpenAiProductService openAi;
    private final DropboxService dropboxService;

    public NecklaceListingUploadService(ListingRepository repository, NecklaceOpenAiProductService openAi,
                                        DropboxService dropboxService) {
        this.repository = repository;
        this.openAi = openAi;
        this.dropboxService = dropboxService;
    }

    public Listing create(ProductUploadRequest input, MultipartFile[] images) throws IOException, DbxException {
        if (images == null || images.length == 0) throw new IllegalArgumentException("At least one image is required");
        if (repository.findBySellerSkuId(input.getSellerSkuId()).isPresent()) {
            throw new IllegalArgumentException("Seller SKU already exists: " + input.getSellerSkuId());
        }
        NecklaceOpenAiProductService.AiResult ai = openAi.analyze(input, images[0]);
        List<DropboxImageUploadResult> uploaded = dropboxService.uploadProductImages(input.getSellerSkuId(), images);

        Map<String, Object> data = new LinkedHashMap<>();
        put(data, "Seller SKU ID", input.getSellerSkuId());
        put(data, "MRP (INR)", input.getMrp());
        put(data, "Your selling price (INR)", input.getSellingPrice());
        put(data, "Fullfilment by", input.getFulfilment());
        put(data, "HSN", input.getHsn());
        put(data, "Country Of Origin", input.getCountryOfOrigin());
        put(data, "Manufacturer Details", input.getManufacturerDetails());
        put(data, "Packer Details", input.getPackerDetails());
        put(data, "Tax Code", input.getTaxCode());
        put(data, "Brand", input.getBrand());
        put(data, "Model Number", input.getModelNumber());
        put(data, "Length (CM)", NaariNestConstants.packageLength);
        put(data, "Breadth (CM)", NaariNestConstants.packageBreadth);
        put(data, "Height (CM)", NaariNestConstants.packageHeigh);
        put(data, "Weight (KG)", NaariNestConstants.packageWeight);
        put(data, "Stock", input.getStock());
        if (input.getAttributes() != null) data.putAll(input.getAttributes());
        data.putAll(ai.fields());
        // Explicit request values must always override AI output.
        if (input.getAttributes() != null) data.putAll(input.getAttributes());
        put(data, "Seller SKU ID", input.getSellerSkuId());
        put(data, "MRP (INR)", input.getMrp());
        put(data, "Your selling price (INR)", input.getSellingPrice());
        put(data, "Fullfilment by", input.getFulfilment());
        put(data, "HSN", input.getHsn());
        put(data, "Country Of Origin", input.getCountryOfOrigin());
        put(data, "Manufacturer Details", input.getManufacturerDetails());
        put(data, "Packer Details", input.getPackerDetails());
        put(data, "Tax Code", input.getTaxCode());
        put(data, "Brand", input.getBrand());
        put(data, "Model Number", input.getModelNumber());
        put(data, "Stock", input.getStock());
        put(data, "Silver Weight (g)", "NA");
        put(data, "Stock", input.getStock());
        put(data, "Procurement SLA (DAY)", 2);
        put(data, "Procurement type", "Instock");
        put(data, "Shipping provider", "Flipkart");
        put(data, "Diameter (mm)", input.getDiameter());
        put(data, "With Ear Chain",  input.getWithEarChain());
        put(data, "Procurement type", "EXPRESS");

        for (int i = 0; i < uploaded.size() && i < 5; i++) {
            String key = i == 0 ? "Main Image URL" : "Other Image URL " + i;
            data.put(key, publicUrl(uploaded.get(i).url()));
        }
        data.put("Supplier Image", publicUrl(uploaded.getFirst().url()));

        Listing listing = new Listing();
        listing.setSellerSkuId(input.getSellerSkuId());
        listing.setCategory(ListingCategory.NECKLACE_CHAIN);
        listing.setStatus(ListingStatus.EXCEL_PENDING);
        listing.setListingData(data);
        listing.setImageUrls(uploaded.stream().map(DropboxImageUploadResult::url).toList());
        listing.setAiResponse(new ObjectMapper().writeValueAsString(ai.fields()));
        return repository.save(listing);
    }

    private void put(Map<String, Object> data, String key, Object value) { if (value != null) data.put(key, value); }
    private String publicUrl(String url) { return url.replace("dl=1", "raw=1").replace("www.dropbox.com", "dl.dropboxusercontent.com"); }
}
