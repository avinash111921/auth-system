package com.identity.timing;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;

/**
    Records time spent in Spring Data repository calls as "db",
    without touching the services.
    The interceptor goes outermost, so a repository's own transaction,
    commit included, is timed.
 */

@Component
class RepositoryTimingPostProcessor implements BeanPostProcessor {

    private static final MethodInterceptor TIMING = invocation -> {
        RequestAttributes request = ServerTiming.currentRequest();
        long start = System.nanoTime();
        try {
            return invocation.proceed();
        } finally {
            ServerTiming.record(request, "db", System.nanoTime() - start);
        }
    };

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (!(bean instanceof Repository<?, ?>)) {
            return bean;
        }
        if (bean instanceof Advised advised && !advised.isFrozen()) {
            advised.addAdvice(0, TIMING);
            return bean;
        }
        ProxyFactory proxy = new ProxyFactory(bean);
        proxy.addAdvice(TIMING);
        return proxy.getProxy();
    }
}
