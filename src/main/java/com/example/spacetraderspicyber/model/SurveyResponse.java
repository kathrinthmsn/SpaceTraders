package com.example.spacetraderspicyber.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SurveyResponse {
    private SurveyData data;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SurveyData {
        private Cooldown cooldown;
        private List<Survey> surveys;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Cooldown {
        private String shipSymbol;
        private int totalSeconds;
        private int remainingSeconds;
        private String expiration;  // Can be replaced with `LocalDateTime` if properly formatted.
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Survey {
        private String signature;
        private String symbol;
        private List<Deposit> deposits;
        private String expiration;  // Can be replaced with `LocalDateTime` if properly formatted.
        private String size;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Deposit {
        private String symbol;
    }
}
