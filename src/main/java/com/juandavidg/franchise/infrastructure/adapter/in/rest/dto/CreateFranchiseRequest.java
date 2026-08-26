package com.juandavidg.franchise.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateFranchiseRequest(

        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "nit is required")
        String nit,

        @NotBlank(message = "city is required")
        String city,

        @NotBlank(message = "country is required")
        String country,

        @Email(message = "email must be a valid email address")
        @NotBlank(message = "email is required")
        String email
) {}
