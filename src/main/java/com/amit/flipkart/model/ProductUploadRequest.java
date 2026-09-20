package com.amit.flipkart.model;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProductUploadRequest {
    private String sellerSkuId;
    private BigDecimal mrp;
    private BigDecimal sellingPrice;
    private String fulfilment = "Seller";
    private String hsn = "7117";
    private String countryOfOrigin = "India";
    private String manufacturerDetails = "NaariNest_19";
    private String packerDetails = "NaariNest_19";
    private String taxCode = "GST_3";
    private String brand = "NaariNest";
    private String modelNumber;
    private String modelName;

    private String plating;
    private BigDecimal length;
    private BigDecimal breadth;
    private BigDecimal height;
    private BigDecimal weight = BigDecimal.valueOf(0.30);
    private BigDecimal diameter = BigDecimal.valueOf(25.0);
    private String withEarChain = "No";

    public BigDecimal getDiameter() {
        return diameter;
    }

    public void setDiameter(BigDecimal diameter) {
        this.diameter = diameter;
    }

    public String getWithEarChain() {
        return withEarChain;
    }

    public void setWithEarChain(String withEarChain) {
        this.withEarChain = withEarChain;
    }



    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    private Integer stock = 5;


    /*
     * Optional additional user supplied values. These are merged into listingData
     * and can be used to override AI-generated values.
     */
    private Map<String, Object> attributes = new LinkedHashMap<>();

    public String getSellerSkuId() {
        return sellerSkuId;
    }

    public void setSellerSkuId(String sellerSkuId) {
        this.sellerSkuId = sellerSkuId;
    }

    public BigDecimal getMrp() {
        return mrp;
    }

    public void setMrp(BigDecimal mrp) {
        this.mrp = mrp;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public String getFulfilment() {
        return fulfilment;
    }

    public void setFulfilment(String fulfilment) {
        this.fulfilment = fulfilment;
    }

    public String getHsn() {
        return hsn;
    }

    public void setHsn(String hsn) {
        this.hsn = hsn;
    }

    public String getCountryOfOrigin() {
        return countryOfOrigin;
    }

    public void setCountryOfOrigin(String countryOfOrigin) {
        this.countryOfOrigin = countryOfOrigin;
    }

    public String getManufacturerDetails() {
        return manufacturerDetails;
    }

    public void setManufacturerDetails(String manufacturerDetails) {
        this.manufacturerDetails = manufacturerDetails;
    }

    public String getPackerDetails() {
        return packerDetails;
    }

    public void setPackerDetails(String packerDetails) {
        this.packerDetails = packerDetails;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getPlating() {
        return plating;
    }

    public void setPlating(String plating) {
        this.plating = plating;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public BigDecimal getLength() {
        return length;
    }

    public void setLength(BigDecimal length) {
        this.length = length;
    }

    public BigDecimal getBreadth() {
        return breadth;
    }

    public void setBreadth(BigDecimal breadth) {
        this.breadth = breadth;
    }

    public BigDecimal getHeight() {
        return height;
    }

    public void setHeight(BigDecimal height) {
        this.height = height;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

}
