package com.example.spacetraderspicyber.api.authorisation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ApiAuthorisationService implements AuthorisationService {

    @Value("${api.auth.token}")
    private String authToken;

    @Override
    public String getAuthToken() {
        return String.format("Bearer %s", authToken);
    }
}
