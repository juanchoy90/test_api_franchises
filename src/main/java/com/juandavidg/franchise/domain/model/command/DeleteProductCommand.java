package com.juandavidg.franchise.domain.model.command;

public record DeleteProductCommand(
        String storeId,
        String productId
) {}
