package com.movie.booking.catalog_service.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final String resource;
    private final UUID id;

    public ResourceNotFoundException(String resource, UUID id) {
        super(resource + " " + id + " not found");
        this.resource = resource;
        this.id = id;
    }
}
