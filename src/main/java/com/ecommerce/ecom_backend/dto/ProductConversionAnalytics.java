package com.ecommerce.ecom_backend.dto;

public class ProductConversionAnalytics {
    private String productId;
    private double conversionRate;

    public ProductConversionAnalytics(String productId, double conversionRate) {
        this.productId = productId;
        this.conversionRate = conversionRate;
    }

    public String getProductId() {
        return productId;
    }

    public double getConversionRate() {
        return conversionRate;
    }
}