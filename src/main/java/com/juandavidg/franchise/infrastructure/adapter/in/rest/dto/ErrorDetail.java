package com.juandavidg.franchise.infrastructure.adapter.in.rest.dto;

public record ErrorDetail(
        String field,
        String reason
) {}
