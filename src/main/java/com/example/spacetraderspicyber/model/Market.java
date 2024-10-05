package com.example.spacetraderspicyber.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

import java.util.List;

@Entity
@Table
@Getter
@Setter
public class Market {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String symbol;
    @ElementCollection
    private List<String> goodsToSell;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "market", fetch = FetchType.EAGER)
    private List<TradeGood> tradeGoods;
}






