package com.example.spacetraderspicyber.client;

import com.example.spacetraderspicyber.events.EventPublisher;
import com.example.spacetraderspicyber.model.*;
import com.example.spacetraderspicyber.service.WaypointService;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.json.JSONObject;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.lang.Thread.sleep;

@Component
public class Mining {

    @Autowired
    private SpacetraderClient spacetraderClient;
    @Autowired
    private Contracts contracts;
    @Autowired
    private WaypointService waypointService;
    @Autowired
    private MarketSearch marketSearch;
    @Autowired
    private EventPublisher eventPublisher;

    //TODO: Find closest Asteroid
    public void flyToAsteroid(String shipSymbol) throws InterruptedException {
        JSONObject contractInfo = contracts.getContractInfo();
        Good goodForDelivery = contracts.getContractDeliveryGood(contractInfo);
        if(!Good.isMinable(goodForDelivery)){
            return;
        }
        if("GOLD_ORE".equals(goodForDelivery.getSymbol()) || "SILVER_ORE".equals(goodForDelivery.getSymbol()) || "PLATINUM_ORE".equals(goodForDelivery.getSymbol())) {
            List<Waypoint> waypoints = waypointService.findByType("PRECIOUS_METAL_DEPOSITS");
            Waypoint waypoint = this.findClosestWaypoint(waypoints, shipSymbol);
            this.navigateToAsteroidLocation(shipSymbol, waypoint.getSymbol());
            eventPublisher.publishEvent(waypoint.getSymbol());
            return;
        }

        JSONObject shipInfo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data").getJSONObject("nav");
        String currentLocation = shipInfo.getString("waypointSymbol");

        JSONObject waypointCurrent = new JSONObject(spacetraderClient.getWaypoint(currentLocation)).getJSONObject("data");
        String type = waypointCurrent.getString("type");
        JSONObject cargo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data").getJSONObject("cargo");
        int capacity = cargo.getInt("capacity");
        int load = cargo.getInt("units");


        if (("ASTEROID".equals(type) || "ASTEROID_FIELD".equals(type) || "ENGINEERED_ASTEROID".equals(type)) || load >= capacity) {
                eventPublisher.publishEvent(currentLocation);
                return;
            }

        JSONObject asteroid = new JSONObject(spacetraderClient.getAsteroidFieldLocation());
        String waypointAsteroid = asteroid.getJSONArray("data").getJSONObject(0).getString("symbol");
        eventPublisher.publishEvent(waypointAsteroid);

        if(!waypointAsteroid.equals(currentLocation)) {
            spacetraderClient.orbitShip(shipSymbol);
            this.navigateToAsteroidLocation(shipSymbol, waypointAsteroid);
        }
    }


    public void navigateToAsteroidLocation(String shipSymbol, String waypointAsteroid) throws InterruptedException {
        JSONObject shipInfo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data").getJSONObject("nav");
        String currentLocation = shipInfo.getString("waypointSymbol");
        if(!currentLocation.equals(waypointAsteroid)) {
            Waypoint waypoint = waypointService.findByName(waypointAsteroid);
            marketSearch.navigateToWaypoint(waypoint, shipSymbol);
        }
    }

    public Waypoint findClosestWaypoint(List<Waypoint> waypoints, String shipSymbol) throws InterruptedException {
        JSONObject shipInfo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data");
        int currentLocationXCoordinate = shipInfo.getJSONObject("nav").getJSONObject("route").getJSONObject("destination").getInt("x");
        int currentLocationYCoordinate = shipInfo.getJSONObject("nav").getJSONObject("route").getJSONObject("destination").getInt("y");
        Waypoint closestWaypoint = null;
        double distance = Double.MAX_VALUE;
        for(Waypoint waypoint: waypoints) {
            JSONObject waypointJson = new JSONObject(spacetraderClient.getWaypoint(waypoint.getSymbol())).getJSONObject("data");
            int waypointMarketX = waypointJson.getInt("x");
            int waypointMarketY = waypointJson.getInt("y");

            double distanceToWaypoint = this.calculateDistance(currentLocationXCoordinate, currentLocationYCoordinate, waypointMarketX, waypointMarketY);
            if(distanceToWaypoint < distance) {
                closestWaypoint = waypoint;
                distance = distanceToWaypoint;
            }
        }
        Integer fuelCapacity = shipInfo.getJSONObject("fuel").getInt("capacity");
        Integer fuelCurrent = shipInfo.getJSONObject("fuel").getInt("current");
        if(distance > fuelCurrent && !fuelCurrent.equals(fuelCapacity)) {
            spacetraderClient.fuelShip(shipSymbol, Fuel.builder().units(fuelCapacity - fuelCurrent).fromCargo(true).build());
        }
        return closestWaypoint;
    }

    private double calculateDistance(double x1, double y1, double x2, double y2) {
        // Implement your distance calculation logic here (e.g., Euclidean distance)
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
}
