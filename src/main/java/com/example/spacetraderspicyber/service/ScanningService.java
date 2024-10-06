package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
import com.example.spacetraderspicyber.model.Contracts.Contract;
import com.example.spacetraderspicyber.model.Good;
import com.example.spacetraderspicyber.model.Trait;
import com.example.spacetraderspicyber.model.Waypoint;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

@Slf4j
@Component
public class ScanningService {

    @Autowired
    private SpacetraderClient spacetraderClient;
    @Autowired
    private ContractsService contractsService;
    @Autowired
    private WaypointService waypointService;


    public boolean surveySuccessful(String shipSymbol) throws InterruptedException {
        List<Contract> contracts = contractsService.getContractInfo();
        Good goodForDelivery = contractsService.getContractDeliveryGood(contracts);
        if (Good.isNotMinable(goodForDelivery)) {
            return false;
        }
        JSONObject cargo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data").getJSONObject("cargo");
        int load = cargo.getInt("units");
        int capacity = cargo.getInt("capacity");

        if (load < capacity) {

            log.info("Goods needed for Contract: {}", goodForDelivery.getSymbol());
            int goodForDeliveryCount = 0;
            while(goodForDeliveryCount <= 2) {
                JSONArray deposits = this.seeDeposits(shipSymbol);
                if (deposits.toString().contains(goodForDelivery.getSymbol())) {
                    goodForDeliveryCount = 0;
                    for (int i = 0; i < deposits.length(); i++) {
                        JSONObject depositObject = deposits.getJSONObject(i);
                        if (goodForDelivery.getSymbol().equals(depositObject.getString("symbol"))) {
                            goodForDeliveryCount++;
                        }
                        if (goodForDeliveryCount >= 2 && deposits.length() <= 5) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        return false;
    }

    public JSONArray seeDeposits(String shipSymbol) throws InterruptedException {
        JSONObject survey = new JSONObject(spacetraderClient.survey(shipSymbol));
        JSONArray deposits = survey.getJSONObject("data").getJSONArray("surveys").getJSONObject(0).getJSONArray("deposits");
        int cooldown = survey.getJSONObject("data").getJSONObject("cooldown").getInt("totalSeconds");
        log.info("deposits surveyed: {}", deposits);
        log.info("{} sleepy time for {}s", shipSymbol, cooldown);
        sleep(cooldown * 1000L);
        return deposits;
    }

    public void scanAllWaypoint() throws InterruptedException {
        for(int i=1; i <=5; i++){
            JSONArray waypointsJson = new JSONObject(spacetraderClient.getWaypoints(i)).getJSONArray("data");
            List<Waypoint> waypoints = convertJsonToEntities(waypointsJson);
            for(Waypoint waypoint: waypoints){
                if (waypointService.findByName(waypoint.getSymbol()) != null) {
                    log.info("Waypoint with symbol {} already exists. Skipping.", waypoint.getSymbol());
                    continue;
                }
                else {
                    waypointService.saveWaypoints(waypoint);
                }
            }

        }
    }

    public static List<Waypoint> convertJsonToEntities(JSONArray waypointsJson) {
        List<Waypoint> waypoints = new ArrayList<>();

        for (int i = 0; i < waypointsJson.length(); i++) {
            JSONObject waypointJson = waypointsJson.getJSONObject(i);
            Waypoint waypointEntity = convertJsonToEntity(waypointJson);
            waypoints.add(waypointEntity);
        }

        return waypoints;
    }

    private static Waypoint convertJsonToEntity(JSONObject waypointJson) {
        Waypoint waypointEntity = new Waypoint();

        waypointEntity.setSystemSymbol(waypointJson.getString("systemSymbol"));
        waypointEntity.setSymbol(waypointJson.getString("symbol"));
        waypointEntity.setType(waypointJson.getString("type"));
        waypointEntity.setX(waypointJson.getInt("x"));
        waypointEntity.setY(waypointJson.getInt("y"));
        waypointEntity.setUnderConstruction(waypointJson.getBoolean("isUnderConstruction"));

        // Convert traits to a list of Trait entities
        List<Trait> traits = new ArrayList<>();
        JSONArray traitsJson = waypointJson.getJSONArray("traits");
        for (int i = 0; i < traitsJson.length(); i++) {
            JSONObject traitJson = traitsJson.getJSONObject(i);
            Trait trait = new Trait();
            trait.setSymbol(traitJson.getString("symbol"));
            trait.setName(traitJson.getString("name"));
            trait.setDescription(traitJson.getString("description"));
            traits.add(trait);
        }
        waypointEntity.setTraits(traits);

        return waypointEntity;
    }
}

