package com.example.spacetraderspicyber.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class Contracts {

    private List<Contract> data;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Contract {
        private String id;
        private String factionSymbol;
        private String type;
        private Terms terms;
        private boolean accepted;
        private boolean fulfilled;
        private String expiration;
        private String deadlineToAccept;

        @Getter
        @Setter
        public static class Terms {
            private String deadline;
            private Payment payment;
            private List<Deliver> deliver;

            @Getter
            @Setter
            public static class Payment {
                private int onAccepted;
                private int onFulfilled;
            }

            @Getter
            @Setter
            public static class Deliver {
                private String tradeSymbol;
                private String destinationSymbol;
                private int unitsRequired;
                private int unitsFulfilled;
            }
        }
    }
}
