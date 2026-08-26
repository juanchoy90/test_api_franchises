package com.juandavidg.franchise.infrastructure.adapter.in.web.dto;

public record ErrorDetail(
        String field,
        String reason
) {}
