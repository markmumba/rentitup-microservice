package com.rentitup.common.grpc.server;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.Set;


@Setter
@Getter
@ConfigurationProperties(prefix = "grpc.auth")
public class GrpcAuthProperties {


	private Set<String> publicMethods = new HashSet<>();


	private boolean enabled = true;

}
