package com.example.spacetraderspicyber.interceptor;


import com.example.spacetraderspicyber.authorisation.ApiAuthorisationService;
import com.example.spacetraderspicyber.authorisation.AuthorisationService;
import feign.RequestInterceptor;
import feign.RequestTemplate;


public class AuthRequestInterceptor implements RequestInterceptor {

    private ApiAuthorisationService authTokenService;

    public AuthRequestInterceptor(ApiAuthorisationService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header("Authorization", authTokenService.getAuthToken());
    }
}
