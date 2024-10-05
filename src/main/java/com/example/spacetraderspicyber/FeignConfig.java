package com.example.spacetraderspicyber;

import com.example.spacetraderspicyber.authorisation.ApiAuthorisationService;
import com.example.spacetraderspicyber.interceptor.AuthRequestInterceptor;
import feign.Logger;
import feign.okhttp.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    AuthRequestInterceptor authFeign() {
        return new AuthRequestInterceptor(new ApiAuthorisationService());
    }

    @Bean(name = "customDelayedFeignInterceptor")
    public DelayedFeignInterceptor delayedFeignInterceptor() {
        return new DelayedFeignInterceptor();
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
