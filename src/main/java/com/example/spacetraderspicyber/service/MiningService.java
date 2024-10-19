package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
import com.example.spacetraderspicyber.model.Good;
import com.example.spacetraderspicyber.model.Ship.ShipData;
import com.example.spacetraderspicyber.model.Waypoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MiningService {

    @Autowired
    private SpacetraderClient spacetraderClient;
    @Autowired
    private ContractsService contractsService;
    @Autowired
    private WaypointService waypointService;
    @Autowired
    private FuelingService fuelingService;

    private final ApplicationEventPublisher eventPublisher;

    public MiningService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }


    public void flyToAsteroid(String shipSymbol, Good good) throws InterruptedException {
        if ("GOLD_ORE".equals(good.getSymbol()) || "SILVER_ORE".equals(good.getSymbol()) || "PLATINUM_ORE".equals(good.getSymbol())) {
            goToClosestAsteroid(waypointService.findByType("PRECIOUS_METAL_DEPOSITS"), shipSymbol);
            return;
        }
        ShipData shipData = spacetraderClient.seeShipDetails(shipSymbol).getData();
        Waypoint waypointCurrent = waypointService.findByName(shipData.getCurrentLocation());
        String typeOfCurrentWaypoint = waypointCurrent.getType();
        if (("ASTEROID".equals(typeOfCurrentWaypoint) || "ASTEROID_FIELD".equals(typeOfCurrentWaypoint) || "ENGINEERED_ASTEROID".equals(typeOfCurrentWaypoint)) || shipData.cargoFull()) {
            eventPublisher.publishEvent(shipData.getCurrentLocation());
                return;
            }
        goToClosestAsteroid(waypointService.findByType("ENGINEERED_ASTEROID"), shipSymbol);
    }


    public void navigateToAsteroidLocation(String shipSymbol, String waypointAsteroid) throws InterruptedException {
        String currentLocation = spacetraderClient.seeShipDetails(shipSymbol).getData().getCurrentLocation();
        if(!currentLocation.equals(waypointAsteroid)) {
            spacetraderClient.orbitShip(shipSymbol);
            fuelingService.fuelShip(shipSymbol);
            waypointService.goToWaypoint(waypointAsteroid, shipSymbol);
        }
    }

    private void goToClosestAsteroid(List<Waypoint> waypoints, String shipSymbol) throws InterruptedException {
        Waypoint waypoint = waypointService.findClosestWaypoint(waypoints, shipSymbol);
        navigateToAsteroidLocation(shipSymbol, waypoint.getSymbol());
        eventPublisher.publishEvent(waypoint.getSymbol());
    }
}
