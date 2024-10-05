package com.example.spacetraderspicyber;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class DelayedFeignInterceptor implements RequestInterceptor {

    @Value("${api.call.delay:500}")
    final long delay = 500;

    @Override
    public void apply(RequestTemplate requestTemplate) {
        delayBeforeCall();
    }

    private void delayBeforeCall() {
        try {
            TimeUnit.MILLISECONDS.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error while delaying the API call", e);
        }
    }
}