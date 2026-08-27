package com.juandavidg.franchise.domain.model.command;

import java.math.BigDecimal;

public record CreateProductCommand(
        String storeId,
        String name,
        String code,
        BigDecimal price,
        String description,
        String category,
        Integer stock
) {}
