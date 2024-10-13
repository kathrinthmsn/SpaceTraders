package com.example.spacetraderspicyber.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ExtractionReport {

    private ExtractedResource data;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ExtractedResource {
        private Cooldown cooldown;
        private Extraction extraction;
        private Cargo cargo;
        private List<Event> events;

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Cooldown {
            private String shipSymbol;
            private int totalSeconds;
            private int remainingSeconds;
            private String expiration;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Extraction {
            private String shipSymbol;
            private Yield yield;

            @Getter
            @Setter
            @NoArgsConstructor
            public static class Yield {
                private String symbol;
                private int units;
            }
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Event {
            private String symbol;
            private String component;
            private String name;
            private String description;
        }
    }
}
