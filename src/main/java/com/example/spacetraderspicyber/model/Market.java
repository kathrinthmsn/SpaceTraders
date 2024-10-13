package com.example.spacetraderspicyber.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table
@Getter
@Setter
public class Market {

    private MarketData data;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Setter
    @NoArgsConstructor
    @Embeddable
    public static class MarketData {
        @Column(unique = true)
        private String symbol;
        @ElementCollection
        private List<TradeItem> exports;
        @ElementCollection
        private List<TradeItem> imports;
        @ElementCollection
        private List<TradeItem> exchange;
        @ElementCollection
        private List<Transaction> transactions;
        @ElementCollection
        private List<TradeGood> tradeGoods;

        @Getter
        @Setter
        @NoArgsConstructor
        @Embeddable
        public static class TradeItem {
            private String symbol;
            private String name;
            private String description;
        }
    }

    @ElementCollection
    private List<String> goodsToSell;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "market", fetch = FetchType.EAGER)
    private List<TradeGood> tradeGoods;

    public String getSymbol() {
        return data.getSymbol();
    }

    public void setSymbol(String symbol) {
        data.setSymbol(symbol);
    }
}






