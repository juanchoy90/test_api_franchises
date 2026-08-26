package com.juandavidg.franchise.infrastructure.adapter.in.rest.dto;

public record StoreResponse(
        String id,
        String franchiseId,
        String name,
        String address,
        String city,
        String phone,
        String status,
        String createdAt,
        String updatedAt
) {}
