package com.juandavidg.franchise.domain.model.command;

public record CreateStoreCommand(
        String franchiseId,
        String name,
        String address,
        String city,
        String phone
) {}
