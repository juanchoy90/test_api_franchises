package com.juandavidg.franchise.domain.model.command;

public record UpdateProductStockCommand(
        String storeId,
        String productId,
        Integer quantity
) {}
