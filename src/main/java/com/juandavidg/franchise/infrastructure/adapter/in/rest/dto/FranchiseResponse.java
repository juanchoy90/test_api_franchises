package com.juandavidg.franchise.infrastructure.adapter.in.rest.dto;

public record FranchiseResponse(
        String id,
        String name,
        String nit,
        String city,
        String country,
        String email,
        String status,
        String createdAt,
        String updatedAt
) {}
