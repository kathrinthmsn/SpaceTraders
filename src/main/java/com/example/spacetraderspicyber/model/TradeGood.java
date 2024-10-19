package com.example.spacetraderspicyber.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@Table
public class TradeGood {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;
    private String type;
    private int tradeVolume;
    private String supply;
    private String activity;
    private double purchasePrice;
    private double sellPrice;

    @ManyToOne
    @JoinColumn(name = "market_id")
    private Market market;

}
