package com.juandavidg.franchise.infrastructure.adapter.in.rest.mapper;

import com.juandavidg.franchise.domain.model.Store;
import com.juandavidg.franchise.domain.model.command.CreateStoreCommand;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.CreateStoreRequest;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.StoreResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StoreWebMapper {

    CreateStoreCommand toCommand(CreateStoreRequest request);

    @Mapping(target = "status",    expression = "java(store.getStatus().name().toLowerCase())")
    @Mapping(target = "createdAt", expression = "java(store.getCreatedAt().toString())")
    @Mapping(target = "updatedAt", expression = "java(store.getUpdatedAt().toString())")
    StoreResponse toResponse(Store store);
}
