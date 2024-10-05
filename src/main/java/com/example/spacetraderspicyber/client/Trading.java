package com.example.spacetraderspicyber.client;

import com.example.spacetraderspicyber.model.Good;
import com.example.spacetraderspicyber.model.Market;
import com.example.spacetraderspicyber.model.TradeGood;
import com.example.spacetraderspicyber.model.WaypointSymbol;
import com.example.spacetraderspicyber.service.MarketService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

@Component
public class Trading {

    @Autowired
    private SpacetraderClient spacetraderClient;
    @Autowired
    private Contracts contracts;
    @Autowired
    private Selling selling;
    @Autowired
    private Extracting extracting;
    @Autowired
    private MarketSearch marketSearch;
    @Autowired
    private MarketService marketService;

    //TODO: Buy Item in another System/ refine Items
    public void buyItemForContract(String shipSymbol) throws InterruptedException {
        if (getShipLoad(shipSymbol) > 5) {
            selling.selling(shipSymbol);
        }
        JSONObject contractInfo = contracts.getContractInfo();
        Good goodForDelivery = contracts.getGoodLeftToDeliverForContract(contractInfo);

        if(this.checkPurchasePrice(shipSymbol, goodForDelivery, contractInfo)){
            contracts.purchaseCargoForContract(shipSymbol);
        }
        else {
            extracting.goToExtractMinerals(shipSymbol);
            contracts.checkContractValidity();
        }
    }

    private int getShipCapacity(String shipSymbol) {
        return new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data").getJSONObject("cargo").getInt("capacity");
    }

    private int getShipLoad(String shipSymbol) {
        return new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data").getJSONObject("cargo").getInt("units");
    }

    public boolean checkPurchasePrice(String shipSymbol, Good goodForDelivery, JSONObject contractInfo) throws InterruptedException {
            if(checkTradeGoods(shipSymbol, goodForDelivery, contractInfo)) {
                return true;
            } else {
                List<Market> markets = marketService.findMarketsByGoodsToSell(goodForDelivery.getSymbol());
                List<TradeGood>tradeGoods = marketService.findTradeGoodsBySymbol(goodForDelivery.getSymbol());
                List<Market> marketsAvailableForBuyingGood = marketService.findMarketsByGoodsToSell(goodForDelivery.getSymbol());
                List<Market> marketsWithTradeGoodNotDiscovered = findMarketsNotInTradeGoods(markets, tradeGoods);
                while (marketsAvailableForBuyingGood.size() > tradeGoods.size()) {
                    Market closestMarket = marketSearch.findClosestMarket(marketsWithTradeGoodNotDiscovered, shipSymbol);
                    marketSearch.navigateToMarket(closestMarket, shipSymbol);
                    JSONObject marketData = new JSONObject(spacetraderClient.viewMarketData(closestMarket.getSymbol()));
                    if (marketData.getJSONObject("data").has("tradeGoods")) {
                        updateTradeGoods(marketData);
                        tradeGoods.add(new TradeGood());
                    }
                    marketsWithTradeGoodNotDiscovered.remove(closestMarket);
                }
                if(checkTradeGoods(shipSymbol, goodForDelivery, contractInfo)) {
                    return true;
                }
            }

        return false;
    }

    public boolean checkTradeGoods(String shipSymbol, Good goodForDelivery, JSONObject contractInfo) throws InterruptedException {
        List<TradeGood> tradeGoods = marketService.findTradeGoodsBySymbol(goodForDelivery.getSymbol());
        List<Market> marketsAvailableForBuyingGood = marketService.findMarketsByGoodsToSell(goodForDelivery.getSymbol());
        if(marketsAvailableForBuyingGood.size() <= tradeGoods.size()) {
            TradeGood tradeGoodWithLowestPrice = findLowestPurchasePrice(tradeGoods);
            double priceToPayForContractGood = tradeGoodWithLowestPrice.getPurchasePrice() * goodForDelivery.getUnits();
            if((getOnFulfilledMoney(contractInfo) > priceToPayForContractGood   && getAgentCredits() > priceToPayForContractGood || (!Good.isMinable(goodForDelivery)) && priceToPayForContractGood < 50000)) {
                System.out.println("Trade Good found with Lowest Price: " + goodForDelivery.getSymbol() + " for " + tradeGoodWithLowestPrice.getPurchasePrice() + "$");
                marketSearch.navigateToMarket(tradeGoodWithLowestPrice.getMarket(), shipSymbol);
                JSONObject marketData = new JSONObject(spacetraderClient.viewMarketData(tradeGoodWithLowestPrice.getMarket().getSymbol()));
                    updateTradeGoods(marketData);
                return true;
            }

        }
        return false;
    }


    public static TradeGood findLowestPurchasePrice(List<TradeGood> tradeGoods) {
        if (tradeGoods == null || tradeGoods.isEmpty()) {
            return null; // or throw an exception, depending on your requirements
        }

        TradeGood lowestPriceTradeGood = tradeGoods.get(0);

        for (TradeGood tradeGood : tradeGoods) {
            if (tradeGood.getPurchasePrice() < lowestPriceTradeGood.getPurchasePrice()) {
                lowestPriceTradeGood = tradeGood;
            }
        }

        return lowestPriceTradeGood;
    }

    public static List<Market> findMarketsNotInTradeGoods(List<Market> markets, List<TradeGood> tradeGoods) {
        List<Market> marketsNotInTradeGoods = new ArrayList<>();

        for (Market market : markets) {
            if (!containsMarketSymbol(tradeGoods, market.getSymbol())) {
                marketsNotInTradeGoods.add(market);
            }
        }

        return marketsNotInTradeGoods;
    }

    private static boolean containsMarketSymbol(List<TradeGood> tradeGoods, String marketSymbol) {
        for (TradeGood tradeGood : tradeGoods) {
            if (tradeGood.getMarket() != null && marketSymbol.equals(tradeGood.getMarket().getSymbol())) {
                return true;
            }
        }
        return false;
    }


    private void updateTradeGoods(JSONObject marketData) {
        if (marketData.getJSONObject("data").has("tradeGoods")) {
            marketSearch.updateTradeGoods(marketData);
        }
    }

    private int getOnFulfilledMoney(JSONObject contractInfo) {
        JSONArray contractsArray = contractInfo.getJSONArray("data");
        return contractsArray.getJSONObject(contractsArray.length() - 1).getJSONObject("terms").getJSONObject("payment").getInt("onFulfilled");
    }

    private int getAgentCredits() {
        return new JSONObject(spacetraderClient.seeAgent()).getJSONObject("data").getInt("credits");
    }

    public TradeGood getTradeGoodByGoodSymbol(JSONObject marketData, Good goodForDelivery) {
        if (marketData.getJSONObject("data").has("tradeGoods")) {
            Gson gson = new Gson();
            Type tradeGoodListType = new TypeToken<List<TradeGood>>() {}.getType();
            List<TradeGood> tradeGoods = gson.fromJson(marketData.getJSONObject("data").getJSONArray("tradeGoods").toString(), tradeGoodListType);
            return getTradeGoodBySymbol(tradeGoods, goodForDelivery.getSymbol());
        }
        TradeGood tradeGood = new TradeGood();
        tradeGood.setPurchasePrice(Double.MAX_VALUE);
        return tradeGood;
    }

    public static TradeGood getTradeGoodBySymbol(List<TradeGood> tradeGoods, String goodSymbol) {
        for (TradeGood tradeGood : tradeGoods) {
            if (tradeGood.getSymbol().equals(goodSymbol)) {
                return tradeGood; // Found a trade good with the target symbol
            }
        }
        return null; // Symbol not found in any trade good
    }

}

