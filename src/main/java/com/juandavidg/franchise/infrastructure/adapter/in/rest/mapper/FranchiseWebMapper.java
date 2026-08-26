package com.juandavidg.franchise.infrastructure.adapter.in.rest.mapper;

import com.juandavidg.franchise.domain.model.Franchise;
import com.juandavidg.franchise.domain.model.command.CreateFranchiseCommand;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.CreateFranchiseRequest;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.FranchiseResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FranchiseWebMapper {

    CreateFranchiseCommand toCommand(CreateFranchiseRequest request);

    @Mapping(target = "status",    expression = "java(franchise.getStatus().name().toLowerCase())")
    @Mapping(target = "createdAt", expression = "java(franchise.getCreatedAt().toString())")
    @Mapping(target = "updatedAt", expression = "java(franchise.getUpdatedAt().toString())")
    FranchiseResponse toResponse(Franchise franchise);
}
