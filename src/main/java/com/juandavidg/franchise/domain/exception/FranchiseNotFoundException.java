package com.juandavidg.franchise.domain.exception;

public class FranchiseNotFoundException extends RuntimeException {

    private final String franchiseId;

    public FranchiseNotFoundException(String franchiseId) {
        super(String.format("Franchise '%s' not found", franchiseId));
        this.franchiseId = franchiseId;
    }

    public String getFranchiseId() {
        return franchiseId;
    }
}
