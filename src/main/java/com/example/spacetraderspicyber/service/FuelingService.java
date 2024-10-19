package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
import com.example.spacetraderspicyber.model.Cargo.Inventory;
import com.example.spacetraderspicyber.model.*;
import com.example.spacetraderspicyber.model.Ship.ShipData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class FuelingService {

    @Autowired
    private SpacetraderClient spacetraderClient;
    @Autowired
    private WaypointService waypointService;
    @Autowired
    private MarketService marketService;

    public void fuelShip(String shipSymbol) {
        ShipData shipInfo = spacetraderClient.seeShipDetails(shipSymbol).getData();
        Waypoint waypoint = waypointService.findByName(shipInfo.getCurrentLocation());
        int amountOfFuelInCargo = findFuelInCargo(shipInfo);
        if (waypointService.hasMarketplace(waypoint)) {
            amountOfFuelInCargo = buyingFuelForCargo(shipInfo, amountOfFuelInCargo);
            if (shipInfo.shipNotFullyFueled()) {
                puttingFuelInTheTankFromMarket(shipSymbol);
            }
        }
        if ((shipInfo.shipNotFullyFueled()) && !waypointService.hasMarketplace(waypoint)) {
            puttingFuelInTheTankFromCargo(shipInfo, amountOfFuelInCargo);
        }
    }

    public void puttingFuelInTheTankFromMarket(String shipSymbol) {
        spacetraderClient.dockShip(shipSymbol);
        log.info("Fueling ship {}", shipSymbol);
        spacetraderClient.fuelShip(shipSymbol);
        spacetraderClient.orbitShip(shipSymbol);
    }

    public void puttingFuelInTheTankFromCargo(ShipData shipInfo, int amountOfFuelInCargo) {
        if (amountOfFuelInCargo != 0) {
            int fuelAvailableForTankInCargo = amountOfFuelInCargo * 100;
            if (fuelAvailableForTankInCargo > shipInfo.getRemainingFuelCapacity()) {
                fuelAvailableForTankInCargo = shipInfo.getRemainingFuelCapacity();
            }
            spacetraderClient.dockShip(shipInfo.getSymbol());
            log.info("{} fueling {}l of fuel from cargo.", shipInfo.getSymbol(), fuelAvailableForTankInCargo);
            spacetraderClient.fuelShip(shipInfo.getSymbol(), Fuel.builder().units(fuelAvailableForTankInCargo).fromCargo(true).build());
            spacetraderClient.orbitShip(shipInfo.getSymbol());
        }
    }

    public int buyingFuelForCargo(ShipData shipInfo, int fuelInCargo) {
        if (fuelInCargo < 5 && shipInfo.cargoFull()) {
            spacetraderClient.dockShip(shipInfo.getSymbol());
            int amountToBuy = 5 - fuelInCargo;
            if (shipInfo.getRemainingCargoCapacity() < amountToBuy) {
                amountToBuy = shipInfo.getRemainingCargoCapacity();
            }
            log.info("{} bought {} fuel.", shipInfo.getSymbol(), amountToBuy);
            spacetraderClient.purchaseCargo(shipInfo.getSymbol(), Good.builder()
                    .symbol("FUEL")
                    .units(amountToBuy)
                    .build());
            spacetraderClient.orbitShip(shipInfo.getSymbol());
            return fuelInCargo + amountToBuy;
        }
        return fuelInCargo;
    }

    public int findFuelInCargo(ShipData shipInfo) {
        List<Inventory> cargoInventory = shipInfo.getCargo().getInventory();
        for (Inventory item : cargoInventory) {
            if (item.getSymbol().equals("FUEL")) {
                log.info("The spaceship {} has {} units of fuel in Cargo.", shipInfo.getSymbol(), item.getUnits());
                return item.getUnits();
            }
        }
        return 0;
    }

    public void findFuelStopOnTheWay(ShipData shipInfo, Waypoint destination) throws InterruptedException {
        String shipSymbol = shipInfo.getSymbol();
        int fuelCurrent = shipInfo.getFuel().getCurrent();
        double distanceToDestination = shipInfo.calculateDistanceToCurrentLocation(destination.getX(), destination.getY());
        if (fuelCurrent > distanceToDestination) {
            return;
        }
        List<Waypoint> waypoints = waypointService.findAll();
        while (distanceToDestination > fuelCurrent) {
            String currentLocationSymbol = shipInfo.getNav().getWaypointSymbol();
            Waypoint currentLocation = waypointService.findByName(currentLocationSymbol);
            waypoints.removeAll(List.of(currentLocation, destination));
            Waypoint nextStop = findNextReachableStop(shipInfo, waypoints, destination);

            if (nextStop == null) {
                Waypoint closestMarket = findClosestMarketWithFuel(shipInfo, waypoints);
                if (closestMarket != null) {
                    handleRefuelingAtMarket(shipInfo, closestMarket);
                } else {
                    throw new RuntimeException("No reachable markets with fuel found!");
                }
                shipInfo = spacetraderClient.seeShipDetails(shipSymbol).getData();
                fuelCurrent = shipInfo.getFuel().getCurrent();
                distanceToDestination = shipInfo.calculateDistanceToCurrentLocation(destination.getX(), destination.getY());
            }
        }
    }

    private Waypoint findNextReachableStop(ShipData shipInfo, List<Waypoint> waypoints, Waypoint destination) {
        Waypoint nextStop = null;
        double smallestDistance = Double.MAX_VALUE;
        for (Waypoint waypoint : waypoints) {
            double distanceToWaypoint = shipInfo.calculateDistanceToCurrentLocation(waypoint.getX(), waypoint.getY());
            double distanceFromWaypointToDestination = waypointService.calculateDistanceBetweenWaypoints(waypoint, destination);

            if (distanceToWaypoint <= shipInfo.getFuel().getCurrent() && distanceFromWaypointToDestination < smallestDistance) {
                nextStop = waypoint;
                smallestDistance = distanceFromWaypointToDestination;
            }
        }
        return nextStop;
    }

    private Waypoint findClosestMarketWithFuel(ShipData shipInfo, List<Waypoint> waypoints) {
        Waypoint closestMarket = null;
        double closestDistance = Double.MAX_VALUE;
        for (Waypoint waypoint : waypoints) {
            double distanceToMarket = shipInfo.calculateDistanceToCurrentLocation(waypoint.getX(), waypoint.getY());
            Optional<Market> marketOpt = Optional.ofNullable(marketService.findByName(waypoint.getSymbol()));
            if (marketOpt.isPresent() && distanceToMarket < closestDistance) {
                closestMarket = waypoint;
                closestDistance = distanceToMarket;
            }
        }
        return closestMarket;
    }

    private void handleRefuelingAtMarket(ShipData shipData, Waypoint market) throws InterruptedException {
        if (!fuelEnoughUntilDestination(market, shipData)) {
            spacetraderClient.changeFlightMode(shipData.getSymbol(), FlightMode.builder().flightMode("DRIFT").build());
        }
        waypointService.navigateWithoutFueling(market.getSymbol(), shipData.getSymbol());
        fuelShip(shipData.getSymbol());
        spacetraderClient.changeFlightMode(shipData.getSymbol(), FlightMode.builder().flightMode("CRUISE").build());
    }

    private boolean fuelEnoughUntilDestination(Waypoint waypoint, ShipData shipInfo) {
        double distanceToWaypoint = shipInfo.calculateDistanceToCurrentLocation(waypoint.getX(), waypoint.getY());
        int fuelCurrent = shipInfo.getFuel().getCurrent();
        return fuelCurrent > distanceToWaypoint;
    }


}
