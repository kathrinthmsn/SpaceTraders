package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
import com.example.spacetraderspicyber.model.Ship.ShipData;
import com.example.spacetraderspicyber.model.ShipNavigation;
import com.example.spacetraderspicyber.model.Waypoint;
import com.example.spacetraderspicyber.model.WaypointSymbol;
import com.example.spacetraderspicyber.repositories.WaypointRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.lang.Thread.sleep;

@Slf4j
@Service
public class WaypointService {
    private final WaypointRepository waypointRepository;

    @Autowired
    private SpacetraderClient spacetraderClient;

    @Autowired
    public WaypointService(WaypointRepository waypointRepository) {
        this.waypointRepository = waypointRepository;
    }

    public void saveWaypoints(Waypoint waypoint) {
        waypointRepository.save(waypoint);
    }

    public void deleteAllWaypoints() {
        waypointRepository.deleteAll();
    }
    public Waypoint findByName(String symbol){
        return waypointRepository.findBySymbol(symbol);
    }
    public List<Waypoint> findAll(){
        return waypointRepository.findAll();
    }

    public List<Waypoint> findByType(String type){
        return waypointRepository.findByTraits_Symbol(type);
    }

    public boolean hasMarketplace(Waypoint waypoint) {
        return waypoint.getTraits().stream().anyMatch(trait -> trait.getSymbol().equals("MARKETPLACE"));
    }

    public double calculateDistanceBetweenWaypoints(Waypoint waypoint, Waypoint waypointDestination) {
        return calculateDistance(waypointDestination.getX(), waypointDestination.getY(), waypoint.getX(), waypoint.getY());
    }

    private double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    public void goToWaypoint(String waypoint, String shipSymbol) throws InterruptedException {
        ShipNavigation navigation = spacetraderClient.navigateToWaypoint(shipSymbol, WaypointSymbol.builder().waypointSymbol(waypoint).build());

        long durationInSeconds = navigation.calculateRouteTime();
        log.info("{} navigates to: {}", shipSymbol, waypoint);
        log.info("{} sleepy time for: {}s", shipSymbol, durationInSeconds);
        sleep(durationInSeconds * 1000);
    }

    public void navigateWithoutFueling(String destination, String shipSymbol) throws InterruptedException {
        String currentLocation = spacetraderClient.seeShipDetails(shipSymbol).getData().getCurrentLocation();
        if (!currentLocation.equals(destination)) {
            goToWaypoint(destination, shipSymbol);
        }
    }

    public Waypoint findClosestWaypoint(List<Waypoint> waypoints, String shipSymbol) {
        ShipData shipInfo = spacetraderClient.seeShipDetails(shipSymbol).getData();
        Waypoint closestWaypoint = null;
        double distance = Double.MAX_VALUE;
        for (Waypoint waypoint : waypoints) {
            Waypoint waypointData = spacetraderClient.getWaypoint(waypoint.getSymbol());
            int waypointX = waypointData.getX();
            int waypointY = waypointData.getY();
            double distanceToWaypoint = shipInfo.calculateDistanceToCurrentLocation(waypointX, waypointY);
            if (distanceToWaypoint < distance) {
                closestWaypoint = waypoint;
                distance = distanceToWaypoint;
            }
        }
        return closestWaypoint;
    }
}
