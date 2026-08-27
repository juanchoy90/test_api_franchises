package com.juandavidg.franchise.infrastructure.adapter.in.rest.mapper;

import com.juandavidg.franchise.domain.model.Product;
import com.juandavidg.franchise.domain.model.command.CreateProductCommand;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.CreateProductRequest;
import com.juandavidg.franchise.infrastructure.adapter.in.rest.dto.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductWebMapper {

    CreateProductCommand toCommand(CreateProductRequest request);

    @Mapping(target = "status",    expression = "java(product.getStatus().name().toLowerCase())")
    @Mapping(target = "createdAt", expression = "java(product.getCreatedAt().toString())")
    @Mapping(target = "updatedAt", expression = "java(product.getUpdatedAt().toString())")
    ProductResponse toResponse(Product product);
}
