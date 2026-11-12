package com.otp.grpc;

import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;

/** The one place exceptions become gRPC statuses.
    Descriptions are fixed strings, never exception messages.
 */

@GrpcAdvice
@Slf4j
public class GrpcErrorAdvice {
    @GrpcExceptionHandler
    public Status handleInvalidArgument(IllegalArgumentException e) {
        return Status.INVALID_ARGUMENT.withDescription("invalid request");
    }
    @GrpcExceptionHandler
    public Status handleUnexpected(Exception e) {
        log.error("unhandled gRPC failure", e);
        return Status.INTERNAL.withDescription("internal error");
    }
}
