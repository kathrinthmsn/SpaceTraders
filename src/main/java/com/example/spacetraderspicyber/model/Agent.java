package com.example.spacetraderspicyber.model;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Agent {

    private AgentData data;

    @Setter
    @Getter
    @NoArgsConstructor
    public static class AgentData {
        private String accountId;
        private String symbol;
        private String headquarters;
        private int credit;
        private String startingFaction;
        private int shipCount;
    }
}
