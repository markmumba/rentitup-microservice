package com.rentitup.common.grpc.client;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GrpcClient {

	String value();
}
