package com.identity.grpc;

import com.identity.util.Hmac;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;

/**
 * Signs every outgoing call with HMAC-SHA256 over (caller, full method name, timestamp).
 * Binding the method stops a captured signature being replayed against a different RPC, and
 * the timestamp bounds replay of the same RPC to the server's clock-skew window.
 */
class ServiceAuthClientInterceptor implements ClientInterceptor {

    static final Metadata.Key<String> CALLER = Metadata.Key.of("x-service-caller", Metadata.ASCII_STRING_MARSHALLER);
    static final Metadata.Key<String> TIMESTAMP = Metadata.Key.of("x-service-timestamp", Metadata.ASCII_STRING_MARSHALLER);
    static final Metadata.Key<String> SIGNATURE = Metadata.Key.of("x-service-signature", Metadata.ASCII_STRING_MARSHALLER);

    private final String serviceName;
    private final SecretKeySpec key;

    ServiceAuthClientInterceptor(ServiceAuthProperties properties) {
        this.serviceName = properties.serviceName();
        this.key = Hmac.key(properties.secret(), "service-auth");
    }

    @Override
    public <Q, R> ClientCall<Q, R> interceptCall(MethodDescriptor<Q, R> method, CallOptions options, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, options)) {
            @Override
            public void start(Listener<R> listener, Metadata headers) {
                String timestamp = Long.toString(Instant.now().getEpochSecond());
                String payload = serviceName + "\n" + method.getFullMethodName() + "\n" + timestamp;
                headers.put(CALLER, serviceName);
                headers.put(TIMESTAMP, timestamp);
                headers.put(SIGNATURE, Base64.getUrlEncoder().withoutPadding().encodeToString(Hmac.sign(key, payload)));
                super.start(listener, headers);
            }
        };
    }
}
