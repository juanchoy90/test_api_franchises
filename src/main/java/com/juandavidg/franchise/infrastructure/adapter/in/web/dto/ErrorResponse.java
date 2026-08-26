package com.juandavidg.franchise.infrastructure.adapter.in.web.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        int status,
        String code,
        String message,
        Instant timestamp,
        List<ErrorDetail> errors
) {}
