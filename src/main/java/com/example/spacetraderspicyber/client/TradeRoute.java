package com.example.spacetraderspicyber.client;

import com.example.spacetraderspicyber.model.Fuel;
import com.example.spacetraderspicyber.model.Good;
import com.example.spacetraderspicyber.model.Market;
import com.example.spacetraderspicyber.model.TradeGood;
import com.example.spacetraderspicyber.service.MarketService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static java.lang.Thread.sleep;

@Component
public class TradeRoute {

    @Autowired
    private MarketService marketService;
    @Autowired
    private MarketSearch marketSearch;
    @Autowired
    private SpacetraderClient spacetraderClient;

    //TODO: survey trade Good Prices
    public void tradeRoute(String shipSymbol) throws InterruptedException {
        Set<String> uniqueSymbols = marketService.findAllUniqueSymbols();
        String bestTrade = "";
        double highestDifference = 0;
        Map<String, Double> priceDifferences = new HashMap();
        for (String symbol : uniqueSymbols) {
            List<TradeGood> tradeGoods = marketService.findBySymbol(symbol);
            double lowestPurchasePrice = Double.MAX_VALUE;
            double highestSellPrice = 0;
            double difference = 0;


            for (TradeGood tradeGood : tradeGoods) {
                if(tradeGood.getPurchasePrice() < lowestPurchasePrice) {
                    lowestPurchasePrice = tradeGood.getPurchasePrice();
                }
                if(tradeGood.getSellPrice() > highestSellPrice) {
                    highestSellPrice = tradeGood.getSellPrice();
                }
            }
            difference = highestSellPrice - lowestPurchasePrice;
            priceDifferences.put(symbol, difference);
            if(difference > highestDifference) {
                highestDifference = difference;
                bestTrade = symbol;
            }
        }
        List<TradeGood> tradeGoods = marketService.findTradeGoodsBySymbol(bestTrade);
        TradeGood lowestPurchasePrice = findTradeGoodWithLowestPurchasePrice(tradeGoods).get();
        TradeGood highestSellPrice = findTradeGoodWithHighestSellPrice(tradeGoods).get();


        System.out.println("Best Trade : " + bestTrade + " Purchase Price " + lowestPurchasePrice.getPurchasePrice() + " Sell Price " + highestSellPrice.getSellPrice());
        JSONObject cargo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data").getJSONObject("cargo");
        int capacity = cargo.getInt("capacity");
        int load = cargo.getInt("units");
        int tradeGoodsBought = 0;
        if(lowestPurchasePrice.getTradeVolume() < capacity - load){
            tradeGoodsBought = lowestPurchasePrice.getTradeVolume();
        }
        else {
            tradeGoodsBought = capacity - load;
        }
        Good goodForTrading = Good.builder().symbol(lowestPurchasePrice.getSymbol()).units(tradeGoodsBought).build();

        if(capacity - load >= 10) {
            marketSearch.navigateToMarket(lowestPurchasePrice.getMarket(), shipSymbol);
            marketSearch.updateTradeGoods(new JSONObject(spacetraderClient.viewMarketData(lowestPurchasePrice.getMarket().getSymbol())));
            Market market = marketService.findByName(lowestPurchasePrice.getMarket().getSymbol());
            TradeGood updatedTradeGood = findTradeGoodBySymbol(market.getTradeGoods(), lowestPurchasePrice.getSymbol()).get();
            if(updatedTradeGood.getPurchasePrice() > highestSellPrice.getSellPrice()){
                return;
            }
            spacetraderClient.dockShip(shipSymbol);
            System.out.println(shipSymbol + " purchasing " + goodForTrading);
            spacetraderClient.purchaseCargo(shipSymbol, goodForTrading);
            if(capacity - load > lowestPurchasePrice.getTradeVolume()) {
                goodForTrading.setUnits((capacity -load) - lowestPurchasePrice.getTradeVolume());
                spacetraderClient.purchaseCargo(shipSymbol, goodForTrading);
                System.out.println(shipSymbol + " purchasing " + goodForTrading);
            }
        }

        marketSearch.navigateToMarket(highestSellPrice.getMarket(), shipSymbol);
        marketSearch.updateTradeGoods(new JSONObject(spacetraderClient.viewMarketData(highestSellPrice.getMarket().getSymbol())));
        Market market = marketService.findByName(highestSellPrice.getMarket().getSymbol());
        TradeGood updatedTradeGood = findTradeGoodBySymbol(market.getTradeGoods(), highestSellPrice.getSymbol()).get();
        if(updatedTradeGood.getSellPrice() < lowestPurchasePrice.getPurchasePrice()){
            Optional<TradeGood> tradeGoodWithSecondHighestSellPriceOpt = findTradeGoodWithSecondHighestSellPrice(tradeGoods);
            if(tradeGoodWithSecondHighestSellPriceOpt.isPresent()){
                TradeGood tradeGoodWithSecondHighestSellPrice = tradeGoodWithSecondHighestSellPriceOpt.get();
                if(tradeGoodWithSecondHighestSellPrice.getSellPrice() > highestSellPrice.getSellPrice()){
                    marketSearch.navigateToMarket(tradeGoodWithSecondHighestSellPrice.getMarket(), shipSymbol);
                    marketSearch.updateTradeGoods(new JSONObject(spacetraderClient.viewMarketData(tradeGoodWithSecondHighestSellPrice.getMarket().getSymbol())));
                    this.sellTradeGood(shipSymbol, goodForTrading, tradeGoodsBought, lowestPurchasePrice, capacity, load);
                }
              }
        }
        this.sellTradeGood(shipSymbol, goodForTrading, tradeGoodsBought, lowestPurchasePrice, capacity, load);
    }

    public void sellTradeGood(String shipSymbol, Good goodForTrading, Integer shipPartsBought, TradeGood lowestPurchasePrice, Integer capacity, Integer load) throws InterruptedException {
        spacetraderClient.dockShip(shipSymbol);
        goodForTrading.setUnits(shipPartsBought);
        System.out.println(shipSymbol + " selling " + goodForTrading);
        spacetraderClient.sellCargo(shipSymbol, goodForTrading);
        if(capacity - load > lowestPurchasePrice.getTradeVolume()) {
            goodForTrading.setUnits((capacity -load) - lowestPurchasePrice.getTradeVolume());
            spacetraderClient.sellCargo(shipSymbol, goodForTrading);
            System.out.println(shipSymbol + " selling " + goodForTrading);
        }
    }

    public static Optional<TradeGood> findTradeGoodWithLowestPurchasePrice(List<TradeGood> tradeGoodsList) {
        return tradeGoodsList.stream()
                .min(Comparator.comparingDouble(TradeGood::getPurchasePrice));
    }


    public static Optional<TradeGood> findTradeGoodWithHighestSellPrice(List<TradeGood> tradeGoodsList) {
        return tradeGoodsList.stream()
                .max(Comparator.comparingDouble(TradeGood::getSellPrice));
    }

    public static Optional<TradeGood> findTradeGoodWithSecondHighestSellPrice(List<TradeGood> tradeGoodsList) {
        return tradeGoodsList.stream()
                .sorted(Comparator.comparingDouble(TradeGood::getSellPrice).reversed())
                .skip(1)
                .findFirst();
    }


    public static Optional<TradeGood> findTradeGoodBySymbol(List<TradeGood> tradeGoods, String symbol) {
        return tradeGoods.stream()
                .filter(tradeGood -> symbol.equals(tradeGood.getSymbol()))
                .findFirst();
    }

    //TODO: Variable ships
    public Market findClosestMarket(List<Market> markets, String shipSymbol) throws InterruptedException {
        JSONObject shipInfo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data");
        Integer currentLocationXCoordinate = shipInfo.getJSONObject("nav").getJSONObject("route").getJSONObject("destination").getInt("x");
        Integer currentLocationYCoordinate = shipInfo.getJSONObject("nav").getJSONObject("route").getJSONObject("destination").getInt("y");
        Market closestMarket = null;
        double distance = Double.MAX_VALUE;
        for(Market market: markets) {
            Integer waypointMarketX = new JSONObject(spacetraderClient.getWaypoint(market.getSymbol())).getJSONObject("data").getInt("x");
            Integer waypointMarketY = new JSONObject(spacetraderClient.getWaypoint(market.getSymbol())).getJSONObject("data").getInt("y");

            double distanceToMarket = this.calculateDistance(currentLocationXCoordinate, currentLocationYCoordinate, waypointMarketX, waypointMarketY);
            if(distanceToMarket < distance) {
                closestMarket = market;
                distance = distanceToMarket;
            }
        }
        Integer fuelCapacity = shipInfo.getJSONObject("fuel").getInt("capacity");
        Integer fuelCurrent = shipInfo.getJSONObject("fuel").getInt("current");
        if(distance > fuelCurrent && shipSymbol.equals("SPICYBER-1") || shipSymbol.equals("SPICYBER-6") || shipSymbol.equals("SPICYBER-4") || shipSymbol.equals("SPICYBER-5")) {
            spacetraderClient.dockShip(shipSymbol);
            spacetraderClient.fuelShip(shipSymbol, Fuel.builder().units(fuelCapacity - fuelCurrent).fromCargo(true).build());
            spacetraderClient.orbitShip(shipSymbol);
        }
        return closestMarket;
    }

    private double calculateDistance(double x1, double y1, double x2, double y2) {
        // Implement your distance calculation logic here (e.g., Euclidean distance)
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

}
