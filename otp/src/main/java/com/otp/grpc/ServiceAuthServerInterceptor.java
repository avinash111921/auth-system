package com.otp.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
    Rejects any call that isn't signed by a known internal caller,
    or that targets a gRPC service the caller isn't allowed to use.
    The signature is HMAC-SHA256 over "caller\n full-method-name\n epoch-seconds",
    produced by the caller's client interceptor.
 */
class ServiceAuthServerInterceptor implements ServerInterceptor {

    static final Metadata.Key<String> CALLER = Metadata.Key.of("x-service-caller", Metadata.ASCII_STRING_MARSHALLER);
    static final Metadata.Key<String> TIMESTAMP = Metadata.Key.of("x-service-timestamp", Metadata.ASCII_STRING_MARSHALLER);
    static final Metadata.Key<String> SIGNATURE = Metadata.Key.of("x-service-signature", Metadata.ASCII_STRING_MARSHALLER);

    private static final long MAX_CLOCK_SKEW_SECONDS = 30;
    private static final int MIN_KEY_BYTES = 32;
    private static final String HEALTH_SERVICE = "grpc.health.v1.Health";

    private final Map<String, Caller> callers;

    ServiceAuthServerInterceptor(ServiceAuthProperties properties) {
        this.callers = properties.trustedCallers().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                        e -> new Caller(key(e.getKey(), e.getValue().secret()), Set.copyOf(e.getValue().allowedServices()))));
    }

    @Override
    public <Q, R> ServerCall.Listener<Q> interceptCall(ServerCall<Q, R> call, Metadata headers, ServerCallHandler<Q, R> next) {
        var method = call.getMethodDescriptor();
        if (HEALTH_SERVICE.equals(method.getServiceName()) || isAuthorized(method.getServiceName(), method.getFullMethodName(), headers)) {
            return next.startCall(call, headers);
        }
        call.close(Status.PERMISSION_DENIED.withDescription("caller not authorized"), new Metadata());
        return new ServerCall.Listener<>() {
        };
    }

    private boolean isAuthorized(String service, String fullMethodName, Metadata headers) {
        String callerName = headers.get(CALLER);
        String timestamp = headers.get(TIMESTAMP);
        String signature = headers.get(SIGNATURE);
        Caller caller = callerName == null ? null : callers.get(callerName);
        if (caller == null || timestamp == null || signature == null || !caller.allowedServices().contains(service)) {
            return false;
        }
        try {
            long sentAt = Long.parseLong(timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - sentAt) > MAX_CLOCK_SKEW_SECONDS) {
                return false;
            }
            byte[] expected = sign(caller.key(), callerName + "\n" + fullMethodName + "\n" + timestamp);
            return MessageDigest.isEqual(expected, Base64.getUrlDecoder().decode(signature));
        } catch (IllegalArgumentException malformed) {
            return false;
        }
    }

    private static SecretKeySpec key(String callerName, String secret) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("service-auth secret for " + callerName + " must be at least " + MIN_KEY_BYTES + " bytes");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    private static byte[] sign(SecretKeySpec key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }

    private record Caller(SecretKeySpec key, Set<String> allowedServices) {
    }
}
