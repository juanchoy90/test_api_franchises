package com.juandavidg.franchise.domain.exception;

public class InsufficientStockException extends RuntimeException {

    private final String storeId;
    private final String productId;
    private final int requestedQuantity;

    public InsufficientStockException(String storeId, String productId, int requestedQuantity) {
        super(String.format("Product '%s' in store '%s' does not have enough stock for the requested change of %d",
                productId, storeId, requestedQuantity));
        this.storeId = storeId;
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
    }

    public String getStoreId() {
        return storeId;
    }

    public String getProductId() {
        return productId;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }
}
