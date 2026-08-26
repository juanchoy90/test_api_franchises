package com.juandavidg.franchise.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateStoreRequest(

        @NotBlank(message = "franchiseId is required")
        String franchiseId,

        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "address is required")
        String address,

        @NotBlank(message = "city is required")
        String city,

        @NotBlank(message = "phone is required")
        String phone
) {}
