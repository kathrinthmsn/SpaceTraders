package com.example.spacetraderspicyber.api;

import com.example.spacetraderspicyber.api.authorisation.AuthorisationService;
import com.example.spacetraderspicyber.api.interceptor.AuthRequestInterceptor;
import feign.Logger;
import feign.okhttp.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    private final AuthorisationService authorisationService;

    @Value("${api.call.delay:500}")
    private long delay;

    public FeignConfig(AuthorisationService authorisationService) {
        this.authorisationService = authorisationService;
    }


    @Bean
    public AuthRequestInterceptor authFeign() {
        return new AuthRequestInterceptor(authorisationService);
    }

    @Bean(name = "customDelayedFeignInterceptor")
    public DelayedFeignInterceptor delayedFeignInterceptor() {
        return new DelayedFeignInterceptor(delay);
    }

    @Bean
    public OkHttpClient client() {
        return new OkHttpClient();
    }

    @Bean
    Logger.Level feignLogger() {
        return Logger.Level.FULL;
    }
}
