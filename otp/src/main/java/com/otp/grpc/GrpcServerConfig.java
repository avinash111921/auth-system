package com.otp.grpc;

import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ServiceAuthProperties.class)
class GrpcServerConfig {

    @Bean
    @GrpcGlobalServerInterceptor
    ServerInterceptor serviceAuthInterceptor(ServiceAuthProperties properties) {
        return new ServiceAuthServerInterceptor(properties);
    }
}
