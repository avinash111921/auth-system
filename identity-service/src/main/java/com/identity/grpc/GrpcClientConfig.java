package com.identity.grpc;

import com.identity.timing.GrpcTimingInterceptor;
import io.grpc.ClientInterceptor;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class GrpcClientConfig {

    @Bean
    @GrpcGlobalClientInterceptor
    ClientInterceptor serviceAuthInterceptor(ServiceAuthProperties properties) {
        return new ServiceAuthClientInterceptor(properties);
    }

    @Bean
    @GrpcGlobalClientInterceptor
    ClientInterceptor timingInterceptor() {
        return new GrpcTimingInterceptor();
    }
}
