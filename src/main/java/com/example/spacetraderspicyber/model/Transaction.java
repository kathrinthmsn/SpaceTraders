package com.example.spacetraderspicyber.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class Transaction {

    private String waypointSymbol;
    private String shipSymbol;
    private String tradeSymbol;
    private String type;
    private int units;
    private int pricePerUnit;
    private int totalPrice;
    private String timestamp;

}
