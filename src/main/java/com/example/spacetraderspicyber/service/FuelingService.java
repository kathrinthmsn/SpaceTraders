package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
import com.example.spacetraderspicyber.model.*;
import com.example.spacetraderspicyber.model.Cargo.Inventory;
import com.example.spacetraderspicyber.model.Ship.ShipData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class FuelingService {

    @Autowired
    private SpacetraderClient spacetraderClient;

    public void fuelShip(String shipSymbol) {
        ShipData shipInfo = spacetraderClient.seeShipDetails(shipSymbol).getData();
        int fuelCapacity = shipInfo.getFuel().getCapacity();
        int fuelCurrent = shipInfo.getFuel().getCurrent();
        String currentLocation = shipInfo.getCurrentLocation();
        Waypoint waypoint = spacetraderClient.getWaypoint(currentLocation);
        if ((fuelCurrent != fuelCapacity) && this.hasMarketplace(waypoint)) {
            spacetraderClient.dockShip(shipSymbol);
            log.info("Fueling ship {}", shipSymbol);
            spacetraderClient.fuelShip(shipSymbol);
            spacetraderClient.orbitShip(shipSymbol);
        }
        if (this.hasMarketplace(waypoint)) {
            List<Inventory> cargoInventory = shipInfo.getCargo().getInventory();

            Inventory fuelItem = null;
            for (Inventory item : cargoInventory) {
                if (!item.getSymbol().isEmpty() && item.getSymbol().equals("FUEL")) {
                    fuelItem = item;
                    break;
                }
            }
            int fuelAmount = 0;
            if (fuelItem != null) {
                fuelAmount = fuelItem.getUnits();
                log.info("The spaceship {} has {} units of fuel.", shipSymbol, fuelAmount);
            }
            Cargo cargo = shipInfo.getCargo();
            int capacity = cargo.getCapacity();
            int load = cargo.getUnits();

            if (fuelAmount < 5 && capacity != load) {
                spacetraderClient.dockShip(shipSymbol);
                int amountToBuy = 5 - fuelAmount;
                if (capacity - load < amountToBuy) {
                    amountToBuy = capacity - load;
                }
                log.info("{} bought {} fuel.", shipSymbol, amountToBuy);
                spacetraderClient.purchaseCargo(shipSymbol, Good.builder()
                        .symbol("FUEL")
                        .units(amountToBuy)
                        .build());
                spacetraderClient.orbitShip(shipSymbol);
            }
        }
        if ((fuelCurrent != fuelCapacity) && !this.hasMarketplace(waypoint)) {
            List<Inventory> cargoInventory = shipInfo.getCargo().getInventory();

            Inventory fuelItem = null;
            for (Inventory item : cargoInventory) {
                if (!item.getSymbol().isEmpty() && item.getSymbol().equals("FUEL")) {
                    fuelItem = item;
                    break;
                }
            }
            int fuelAmount = 0;
            if (fuelItem != null) {
                fuelAmount = fuelItem.getUnits();
                log.info("The spaceship {} has {} units of fuel.", shipSymbol, fuelAmount);
                int fuelAvailable = fuelAmount * 100;
                if (fuelAvailable < fuelCapacity - fuelCurrent) {
                    fuelAmount = fuelAvailable;
                } else {
                    fuelAmount = fuelCapacity - fuelCurrent;
                }
                spacetraderClient.dockShip(shipSymbol);
                log.info("{} fueling {} units of fuel from cargo.", shipSymbol, fuelAmount);
                spacetraderClient.fuelShip(shipSymbol, Fuel.builder().units(fuelAmount).fromCargo(true).build());
                spacetraderClient.orbitShip(shipSymbol);
            }
        }
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
}
