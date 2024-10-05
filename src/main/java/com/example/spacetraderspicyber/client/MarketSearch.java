package com.example.spacetraderspicyber.client;

import com.example.spacetraderspicyber.model.*;
import com.example.spacetraderspicyber.service.MarketService;
import com.example.spacetraderspicyber.service.WaypointService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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

@Component
public class MarketSearch {

    @Autowired
    private SpacetraderClient spacetraderClient;
    @Autowired
    private Fueling fueling;
    @Autowired
    private MarketService marketService;
    @Autowired
    private WaypointService waypointService;

    public void goToMarket(String shipSymbol, Good good) throws InterruptedException {
        JSONObject shipInfo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data").getJSONObject("nav");
        String currentLocation = shipInfo.getString("waypointSymbol");
        System.out.println(shipSymbol + " currently located at: " + currentLocation);

        List<Market> markets = marketService.findMarketsByGoodsToSell(good.getSymbol());
        if(sellableInCurrentWaypoint(markets, currentLocation)){
            return;
        }
        else  {
            this.navigateToMarket(this.findClosestMarket(markets, shipSymbol) , shipSymbol);
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
        while(true) {
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

                Type tradeGoodListType = new TypeToken<List<TradeGood>>() {}.getType();
                List<TradeGood> tradeGoods = gson.fromJson(marketData.getJSONObject("data").getJSONArray("tradeGoods").toString(), tradeGoodListType);
                tradeGoods.forEach(tradeGood -> tradeGood.setMarket(market));
                market.setTradeGoods(tradeGoods);

                List<String> goodsToSellSymbols = extractSymbolList(marketData.getJSONObject("data").getJSONArray("imports"),marketData.getJSONObject("data").getJSONArray("exports"), marketData.getJSONObject("data").getJSONArray("exchange"));
                market.setGoodsToSell(goodsToSellSymbols);

                marketService.saveMarket(market);

            } else {
                market.setTradeGoods(Collections.emptyList());
            }

             System.out.println("Trade Goods updated for Market: " + market.getSymbol());
        } else {
            System.out.println("Market not found for symbol: " +  marketData.getJSONObject("data").getString("symbol"));
        }
    }

    @Transactional
    public void mapMarketData() throws InterruptedException {
        for (int page = 1; page < 10; page++) {
                JSONArray waypoints = new JSONObject(spacetraderClient.getWaypoints(page)).getJSONArray("data");
                for (int i = 0; i < waypoints.length(); i++) {
                    JSONObject waypoint = waypoints.getJSONObject(i);
                    if (hasMarketplace(waypoint)) {

                        String symbol = waypoint.getString("symbol");

                        // Check if the market with the symbol already exists
                        if (marketService.findByName(symbol) != null) {
                            System.out.println("Market with symbol " + symbol + " already exists. Skipping.");
                            continue;
                        }
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

                        List<String> goodsToSellSymbols = extractSymbolList(marketData.getJSONObject("data").getJSONArray("imports"),marketData.getJSONObject("data").getJSONArray("exports"), marketData.getJSONObject("data").getJSONArray("exchange"));
                        market.setGoodsToSell(goodsToSellSymbols);

                        marketService.saveMarket(market);
                        System.out.println("Market added: " + market.getSymbol());
                    }
                }
            }
        return;
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
        if(!currentLocation.equals(market.getSymbol())) {
            spacetraderClient.orbitShip(shipSymbol);
            fueling.fuelShip(shipSymbol);
            findFuelStop(shipSymbol, market.getSymbol());

            String json = spacetraderClient.navigateToWaypoint(shipSymbol, WaypointSymbol.builder().waypointSymbol(market.getSymbol()).build());
            LocalDateTime departureTime = getLocalDateTimeFromJson(json, "departureTime");
            LocalDateTime arrivalTime = getLocalDateTimeFromJson(json, "arrival");

            long durationInSeconds = calculateDurationInSeconds(departureTime, arrivalTime);


            System.out.println(shipSymbol +  " navigates to: " + market.getSymbol());
            System.out.println(shipSymbol + " sleepy time for: " + durationInSeconds + "s");
            sleep(durationInSeconds * 1000);
        }
    }

    public void navigateWithoutFueling(Market market, String shipSymbol) throws InterruptedException {
        JSONObject shipInfo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data").getJSONObject("nav");
        String currentLocation = shipInfo.getString("waypointSymbol");
        if(!currentLocation.equals(market.getSymbol())) {
            String json = spacetraderClient.navigateToWaypoint(shipSymbol, WaypointSymbol.builder().waypointSymbol(market.getSymbol()).build());
            LocalDateTime departureTime = getLocalDateTimeFromJson(json, "departureTime");
            LocalDateTime arrivalTime = getLocalDateTimeFromJson(json, "arrival");

            long durationInSeconds = calculateDurationInSeconds(departureTime, arrivalTime);

            System.out.println(shipSymbol +  " navigates to: " + market.getSymbol());
            System.out.println(shipSymbol + " sleepy time for: " + durationInSeconds + "s");
            sleep(durationInSeconds * 1000);
        }
    }

    public void navigateToWaypoint(Waypoint waypoint, String shipSymbol) throws InterruptedException {
        JSONObject shipInfo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data").getJSONObject("nav");
        String currentLocation = shipInfo.getString("waypointSymbol");
        if(!currentLocation.equals(waypoint.getSymbol())) {
            spacetraderClient.orbitShip(shipSymbol);
            fueling.fuelShip(shipSymbol);
            this.findFuelStop(shipSymbol, waypoint.getSymbol());

            String json = spacetraderClient.navigateToWaypoint(shipSymbol, WaypointSymbol.builder().waypointSymbol(waypoint.getSymbol()).build());
            LocalDateTime departureTime = getLocalDateTimeFromJson(json, "departureTime");
            LocalDateTime arrivalTime = getLocalDateTimeFromJson(json, "arrival");

            long durationInSeconds = calculateDurationInSeconds(departureTime, arrivalTime);

            System.out.println(shipSymbol +  " navigates to: " + waypoint.getSymbol());
            System.out.println(shipSymbol + " sleepy time for: " + durationInSeconds + "s");
            sleep(durationInSeconds * 1000);
        }
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
                        fueling.fuelShip(shipSymbol);
                        spacetraderClient.changeFlightMode(shipSymbol, FlightMode.builder().flightMode("CRUISE").build());
                    } else {
                        this.navigateToWaypoint(nextStop, shipSymbol);
                        fueling.fuelShip(shipSymbol);
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
        for(Market market: markets) {
            sleep(1500);
            JSONObject waypointMarket = new JSONObject(spacetraderClient.getWaypoint(market.getSymbol())).getJSONObject("data");
            int waypointMarketX = waypointMarket.getInt("x");
            int waypointMarketY = waypointMarket.getInt("y");

            double distanceToMarket = this.calculateDistance(currentLocationXCoordinate, currentLocationYCoordinate, waypointMarketX, waypointMarketY);
            if(distanceToMarket < distance) {
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
        // Implement your distance calculation logic here (e.g., Euclidean distance)
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

}
