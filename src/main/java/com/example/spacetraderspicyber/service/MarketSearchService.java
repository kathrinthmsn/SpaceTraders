package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
import com.example.spacetraderspicyber.model.Good;
import com.example.spacetraderspicyber.model.Market;
import com.example.spacetraderspicyber.model.Market.MarketData;
import com.example.spacetraderspicyber.model.Ship.ShipData;
import com.example.spacetraderspicyber.model.Ship.ShipData.Nav;
import com.example.spacetraderspicyber.model.TradeGood;
import com.example.spacetraderspicyber.model.Waypoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
public class MarketSearchService {

    @Autowired
    private SpacetraderClient spacetraderClient;
    @Autowired
    private FuelingService fuelingService;
    @Autowired
    private MarketService marketService;
    @Autowired
    private WaypointService waypointService;


    @Transactional
    public void saveMarketDataToDb() {
        for (int page = 1; page < 10; page++) {
            List<Waypoint> waypoints = spacetraderClient.getWaypoints(page).getData();
            for (Waypoint waypoint : waypoints) {
                if (waypointService.hasMarketplace(waypoint)) {
                    String symbol = waypoint.getSymbol();
                    if (marketExistsInDb(symbol)) continue;
                    Market market = spacetraderClient.viewMarketData(symbol);
                    market.setData(market.getData());
                    for (TradeGood tradeGood : market.getData().getTradeGoods()) {
                        tradeGood.setMarket(market);
                    }
                    marketService.saveMarket(market);
                    log.info("Market added: {}", market.getSymbol());
                }
            }
        }
    }

    private boolean marketExistsInDb(String symbol) {
        if (marketService.findByName(symbol) != null) {
            log.info("Market with symbol {} already exists. Skipping.", symbol);
            return true;
        }
        return false;
    }

    public void goToMarketForGood(String shipSymbol, Good good) throws InterruptedException {
        Nav shipInfo = spacetraderClient.seeShipDetails(shipSymbol).getData().getNav();
        String currentLocation = shipInfo.getWaypointSymbol();
        log.info("{} currently located at: {}", shipSymbol, currentLocation);

        List<Market> marketsWhereGoodIsSold = marketService.findMarketsByGoodsToSell(good.getSymbol());
        Market market = marketService.findByName(currentLocation);
        if (!market.getGoodsToSell().contains(good.getSymbol())) {
            this.navigateToMarket(this.findClosestMarket(marketsWhereGoodIsSold, shipSymbol), shipSymbol);
        }
    }

    public void marketResearch(String shipSymbol) throws InterruptedException {
        List<Market> allMarkets = marketService.findAll();
        while (true) {
            Market closestMarket = findClosestMarket(allMarkets, shipSymbol);
            allMarkets.remove(closestMarket);
            waypointService.navigateWithoutFueling(closestMarket.getSymbol(), shipSymbol);
            MarketData marketData = spacetraderClient.viewMarketData(closestMarket.getSymbol()).getData();
            this.updateTradeGoods(marketData);
        }
    }

    public Market findClosestMarket(List<Market> markets, String shipSymbol) {
        ShipData shipInfo = spacetraderClient.seeShipDetails(shipSymbol).getData();
        Market closestMarket = null;
        double distance = Double.MAX_VALUE;
        for (Market market : markets) {
            Waypoint waypointMarket = spacetraderClient.getWaypoint(market.getSymbol());
            double distanceToMarket = shipInfo.calculateDistanceToCurrentLocation(waypointMarket.getX(), waypointMarket.getY());
            if (distanceToMarket < distance) {
                closestMarket = market;
                distance = distanceToMarket;
            }
        }
        return closestMarket;
    }


    public void updateTradeGoods(MarketData marketData) {
        Market market = marketService.findByName(marketData.getSymbol());
        if (market != null) {
            market.setData(market.getData());
            marketService.saveMarket(market);
            log.info("Trade Goods updated for Market: {}", market.getSymbol());
        } else {
            log.error("Market not found for symbol: {}", marketData.getSymbol());
        }
    }

    public void navigateToMarket(Market market, String shipSymbol) throws InterruptedException {
        ShipData shipInfo = spacetraderClient.seeShipDetails(shipSymbol).getData();
        String currentLocation = shipInfo.getNav().getWaypointSymbol();
        if (!currentLocation.equals(market.getSymbol())) {
            spacetraderClient.orbitShip(shipSymbol);
            fuelingService.fuelShip(shipSymbol);
            Waypoint marketDestination = waypointService.findByName(market.getSymbol());
            fuelingService.findFuelStopOnTheWay(shipInfo, marketDestination);
            waypointService.goToWaypoint(market.getSymbol(), shipSymbol);
        }
    }

}
