package com.identity.client;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
    Lets controllers take a {@link ClientContext} parameter.
    A caller without a valid device cookie gets a fresh one on this
    response, so a login challenge and its OTP step see the same ID.
 */

@Component
@RequiredArgsConstructor
public class ClientContextResolver implements HandlerMethodArgumentResolver {

    private final DeviceCookie deviceCookie;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == ClientContext.class;
    }

    @Override
    public ClientContext resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                                         NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
        String deviceId = deviceCookie.read(request).orElseGet(() -> deviceCookie.issue(response));
        return new ClientContext(request.getRemoteAddr(), deviceId);
    }
}
