package com.example.spacetraderspicyber.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table
@Getter
@Setter
public class Market {

    @Embedded
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
        @ElementCollection(fetch = FetchType.EAGER)
        private List<TradeItem> exports;
        @ElementCollection(fetch = FetchType.EAGER)
        private List<TradeItem> imports;
        @ElementCollection(fetch = FetchType.EAGER)
        private List<TradeItem> exchange;
        @ElementCollection
        private List<Transaction> transactions;
        @OneToMany(cascade = CascadeType.ALL, mappedBy = "market", fetch = FetchType.EAGER)
        private List<TradeGood> tradeGoods = new ArrayList<>();

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
    private List<String> goodsToSell = new ArrayList<>();

    public String getSymbol() {
        return data.getSymbol();
    }

    public void setSymbol(String symbol) {
        data.setSymbol(symbol);
    }

    public void initializeGoodsToSell() {
        if (data != null) {
            goodsToSell = Stream.of(data.getExchange(), data.getExports(), data.getImports())
                    .flatMap(List::stream)
                    .map(MarketData.TradeItem::getSymbol)
                    .distinct()
                    .collect(Collectors.toList());
        }
    }

    public void setData(MarketData data) {
        this.data = data;
        initializeGoodsToSell();
    }
}






