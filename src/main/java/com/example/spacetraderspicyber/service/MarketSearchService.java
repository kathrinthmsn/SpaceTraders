package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
import com.example.spacetraderspicyber.model.*;
import com.example.spacetraderspicyber.model.Market.MarketData;
import com.example.spacetraderspicyber.model.Market.MarketData.TradeItem;
import com.example.spacetraderspicyber.model.Ship.ShipData;
import com.example.spacetraderspicyber.model.Ship.ShipData.Nav;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.lang.Thread.sleep;

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

    public void goToMarket(String shipSymbol, Good good) throws InterruptedException {
        Nav shipInfo = spacetraderClient.seeShipDetails(shipSymbol).getData().getNav();
        String currentLocation = shipInfo.getWaypointSymbol();
        log.info("{} currently located at: {}", shipSymbol, currentLocation);

        List<Market> markets = marketService.findMarketsByGoodsToSell(good.getSymbol());
        if (sellableInCurrentWaypoint(markets, currentLocation)) {
            return;
        } else {
            this.navigateToMarket(this.findClosestMarket(markets, shipSymbol), shipSymbol);
        }
    }

    public static boolean sellableInCurrentWaypoint(List<Market> markets, String targetSymbol) {
        for (Market market : markets) {
            if (market.getSymbol().equals(targetSymbol)) {
                return true; // Found a market
            }
        }
        return false; // Symbol not found in any market
    }


    public boolean hasMarketplace(Waypoint waypoint) {
        List<Trait> traits = waypoint.getTraits();

        for (Trait trait : traits) {
            String symbol = trait.getSymbol();

            if ("MARKETPLACE".equals(symbol)) {
                return true;
            }
        }

        return false;
    }

    public void marketResearch(String shipSymbol) throws InterruptedException {
        while (true) {
            List<Market> allMarkets = marketService.findAll();
            Market closestMarket = findClosestMarket(allMarkets, shipSymbol);
            allMarkets.remove(closestMarket);
            navigateWithoutFueling(closestMarket, shipSymbol);
            MarketData marketData = spacetraderClient.viewMarketData(closestMarket.getSymbol()).getData();
            this.updateTradeGoods(marketData);
        }
    }


    public void updateTradeGoods(MarketData marketData) {

        Market market = marketService.findByName(marketData.getSymbol());

        if (market != null) {
            if (!marketData.getTradeGoods().isEmpty()) {
                Gson gson = new Gson();
                marketService.deleteMarket(market);
                market.getTradeGoods().clear();

                Type tradeGoodListType = new TypeToken<List<TradeGood>>() {
                }.getType();
                List<TradeGood> tradeGoods = gson.fromJson(marketData.getTradeGoods().toString(), tradeGoodListType);
                tradeGoods.forEach(tradeGood -> tradeGood.setMarket(market));
                market.setTradeGoods(tradeGoods);

                List<String> goodsToSellSymbols = extractSymbolList(marketData.getImports(), marketData.getExports(), marketData.getExchange());
                market.setGoodsToSell(goodsToSellSymbols);

                marketService.saveMarket(market);

            } else {
                market.setTradeGoods(Collections.emptyList());
            }

            log.info("Trade Goods updated for Market: {}", market.getSymbol());
        } else {
            log.error("Market not found for symbol: {}", marketData.getSymbol());
        }
    }

    @Transactional
    public void saveMarketDataToDb() {
        for (int page = 1; page < 10; page++) {
            List<Waypoint> waypoints = spacetraderClient.getWaypoints(page).getData();
            for (Waypoint waypoint : waypoints) {
                if (hasMarketplace(waypoint)) {
                    String symbol = waypoint.getSymbol();

                    if (marketExistsInDb(symbol)) continue;
                    MarketData marketData = spacetraderClient.viewMarketData(symbol).getData();
                    Market market = new Market();
                    market.setSymbol(symbol);

                    Gson gson = new Gson();
                    if (!marketData.getTradeGoods().isEmpty()) {
                        Type tradeGoodListType = new TypeToken<List<TradeGood>>() {
                        }.getType();
                        List<TradeGood> tradeGoods = gson.fromJson(marketData.getTradeGoods().toString(), tradeGoodListType);
                        tradeGoods.forEach(tradeGood -> tradeGood.setMarket(market));
                        market.setTradeGoods(tradeGoods);
                    } else {
                        market.setTradeGoods(Collections.emptyList());
                    }

                    List<String> goodsToSellSymbols = extractSymbolList(marketData.getImports(), marketData.getExports(), marketData.getExchange());
                    market.setGoodsToSell(goodsToSellSymbols);

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

    private static List<String> extractSymbolList(List<TradeItem> imports, List<TradeItem> exports, List<TradeItem> exchange) {
        List<String> symbols = new ArrayList<>();
        for (TradeItem anImport : imports) {
            symbols.add(anImport.getSymbol());
        }
        for (TradeItem export : exports) {
            symbols.add(export.getSymbol());
        }
        for (TradeItem tradeItem : exchange) {
            symbols.add(tradeItem.getSymbol());
        }
        return symbols;
    }


    public void navigateToMarket(Market market, String shipSymbol) throws InterruptedException {
        Nav shipInfo = spacetraderClient.seeShipDetails(shipSymbol).getData().getNav();
        String currentLocation = shipInfo.getWaypointSymbol();
        if (!currentLocation.equals(market.getSymbol())) {
            spacetraderClient.orbitShip(shipSymbol);
            fuelingService.fuelShip(shipSymbol);
            findFuelStop(shipSymbol, market.getSymbol());

            goToWaypoint(market.getSymbol(), shipSymbol);
        }
    }

    public void navigateWithoutFueling(Market market, String shipSymbol) throws InterruptedException {
        String currentLocation = spacetraderClient.seeShipDetails(shipSymbol).getData().getCurrentLocation();
        if (!currentLocation.equals(market.getSymbol())) {
            goToWaypoint(market.getSymbol(), shipSymbol);
        }
    }

    public void navigateToWaypoint(Waypoint waypoint, String shipSymbol) throws InterruptedException {
        String currentLocation = spacetraderClient.seeShipDetails(shipSymbol).getData().getCurrentLocation();
        if (!currentLocation.equals(waypoint.getSymbol())) {
            spacetraderClient.orbitShip(shipSymbol);
            fuelingService.fuelShip(shipSymbol);
            this.findFuelStop(shipSymbol, waypoint.getSymbol());

            goToWaypoint(waypoint.getSymbol(), shipSymbol);
        }
    }


    private void goToWaypoint(String waypoint, String shipSymbol) throws InterruptedException {
        ShipNavigation navigation = spacetraderClient.navigateToWaypoint(shipSymbol, WaypointSymbol.builder().waypointSymbol(waypoint).build());

        long durationInSeconds = navigation.calculateRouteTime();
        log.info("{} navigates to: {}", shipSymbol, waypoint);
        log.info("{} sleepy time for: {}s", shipSymbol, durationInSeconds);
        sleep(durationInSeconds * 1000);
    }

    public void findFuelStop(String shipSymbol, String destinationSymbol) throws InterruptedException {
        ShipData shipInfo = spacetraderClient.seeShipDetails(shipSymbol).getData();

        Waypoint waypointDestination = spacetraderClient.getWaypoint(destinationSymbol);
        int waypointMarketX = waypointDestination.getX();
        int waypointMarketY = waypointDestination.getY();

        int fuelCurrent = shipInfo.getFuel().getCurrent();

        double distanceToMarket = shipInfo.calculateDistanceToCurrentLocation(waypointMarketX, waypointMarketY);

        if (distanceToMarket > fuelCurrent) {


            double distanceFromWaypointToDestination = Double.MAX_VALUE;

            while (distanceFromWaypointToDestination > fuelCurrent) {

                Waypoint nextStop = null;

                while (nextStop == null) {
                    double distanceToNextMarket = Double.MAX_VALUE;
                    double smallestDistanceFromWaypointToDestination = Double.MAX_VALUE;
                    Market closestMarket = null;
                    List<Waypoint> allWaypoints = waypointService.findAll();
                    String currentLocation = shipInfo.getNav().getWaypointSymbol();
                    Waypoint currentLocationWaypoint = waypointService.findByName(currentLocation);
                    allWaypoints.removeAll(List.of(currentLocationWaypoint, waypointDestination));

                    for (Waypoint waypointFromDb : allWaypoints) {

                        double distanceToWaypoint = shipInfo.calculateDistanceToCurrentLocation(waypointFromDb.getX(), waypointFromDb.getY());

                        if (distanceToWaypoint < distanceToMarket) {

                            distanceFromWaypointToDestination = this.calculateDistanceToWaypoint(waypointFromDb, waypointDestination);

                            if (distanceFromWaypointToDestination < distanceToMarket) {

                                Optional<Market> closestMarketOpt = Optional.ofNullable(marketService.findByName(waypointFromDb.getSymbol()));
                                if (closestMarketOpt.isPresent() && distanceToWaypoint < distanceToNextMarket) {
                                    distanceToNextMarket = distanceToWaypoint;
                                    closestMarket = closestMarketOpt.get();
                                }
                                if (distanceFromWaypointToDestination < smallestDistanceFromWaypointToDestination) {
                                    if (distanceToWaypoint <= fuelCurrent) {
                                        smallestDistanceFromWaypointToDestination = distanceFromWaypointToDestination;
                                        nextStop = waypointFromDb;
                                    }
                                }
                            }
                        }
                    }


                    if (nextStop == null) {
                        spacetraderClient.changeFlightMode(shipSymbol, FlightMode.builder().flightMode("DRIFT").build());
                        this.navigateWithoutFueling(closestMarket, shipSymbol);
                        fuelingService.fuelShip(shipSymbol);
                        spacetraderClient.changeFlightMode(shipSymbol, FlightMode.builder().flightMode("CRUISE").build());
                    } else {
                        this.navigateToWaypoint(nextStop, shipSymbol);
                        fuelingService.fuelShip(shipSymbol);
                    }
                    shipInfo = spacetraderClient.seeShipDetails(shipSymbol).getData();
                    fuelCurrent = shipInfo.getFuel().getCurrent();
                    if (fuelCurrent > smallestDistanceFromWaypointToDestination) {
                        return;
                    }
                    distanceToMarket = shipInfo.calculateDistanceToCurrentLocation(waypointDestination.getX(), waypointDestination.getY());
                }
            }
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


    public double calculateDistanceToWaypoint(Waypoint waypoint, Waypoint waypointDestination) {
        int waypointMarketXDestination = waypointDestination.getX();
        int waypointMarketYDestination = waypointDestination.getY();

        Waypoint waypointData = spacetraderClient.getWaypoint(waypoint.getSymbol());
        int waypointMarketX = waypointData.getX();
        int waypointMarketY = waypointData.getY();

        return this.calculateDistance(waypointMarketXDestination, waypointMarketYDestination, waypointMarketX, waypointMarketY);
    }

    private double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

}
