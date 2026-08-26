package com.juandavidg.franchise.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class Store {

    private final String id;
    private final String franchiseId;
    private String name;
    private String address;
    private String city;
    private String phone;
    private final StoreStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
}
