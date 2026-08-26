package com.juandavidg.franchise.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class Franchise {

    private final String id;
    private String name;
    private final String nit;
    private String city;
    private String country;
    private String email;
    private final FranchiseStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
}
