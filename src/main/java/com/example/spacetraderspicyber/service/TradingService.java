package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
import com.example.spacetraderspicyber.model.Contracts.Contract;
import com.example.spacetraderspicyber.model.Good;
import com.example.spacetraderspicyber.model.Market;
import com.example.spacetraderspicyber.model.Market.MarketData;
import com.example.spacetraderspicyber.model.TradeGood;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TradingService {

    @Autowired
    private SpacetraderClient spacetraderClient;
    @Autowired
    private ContractsService contractsService;
    @Autowired
    private SellingService sellingService;
    @Autowired
    private ExtractingService extractingService;
    @Autowired
    private MarketSearchService marketSearchService;
    @Autowired
    private MarketService marketService;

    public void buyItemForContract(String shipSymbol) throws InterruptedException {
        if (getShipLoad(shipSymbol) > 5) {
            sellingService.selling(shipSymbol);
        }
        List<Contract> contracts = contractsService.getContractInfo();
        Good goodForDelivery = contractsService.getGoodLeftToDeliverForContract(contracts);

        if (checkPurchasePrice(shipSymbol, goodForDelivery, contracts)) {
            contractsService.purchaseCargoForContract(shipSymbol);
        } else {
            extractingService.goToExtractMinerals(shipSymbol);
            contractsService.checkContractValidity();
        }
    }

    private int getShipLoad(String shipSymbol) {
        return spacetraderClient.seeShipDetails(shipSymbol).getData().getCargo().getUnits();
    }

    private boolean checkPurchasePrice(String shipSymbol, Good goodForDelivery, List<Contract> contracts) throws InterruptedException {
        if (checkTradeGoods(shipSymbol, goodForDelivery, contracts)) {
            return true;
        } else {
            List<Market> markets = marketService.findMarketsByGoodsToSell(goodForDelivery.getSymbol());
            List<TradeGood> tradeGoods = marketService.findTradeGoodsBySymbol(goodForDelivery.getSymbol());
            List<Market> marketsAvailableForBuyingGood = marketService.findMarketsByGoodsToSell(goodForDelivery.getSymbol());
            List<Market> marketsWithTradeGoodNotDiscovered = findMarketsNotInTradeGoods(markets, tradeGoods);
            while (marketsAvailableForBuyingGood.size() > tradeGoods.size()) {
                Market closestMarket = marketSearchService.findClosestMarket(marketsWithTradeGoodNotDiscovered, shipSymbol);
                marketSearchService.navigateToMarket(closestMarket, shipSymbol);
                MarketData marketData = spacetraderClient.viewMarketData(closestMarket.getSymbol()).getData();
                if (!marketData.getTradeGoods().isEmpty()) {
                    updateTradeGoods(marketData);
                    tradeGoods.add(new TradeGood());
                }
                marketsWithTradeGoodNotDiscovered.remove(closestMarket);
            }
            return checkTradeGoods(shipSymbol, goodForDelivery, contracts);
        }
    }

    public boolean checkTradeGoods(String shipSymbol, Good goodForDelivery, List<Contract> contracts) throws InterruptedException {
        List<TradeGood> tradeGoods = marketService.findTradeGoodsBySymbol(goodForDelivery.getSymbol());
        List<Market> marketsAvailableForBuyingGood = marketService.findMarketsByGoodsToSell(goodForDelivery.getSymbol());
        if (marketsAvailableForBuyingGood.size() <= tradeGoods.size()) {
            TradeGood tradeGoodWithLowestPrice = findLowestPurchasePrice(tradeGoods);
            double priceToPayForContractGood = tradeGoodWithLowestPrice.getPurchasePrice() * goodForDelivery.getUnits();
            if ((getOnFulfilledMoney(contracts) > priceToPayForContractGood && getAgentCredits() > priceToPayForContractGood || (Good.isNotMinable(goodForDelivery)) && priceToPayForContractGood < 50000)) {
                log.info("Trade Good found with Lowest Price: {} for {}$", goodForDelivery.getSymbol(), tradeGoodWithLowestPrice.getPurchasePrice());
                marketSearchService.navigateToMarket(tradeGoodWithLowestPrice.getMarket(), shipSymbol);
                MarketData marketData = spacetraderClient.viewMarketData(tradeGoodWithLowestPrice.getMarket().getSymbol()).getData();
                updateTradeGoods(marketData);
                return true;
            }

        }
        return false;
    }


    public static TradeGood findLowestPurchasePrice(List<TradeGood> tradeGoods) {
        if (tradeGoods == null || tradeGoods.isEmpty()) {
            return null;
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


    private void updateTradeGoods(MarketData marketData) {
        if (!marketData.getTradeGoods().isEmpty()) {
            marketSearchService.updateTradeGoods(marketData);
        }
    }

    private int getOnFulfilledMoney(List<Contract> contracts) {
        Contract lastContract = contracts.get(contracts.size() - 1);
        return lastContract.getTerms().getPayment().getOnFulfilled();
    }

    private int getAgentCredits() {
        return spacetraderClient.seeAgent().getData().getCredit();
    }

}

