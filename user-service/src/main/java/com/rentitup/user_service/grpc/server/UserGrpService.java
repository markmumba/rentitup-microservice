package com.rentitup.user_service.grpc.server;

import com.rentitup.shared.proto.user.UserServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserGrpService extends UserServiceGrpc.UserServiceImplBase {

}
