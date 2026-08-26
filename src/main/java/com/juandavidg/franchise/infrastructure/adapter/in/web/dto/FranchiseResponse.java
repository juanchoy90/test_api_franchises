package com.juandavidg.franchise.infrastructure.adapter.in.web.dto;

import java.time.Instant;

public record FranchiseResponse(
        String id,
        String name,
        String nit,
        String city,
        String country,
        String email,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
