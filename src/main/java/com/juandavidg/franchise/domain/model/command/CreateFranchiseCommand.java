package com.juandavidg.franchise.domain.model.command;

public record CreateFranchiseCommand(
        String name,
        String nit,
        String city,
        String country,
        String email
) {}
