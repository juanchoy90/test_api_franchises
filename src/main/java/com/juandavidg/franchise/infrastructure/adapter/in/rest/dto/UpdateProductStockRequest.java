package com.juandavidg.franchise.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateProductStockRequest(

        @NotNull(message = "quantity is required")
        Integer quantity
) {}
