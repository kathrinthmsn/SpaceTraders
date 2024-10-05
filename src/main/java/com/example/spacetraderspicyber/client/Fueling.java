package com.example.spacetraderspicyber.client;

import com.example.spacetraderspicyber.model.Fuel;
import com.example.spacetraderspicyber.model.Good;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Fueling {

    @Autowired
    private SpacetraderClient spacetraderClient;

    public void fuelShip(String shipSymbol) {
        JSONObject shipInfo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol));
        Integer fuelCapacity = shipInfo.getJSONObject("data").getJSONObject("fuel").getInt("capacity");
        Integer fuelCurrent = shipInfo.getJSONObject("data").getJSONObject("fuel").getInt("current");
        String currentLocation = shipInfo.getJSONObject("data").getJSONObject("nav").getString("waypointSymbol");
        JSONObject waypoint = new JSONObject(spacetraderClient.getWaypoint(currentLocation)).getJSONObject("data");
        if (!fuelCurrent.equals(fuelCapacity) && this.hasMarketplace(waypoint)) {
            spacetraderClient.dockShip(shipSymbol);
            System.out.println("Fueling ship " + shipSymbol);
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
                System.out.println("The spaceship " + shipSymbol + " has " + fuelAmount + " units of fuel.");
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
                System.out.println(shipSymbol + " bought " + amountToBuy + " fuel.");
                spacetraderClient.purchaseCargo(shipSymbol, Good.builder()
                        .symbol("FUEL")
                        .units(amountToBuy)
                        .build());
                spacetraderClient.orbitShip(shipSymbol);
            }
        }
        if (!fuelCurrent.equals(fuelCapacity) && !this.hasMarketplace(waypoint)) {
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
                System.out.println("The spaceship " + shipSymbol + " has " + fuelAmount + " units of fuel.");
                int fuelAvailable = fuelAmount * 100;
                if (fuelAvailable < fuelCapacity - fuelCurrent) {
                    fuelAmount = fuelAvailable;
                } else {
                    fuelAmount = fuelCapacity - fuelCurrent;
                }
                spacetraderClient.dockShip(shipSymbol);
                System.out.println(shipSymbol + " fueling " + fuelAmount + " units of fuel from cargo.");
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
