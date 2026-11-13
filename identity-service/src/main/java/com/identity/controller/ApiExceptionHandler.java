package com.identity.controller;

import com.identity.error.AuthException;
import com.identity.error.ErrorCode;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Locale;

/**
 * The single place where failures become HTTP responses (RFC 9457 problem details). Clients
 * branch on the stable {@code code} property; exception messages never reach the body.
 * Validation and other framework errors are already handled by the superclass.
 */
@RestControllerAdvice
@Slf4j
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AuthException.class)
    ProblemDetail handleAuth(AuthException ex) {
        return problem(ex.code());
    }

    @ExceptionHandler(StatusRuntimeException.class)
    ProblemDetail handleDownstream(StatusRuntimeException ex) {
        return switch (ex.getStatus().getCode()) {
            case UNAVAILABLE, DEADLINE_EXCEEDED -> {
                log.warn("downstream call failed: {}", ex.getStatus().getCode());
                yield problem(ErrorCode.TEMPORARILY_UNAVAILABLE);
            }
            default -> {
                log.error("downstream call failed", ex);
                yield problem(ErrorCode.INTERNAL);
            }
        };
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("unhandled exception", ex);
        return problem(ErrorCode.INTERNAL);
    }

    private static ProblemDetail problem(ErrorCode code) {
        ProblemDetail detail = ProblemDetail.forStatus(code.status());
        detail.setProperty("code", code.name().toLowerCase(Locale.ROOT));
        return detail;
    }
}
