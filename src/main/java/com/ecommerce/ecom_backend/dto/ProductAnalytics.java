package com.ecommerce.ecom_backend.dto;

public class ProductAnalytics {
    private String productId;
    private long count;

    public ProductAnalytics(String productId, long count) {
        this.productId = productId;
        this.count = count;
    }

    public String getProductId() {
        return productId;
    }

    public long getCount() {
        return count;
    }
}