package com.juandavidg.franchise.domain.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final String field;
    private final String value;

    public ResourceNotFoundException(String resourceType, String field, String value) {
        super(String.format("%s with %s '%s' not found", resourceType, field, value));
        this.resourceType = resourceType;
        this.field = field;
        this.value = value;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}
