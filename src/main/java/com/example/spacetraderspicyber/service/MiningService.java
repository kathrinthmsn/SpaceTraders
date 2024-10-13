package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
import com.example.spacetraderspicyber.model.*;
import com.example.spacetraderspicyber.model.Contracts.Contract;
import com.example.spacetraderspicyber.model.Ship.ShipData;
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
    private MarketSearchService marketSearchService;
    private final ApplicationEventPublisher eventPublisher;

    public MiningService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    //TODO: Find closest Asteroid
    public void flyToAsteroid(String shipSymbol) throws InterruptedException {
        List<Contract> contracts = contractsService.getContractInfo();
        Good goodForDelivery = contractsService.getContractDeliveryGood(contracts);
        if (Good.isNotMinable(goodForDelivery)) {
            return;
        }
        if("GOLD_ORE".equals(goodForDelivery.getSymbol()) || "SILVER_ORE".equals(goodForDelivery.getSymbol()) || "PLATINUM_ORE".equals(goodForDelivery.getSymbol())) {
            List<Waypoint> waypoints = waypointService.findByType("PRECIOUS_METAL_DEPOSITS");
            Waypoint waypoint = this.findClosestWaypoint(waypoints, shipSymbol);
            this.navigateToAsteroidLocation(shipSymbol, waypoint.getSymbol());
            eventPublisher.publishEvent(waypoint.getSymbol());
            return;
        }

        String currentLocation = spacetraderClient.seeShipDetails(shipSymbol).getData().getCurrentLocation();

        Waypoint waypointCurrent = spacetraderClient.getWaypoint(currentLocation);
        String type = waypointCurrent.getType();
        Cargo cargo = spacetraderClient.seeShipDetails(shipSymbol).getData().getCargo();
        int capacity = cargo.getCapacity();
        int load = cargo.getUnits();


        if (("ASTEROID".equals(type) || "ASTEROID_FIELD".equals(type) || "ENGINEERED_ASTEROID".equals(type)) || load >= capacity) {
                eventPublisher.publishEvent(currentLocation);
                return;
            }

        Waypoints asteroidWaypoints = spacetraderClient.getAsteroidFieldLocation();
        String asteroidWaypoint = asteroidWaypoints.getData().get(0).getSymbol();
        eventPublisher.publishEvent(asteroidWaypoint);

        if (!asteroidWaypoint.equals(currentLocation)) {
            spacetraderClient.orbitShip(shipSymbol);
            this.navigateToAsteroidLocation(shipSymbol, asteroidWaypoint);
        }
    }


    public void navigateToAsteroidLocation(String shipSymbol, String waypointAsteroid) throws InterruptedException {
        String currentLocation = spacetraderClient.seeShipDetails(shipSymbol).getData().getCurrentLocation();
        if(!currentLocation.equals(waypointAsteroid)) {
            Waypoint waypoint = waypointService.findByName(waypointAsteroid);
            marketSearchService.navigateToWaypoint(waypoint, shipSymbol);
        }
    }

    public Waypoint findClosestWaypoint(List<Waypoint> waypoints, String shipSymbol) {
        ShipData shipInfo = spacetraderClient.seeShipDetails(shipSymbol).getData();
        Waypoint closestWaypoint = null;
        double distance = Double.MAX_VALUE;
        for(Waypoint waypoint: waypoints) {
            Waypoint waypointData = spacetraderClient.getWaypoint(waypoint.getSymbol());
            int waypointMarketX = waypointData.getX();
            int waypointMarketY = waypointData.getY();

            double distanceToWaypoint = shipInfo.calculateDistanceToCurrentLocation(waypointMarketX, waypointMarketY);
            if(distanceToWaypoint < distance) {
                closestWaypoint = waypoint;
                distance = distanceToWaypoint;
            }
        }
        int fuelCapacity = shipInfo.getFuel().getCapacity();
        int fuelCurrent = shipInfo.getFuel().getCurrent();
        if (distance > fuelCurrent && (fuelCurrent != fuelCapacity)) {
            spacetraderClient.fuelShip(shipSymbol, Fuel.builder().units(fuelCapacity - fuelCurrent).fromCargo(true).build());
        }
        return closestWaypoint;
    }
}
