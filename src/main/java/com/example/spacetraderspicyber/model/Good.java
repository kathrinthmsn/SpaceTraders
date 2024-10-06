package com.example.spacetraderspicyber.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Good {

    private String symbol;
    private Integer units;
    private String shipSymbol;
    private String tradeSymbol;

    @Override
    public String toString() {
        return "Good{" +
                "symbol='" + symbol + '\'' +
                ", units=" + units +
                '}';
    }

    public String toStringContracts() {
        return "Good{" +
                "symbol='" + tradeSymbol + '\'' +
                ", units=" + units +
                '}';
    }

    //TODO: For Mining Gold/Silver-> go to different Asteroid
    public static boolean isNotMinable(Good goodForDelivery) {
        List<String> minableGoods = List.of("ALUMINUM_ORE", "COPPER_ORE","IRON_ORE","SILVER_ORE", "GOLD_ORE", "PLATINUM_ORE", "ICE_WATER", "QUARTZ_SAND", "SILICON_CRYSTALS");
        return !minableGoods.contains(goodForDelivery.tradeSymbol);
    }
}
