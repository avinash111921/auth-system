package com.identity.timing;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import org.springframework.web.context.request.RequestAttributes;

/**
    Times every outgoing gRPC call, start to close,
    under the service's short name:
    auth.risk.v1.RiskService becomes "risk".
    Deadline overruns are recorded too.
 */

public class GrpcTimingInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                               CallOptions options, Channel next) {
        RequestAttributes request = ServerTiming.currentRequest();
        if (request == null) {
            return next.newCall(method, options);
        }
        String metric = metricName(method.getServiceName());
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, options)) {
            @Override
            public void start(Listener<RespT> listener, Metadata headers) {
                long started = System.nanoTime();
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(listener) {
                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        ServerTiming.record(request, metric, System.nanoTime() - started);
                        super.onClose(status, trailers);
                    }
                }, headers);
            }
        };
    }

    private static String metricName(String serviceName) {
        String[] parts = serviceName == null ? new String[0] : serviceName.split("\\.");
        return parts.length > 1 ? parts[1] : "grpc";
    }
}
