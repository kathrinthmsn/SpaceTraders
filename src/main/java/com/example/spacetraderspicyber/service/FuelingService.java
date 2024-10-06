package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
import com.example.spacetraderspicyber.model.Fuel;
import com.example.spacetraderspicyber.model.Good;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FuelingService {

    @Autowired
    private SpacetraderClient spacetraderClient;

    public void fuelShip(String shipSymbol) {
        JSONObject shipInfo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol));
        int fuelCapacity = shipInfo.getJSONObject("data").getJSONObject("fuel").getInt("capacity");
        int fuelCurrent = shipInfo.getJSONObject("data").getJSONObject("fuel").getInt("current");
        String currentLocation = shipInfo.getJSONObject("data").getJSONObject("nav").getString("waypointSymbol");
        JSONObject waypoint = new JSONObject(spacetraderClient.getWaypoint(currentLocation)).getJSONObject("data");
        if ((fuelCurrent != fuelCapacity) && this.hasMarketplace(waypoint)) {
            spacetraderClient.dockShip(shipSymbol);
            log.info("Fueling ship {}", shipSymbol);
            spacetraderClient.fuelShip(shipSymbol);
            spacetraderClient.orbitShip(shipSymbol);
        }
        if (this.hasMarketplace(waypoint)) {
            JSONArray cargoInventory = shipInfo.getJSONObject("data")
                    .getJSONObject("cargo").getJSONArray("inventory");

            // Search for fuel item in the inventory
            JSONObject fuelItem = null;
            for (int i = 0; i < cargoInventory.length(); i++) {
                JSONObject item = cargoInventory.getJSONObject(i);
                if (item.has("symbol") && item.getString("symbol").equals("FUEL")) {
                    fuelItem = item;
                    break;
                }
            }
            int fuelAmount = 0;
            if (fuelItem != null) {
                fuelAmount = fuelItem.getInt("units");
                log.info("The spaceship {} has {} units of fuel.", shipSymbol, fuelAmount);
            }
            JSONObject cargo = shipInfo.getJSONObject("data").getJSONObject("cargo");
            int capacity = cargo.getInt("capacity");
            int load = cargo.getInt("units");

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
            JSONArray cargoInventory = shipInfo.getJSONObject("data")
                    .getJSONObject("cargo").getJSONArray("inventory");

            // Search for fuel item in the inventory
            JSONObject fuelItem = null;
            for (int i = 0; i < cargoInventory.length(); i++) {
                JSONObject item = cargoInventory.getJSONObject(i);
                if (item.has("symbol") && item.getString("symbol").equals("FUEL")) {
                    fuelItem = item;
                    break;
                }
            }
            int fuelAmount = 0;
            if (fuelItem != null) {
                fuelAmount = fuelItem.getInt("units");
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
}
