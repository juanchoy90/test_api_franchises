package com.juandavidg.franchise.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;

public record ProductResponse(
        String id,
        String storeId,
        String name,
        String code,
        BigDecimal price,
        String description,
        String category,
        Integer stock,
        String status,
        String createdAt,
        String updatedAt
) {}
