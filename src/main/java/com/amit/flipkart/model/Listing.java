package com.amit.flipkart.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.*;

@Entity
@Table(name = "listing")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_sku_id", nullable = false, unique = true)
    private String sellerSkuId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingStatus status = ListingStatus.EXCEL_PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "listing_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> listingData = new LinkedHashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_urls", columnDefinition = "jsonb")
    private List<String> imageUrls = new ArrayList<>();

    @Column(name = "ai_response", columnDefinition = "text")
    private String aiResponse;

    @Column(name = "excel_file")
    private String excelFile;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public String getSellerSkuId() { return sellerSkuId; }
    public void setSellerSkuId(String sellerSkuId) { this.sellerSkuId = sellerSkuId; }
    public ListingStatus getStatus() { return status; }
    public void setStatus(ListingStatus status) { this.status = status; }
    public Map<String, Object> getListingData() { return listingData; }
    public void setListingData(Map<String, Object> listingData) { this.listingData = listingData; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public String getAiResponse() { return aiResponse; }
    public void setAiResponse(String aiResponse) { this.aiResponse = aiResponse; }
    public String getExcelFile() { return excelFile; }
    public void setExcelFile(String excelFile) { this.excelFile = excelFile; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
