package com.juandavidg.franchise.domain.exception;

public class ProductNotFoundException extends RuntimeException {

    private final String storeId;
    private final String productId;

    public ProductNotFoundException(String storeId, String productId) {
        super(String.format("Product '%s' not found in store '%s'", productId, storeId));
        this.storeId = storeId;
        this.productId = productId;
    }

    public String getStoreId() {
        return storeId;
    }

    public String getProductId() {
        return productId;
    }
}
