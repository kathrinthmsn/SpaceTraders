package com.example.spacetraderspicyber.api.interceptor;


import com.example.spacetraderspicyber.api.authorisation.AuthorisationService;
import feign.RequestInterceptor;
import feign.RequestTemplate;


public class AuthRequestInterceptor implements RequestInterceptor {

    private final AuthorisationService authorisationService;

    public AuthRequestInterceptor(AuthorisationService authorisationService) {
        this.authorisationService = authorisationService;
    }

    @Override
    public void apply(RequestTemplate template) {
        String token = authorisationService.getAuthToken();
        if (token != null && !token.isEmpty()) {
            template.header("Authorization", token);
        } else {
            throw new IllegalStateException("Authorization token is missing or invalid");
        }
    }
}
