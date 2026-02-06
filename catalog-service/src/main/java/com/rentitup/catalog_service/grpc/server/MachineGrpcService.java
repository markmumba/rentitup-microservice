package com.rentitup.catalog_service.grpc.server;

import com.rentitup.catalog_service.mapper.CatalogMapper;
import com.rentitup.shared.proto.catalog.CatalogServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class MachineGrpcService extends CatalogServiceGrpc.CatalogServiceImplBase {

}
