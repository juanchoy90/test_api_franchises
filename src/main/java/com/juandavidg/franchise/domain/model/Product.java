package com.juandavidg.franchise.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class Product {

    private final String id;
    private final String storeId;
    private final String franchiseId;
    private String name;
    private String code;
    private BigDecimal price;
    private String description;
    private String category;
    private Integer stock;
    private final ProductStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
}
