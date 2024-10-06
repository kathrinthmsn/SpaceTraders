package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
import com.example.spacetraderspicyber.model.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        JSONObject shipInfo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data").getJSONObject("nav");
        String currentLocation = shipInfo.getString("waypointSymbol");
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


    public boolean hasMarketplace(JSONObject waypoint) {
        JSONArray traits = waypoint.getJSONArray("traits");

        for (int i = 0; i < traits.length(); i++) {
            JSONObject trait = traits.getJSONObject(i);
            String symbol = trait.getString("symbol");

            if ("MARKETPLACE".equals(symbol)) {
                return true;
            }
        }

        return false;
    }


    private static LocalDateTime getLocalDateTimeFromJson(String json, String key) {
        String timestampString = new JSONObject(json).getJSONObject("data").getJSONObject("nav").getJSONObject("route").getString(key);
        return LocalDateTime.parse(timestampString, DateTimeFormatter.ISO_DATE_TIME);
    }

    private static long calculateDurationInSeconds(LocalDateTime departureTime, LocalDateTime arrivalTime) {
        Duration duration = Duration.between(departureTime, arrivalTime);
        return duration.getSeconds();
    }

    public void marketResearch(String shipSymbol) throws InterruptedException {
        while (true) {
            List<Market> allMarkets = marketService.findAll();
            for (int i = 0; i < allMarkets.size(); i++) {
                Market closestMarket = findClosestMarket(allMarkets, shipSymbol);
                allMarkets.remove(closestMarket);
                navigateWithoutFueling(closestMarket, shipSymbol);
                JSONObject marketData = new JSONObject(spacetraderClient.viewMarketData(closestMarket.getSymbol()));
                this.updateTradeGoods(marketData);
            }
        }
    }


    public void updateTradeGoods(JSONObject marketData) {

        Market market = marketService.findByName(marketData.getJSONObject("data").getString("symbol"));

        if (market != null) {
            if (marketData.getJSONObject("data").has("tradeGoods")) {
                Gson gson = new Gson();
                marketService.deleteMarket(market);
                market.getTradeGoods().clear();

                Type tradeGoodListType = new TypeToken<List<TradeGood>>() {
                }.getType();
                List<TradeGood> tradeGoods = gson.fromJson(marketData.getJSONObject("data").getJSONArray("tradeGoods").toString(), tradeGoodListType);
                tradeGoods.forEach(tradeGood -> tradeGood.setMarket(market));
                market.setTradeGoods(tradeGoods);

                List<String> goodsToSellSymbols = extractSymbolList(marketData.getJSONObject("data").getJSONArray("imports"), marketData.getJSONObject("data").getJSONArray("exports"), marketData.getJSONObject("data").getJSONArray("exchange"));
                market.setGoodsToSell(goodsToSellSymbols);

                marketService.saveMarket(market);

            } else {
                market.setTradeGoods(Collections.emptyList());
            }

            log.info("Trade Goods updated for Market: {}", market.getSymbol());
        } else {
            log.error("Market not found for symbol: {}", marketData.getJSONObject("data").getString("symbol"));
        }
    }

    @Transactional
    public void mapMarketData() {
        for (int page = 1; page < 10; page++) {
            JSONArray waypoints = new JSONObject(spacetraderClient.getWaypoints(page)).getJSONArray("data");
            for (int i = 0; i < waypoints.length(); i++) {
                JSONObject waypoint = waypoints.getJSONObject(i);
                if (hasMarketplace(waypoint)) {
                    String symbol = waypoint.getString("symbol");

                    if (marketExistsInDb(symbol)) continue;
                    JSONObject marketData = new JSONObject(spacetraderClient.viewMarketData(symbol));
                    Market market = new Market();
                    market.setSymbol(symbol);

                    Gson gson = new Gson();
                    if (marketData.getJSONObject("data").has("tradeGoods")) {
                        Type tradeGoodListType = new TypeToken<List<TradeGood>>() {
                        }.getType();
                        List<TradeGood> tradeGoods = gson.fromJson(marketData.getJSONObject("data").getJSONArray("tradeGoods").toString(), tradeGoodListType);
                        tradeGoods.forEach(tradeGood -> tradeGood.setMarket(market));
                        market.setTradeGoods(tradeGoods);
                    } else {
                        market.setTradeGoods(Collections.emptyList());
                    }

                    List<String> goodsToSellSymbols = extractSymbolList(marketData.getJSONObject("data").getJSONArray("imports"), marketData.getJSONObject("data").getJSONArray("exports"), marketData.getJSONObject("data").getJSONArray("exchange"));
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

    private static List<String> extractSymbolList(JSONArray imports, JSONArray exports, JSONArray exchange) {
        List<String> symbols = new ArrayList<>();
        for (int i = 0; i < imports.length(); i++) {
            symbols.add(imports.getJSONObject(i).getString("symbol"));
        }
        for (int i = 0; i < exports.length(); i++) {
            symbols.add(exports.getJSONObject(i).getString("symbol"));
        }
        for (int i = 0; i < exchange.length(); i++) {
            symbols.add(exchange.getJSONObject(i).getString("symbol"));
        }
        return symbols;
    }


    public void navigateToMarket(Market market, String shipSymbol) throws InterruptedException {
        JSONObject shipInfo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data").getJSONObject("nav");
        String currentLocation = shipInfo.getString("waypointSymbol");
        if (!currentLocation.equals(market.getSymbol())) {
            spacetraderClient.orbitShip(shipSymbol);
            fuelingService.fuelShip(shipSymbol);
            findFuelStop(shipSymbol, market.getSymbol());

            goToWaypoint(market.getSymbol(), shipSymbol);
        }
    }

    public void navigateWithoutFueling(Market market, String shipSymbol) throws InterruptedException {
        JSONObject shipInfo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data").getJSONObject("nav");
        String currentLocation = shipInfo.getString("waypointSymbol");
        if (!currentLocation.equals(market.getSymbol())) {
            goToWaypoint(market.getSymbol(), shipSymbol);
        }
    }

    public void navigateToWaypoint(Waypoint waypoint, String shipSymbol) throws InterruptedException {
        JSONObject shipInfo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data").getJSONObject("nav");
        String currentLocation = shipInfo.getString("waypointSymbol");
        if (!currentLocation.equals(waypoint.getSymbol())) {
            spacetraderClient.orbitShip(shipSymbol);
            fuelingService.fuelShip(shipSymbol);
            this.findFuelStop(shipSymbol, waypoint.getSymbol());

            goToWaypoint(waypoint.getSymbol(), shipSymbol);
        }
    }


    private void goToWaypoint(String waypoint, String shipSymbol) throws InterruptedException {
        String json = spacetraderClient.navigateToWaypoint(shipSymbol, WaypointSymbol.builder().waypointSymbol(waypoint).build());
        LocalDateTime departureTime = getLocalDateTimeFromJson(json, "departureTime");
        LocalDateTime arrivalTime = getLocalDateTimeFromJson(json, "arrival");

        long durationInSeconds = calculateDurationInSeconds(departureTime, arrivalTime);
        log.info("{} navigates to: {}", shipSymbol, waypoint);
        log.info("{} sleepy time for: {}s", shipSymbol, durationInSeconds);
        sleep(durationInSeconds * 1000);
    }

    public void findFuelStop(String shipSymbol, String destinationSymbol) throws InterruptedException {
        JSONObject shipInfo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data");
        int currentLocationXCoordinate = shipInfo.getJSONObject("nav").getJSONObject("route").getJSONObject("destination").getInt("x");
        int currentLocationYCoordinate = shipInfo.getJSONObject("nav").getJSONObject("route").getJSONObject("destination").getInt("y");

        JSONObject waypointDestinationJson = new JSONObject(spacetraderClient.getWaypoint(destinationSymbol)).getJSONObject("data");
        int waypointMarketX = waypointDestinationJson.getInt("x");
        int waypointMarketY = waypointDestinationJson.getInt("y");

        int fuelCurrent = shipInfo.getJSONObject("fuel").getInt("current");

        double distanceToMarket = this.calculateDistance(currentLocationXCoordinate, currentLocationYCoordinate, waypointMarketX, waypointMarketY);

        if (distanceToMarket > fuelCurrent) {


            double distanceFromWaypointToDestination = Double.MAX_VALUE;

            while (distanceFromWaypointToDestination > fuelCurrent) {

                Waypoint nextStop = null;

                while (nextStop == null) {
                    double distanceToNextMarket = Double.MAX_VALUE;
                    double smallestDistanceFromWaypointToDestination = Double.MAX_VALUE;
                    Market closestMarket = null;
                    List<Waypoint> allWaypoints = waypointService.findAll();
                    String currentLocation = shipInfo.getJSONObject("nav").getString("waypointSymbol");
                    Waypoint currentLocationWaypoint = waypointService.findByName(currentLocation);
                    Waypoint waypointDestination = waypointService.findByName(destinationSymbol);
                    allWaypoints.removeAll(List.of(currentLocationWaypoint, waypointDestination));

                    for (Waypoint waypointFromDb : allWaypoints) {

                        double distanceToWaypoint = this.calculateDistanceToWaypoint(waypointFromDb, currentLocationXCoordinate, currentLocationYCoordinate);

                        if (distanceToWaypoint < distanceToMarket) {

                            distanceFromWaypointToDestination = this.calculateDistanceToWaypoint(waypointFromDb, waypointDestinationJson);

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
                    shipInfo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data");
                    fuelCurrent = shipInfo.getJSONObject("fuel").getInt("current");
                    if (fuelCurrent > smallestDistanceFromWaypointToDestination) {
                        return;
                    }
                    currentLocationXCoordinate = shipInfo.getJSONObject("nav").getJSONObject("route").getJSONObject("destination").getInt("x");
                    currentLocationYCoordinate = shipInfo.getJSONObject("nav").getJSONObject("route").getJSONObject("destination").getInt("y");
                    distanceToMarket = this.calculateDistanceToWaypoint(waypointDestination, currentLocationXCoordinate, currentLocationYCoordinate);
                }
            }
        }
    }


    public Market findClosestMarket(List<Market> markets, String shipSymbol) throws InterruptedException {
        JSONObject shipInfo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data");
        int currentLocationXCoordinate = shipInfo.getJSONObject("nav").getJSONObject("route").getJSONObject("destination").getInt("x");
        int currentLocationYCoordinate = shipInfo.getJSONObject("nav").getJSONObject("route").getJSONObject("destination").getInt("y");
        Market closestMarket = null;
        double distance = Double.MAX_VALUE;
        for (Market market : markets) {
            sleep(1500);
            JSONObject waypointMarket = new JSONObject(spacetraderClient.getWaypoint(market.getSymbol())).getJSONObject("data");
            int waypointMarketX = waypointMarket.getInt("x");
            int waypointMarketY = waypointMarket.getInt("y");

            double distanceToMarket = this.calculateDistance(currentLocationXCoordinate, currentLocationYCoordinate, waypointMarketX, waypointMarketY);
            if (distanceToMarket < distance) {
                closestMarket = market;
                distance = distanceToMarket;
            }
        }
        return closestMarket;
    }


    public double calculateDistanceToWaypoint(Waypoint waypoint, JSONObject waypointDestinationJson) throws InterruptedException {
        int waypointMarketXDestination = waypointDestinationJson.getInt("x");
        int waypointMarketYDestination = waypointDestinationJson.getInt("y");

        JSONObject waypointJson = new JSONObject(spacetraderClient.getWaypoint(waypoint.getSymbol())).getJSONObject("data");
        int waypointMarketX = waypointJson.getInt("x");
        int waypointMarketY = waypointJson.getInt("y");

        return this.calculateDistance(waypointMarketXDestination, waypointMarketYDestination, waypointMarketX, waypointMarketY);
    }

    public double calculateDistanceToWaypoint(Waypoint waypointDestination, Integer currentX, Integer currentY) throws InterruptedException {
        sleep(500);
        JSONObject waypointDestinationJson = new JSONObject(spacetraderClient.getWaypoint(waypointDestination.getSymbol())).getJSONObject("data");
        int waypointMarketXDestination = waypointDestinationJson.getInt("x");
        int waypointMarketYDestination = waypointDestinationJson.getInt("y");

        return this.calculateDistance(waypointMarketXDestination, waypointMarketYDestination, currentX, currentY);
    }


    private double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

}
