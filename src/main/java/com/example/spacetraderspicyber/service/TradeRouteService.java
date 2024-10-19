package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
import com.example.spacetraderspicyber.model.Cargo;
import com.example.spacetraderspicyber.model.Good;
import com.example.spacetraderspicyber.model.Market;
import com.example.spacetraderspicyber.model.TradeGood;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
public class TradeRouteService {

    @Autowired
    private MarketService marketService;
    @Autowired
    private MarketSearchService marketSearchService;
    @Autowired
    private SpacetraderClient spacetraderClient;

    public void tradeRoute(String shipSymbol) throws InterruptedException {
        String bestTradeGood = findBestTradeGood();
        if (bestTradeGood.isEmpty()) {
            log.warn("No profitable trade routes found.");
            return;
        }

        List<TradeGood> tradeGoods = marketService.findTradeGoodsBySymbol(bestTradeGood);
        Optional<TradeGood> lowestPurchaseOpt = findTradeGoodWithLowestPurchasePrice(tradeGoods);
        Optional<TradeGood> highestSellOpt = findTradeGoodWithHighestSellPrice(tradeGoods);

        if (lowestPurchaseOpt.isEmpty() || highestSellOpt.isEmpty()) {
            log.warn("Couldn't find valid trade goods for the best trade route.");
            return;
        }

        TradeGood lowestPurchase = lowestPurchaseOpt.get();
        TradeGood highestSell = highestSellOpt.get();

        log.info("Best Trade Symbol: {}, Purchase Price: {}, Sell Price: {}", bestTradeGood,
                lowestPurchase.getPurchasePrice(), highestSell.getSellPrice());

        Cargo cargo = spacetraderClient.seeShipDetails(shipSymbol).getData().getCargo();
        int availableCapacity = cargo.getCapacity() - cargo.getUnits();
        int tradeGoodsToBuy = Math.min(lowestPurchase.getTradeVolume(), availableCapacity);

        if (availableCapacity >= 10) {
            navigateAndUpdateMarket(lowestPurchase.getMarket(), shipSymbol);
            purchaseCargo(shipSymbol, lowestPurchase.getSymbol(), tradeGoodsToBuy);
            navigateAndUpdateMarket(highestSell.getMarket(), shipSymbol);
            sellCargo(shipSymbol, lowestPurchase.getSymbol(), tradeGoodsToBuy);
        } else {
            log.warn("Not enough cargo space available. Available: {}, Required: 10", availableCapacity);
        }
    }

    private String findBestTradeGood() {
        Set<String> allTradeGoods = marketService.findAllUniqueTradeGoods();
        String bestTradeSymbol = "";
        double highestDifference = 0;
        for (String tradeGood : allTradeGoods) {
            List<TradeGood> tradeGoods = marketService.findByTradeGoodSymbol(tradeGood);
            double priceDifference = getPriceDifference(tradeGoods);
            if (priceDifference > highestDifference) {
                highestDifference = priceDifference;
                bestTradeSymbol = tradeGood;
            }
        }
        return bestTradeSymbol;
    }

    private void purchaseCargo(String shipSymbol, String goodSymbol, int quantity) throws InterruptedException {
        Good goodToBuy = Good.builder().symbol(goodSymbol).units(quantity).build();
        log.info("{} purchasing {} units of {}", shipSymbol, quantity, goodSymbol);
        spacetraderClient.purchaseCargo(shipSymbol, goodToBuy);
    }

    private void sellCargo(String shipSymbol, String goodSymbol, int quantity) throws InterruptedException {
        Good goodToSell = Good.builder().symbol(goodSymbol).units(quantity).build();
        log.info("{} selling {} units of {}", shipSymbol, quantity, goodSymbol);
        spacetraderClient.sellCargo(shipSymbol, goodToSell);
    }

    private void navigateAndUpdateMarket(Market market, String shipSymbol) throws InterruptedException {
        marketSearchService.navigateToMarket(market, shipSymbol);
        marketSearchService.updateTradeGoods(spacetraderClient.viewMarketData(market.getSymbol()).getData());
        log.info("{} navigated to market: {}", shipSymbol, market.getSymbol());
    }

    private static double getPriceDifference(List<TradeGood> tradeGoods) {
        double lowestPurchasePrice = tradeGoods.stream()
                .mapToDouble(TradeGood::getPurchasePrice)
                .min()
                .orElse(Double.MAX_VALUE);

        double highestSellPrice = tradeGoods.stream()
                .mapToDouble(TradeGood::getSellPrice)
                .max()
                .orElse(0);

        return highestSellPrice - lowestPurchasePrice;
    }

    public static Optional<TradeGood> findTradeGoodWithLowestPurchasePrice(List<TradeGood> tradeGoodsList) {
        return tradeGoodsList.stream()
                .min(Comparator.comparingDouble(TradeGood::getPurchasePrice));
    }

    public static Optional<TradeGood> findTradeGoodWithHighestSellPrice(List<TradeGood> tradeGoodsList) {
        return tradeGoodsList.stream()
                .max(Comparator.comparingDouble(TradeGood::getSellPrice));
    }
}
