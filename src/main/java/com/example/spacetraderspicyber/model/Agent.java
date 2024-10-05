package com.example.spacetraderspicyber.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class Agent {

    private String symbol = "SPICYBER";
    private String faction = "COSMIC";
}
