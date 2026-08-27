package com.juandavidg.franchise.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateProductRequest(

        @NotBlank(message = "storeId is required")
        String storeId,

        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "code is required")
        String code,

        @NotNull(message = "price is required")
        BigDecimal price,

        @NotBlank(message = "description is required")
        String description,

        @NotBlank(message = "category is required")
        String category,

        @NotNull(message = "stock is required")
        @Min(value = 0, message = "stock must not be negative")
        Integer stock
) {}
