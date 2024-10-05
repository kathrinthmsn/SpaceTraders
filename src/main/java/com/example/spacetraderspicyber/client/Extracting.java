package com.example.spacetraderspicyber.client;

import com.example.spacetraderspicyber.events.EventPublisher;
import com.example.spacetraderspicyber.model.Good;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static java.lang.Thread.sleep;

@Component
public class Extracting {

    @Autowired
    private SpacetraderClient spacetraderClient;
    @Autowired
    private Fueling fueling;
    @Autowired
    private Mining mining;
    @Autowired
    private Scanning scanning;
    @Autowired
    private Selling selling;
    @Autowired
    private Contracts contracts;

    public void goToExtractMinerals(String shipSymbol) throws InterruptedException {

        JSONObject contractInfo = contracts.getContractInfo();
        JSONArray contractsArray = contractInfo.getJSONArray("data");
        boolean fulfilled = contractsArray.getJSONObject(contractsArray.length()-1).getBoolean("fulfilled");
        boolean accepted = contractsArray.getJSONObject(contractsArray.length()-1).getBoolean("accepted");


        while(accepted && !fulfilled) {
            mining.flyToAsteroid(shipSymbol);
            if(scanning.surveySucceful(shipSymbol)) {
                this.extractMinerals(shipSymbol, contractInfo);
            }
              selling.selling(shipSymbol);
        }
    }

    public void extractMinerals(String shipSymbol, JSONObject contractInfo) throws InterruptedException {

        String shipInfo = spacetraderClient.seeShipDetails(shipSymbol);

        JSONObject shipData = new JSONObject(shipInfo).getJSONObject("data");
        JSONObject cargo = shipData.getJSONObject("cargo");
        System.out.println("Cargo before extracting: " + cargo);
        int load = cargo.getInt("units");
        int capacity = cargo.getInt("capacity");

        while (load < capacity) {
            JSONObject extracted = new JSONObject(spacetraderClient.extractMinerals(shipSymbol));
            String extractedMineral = extracted.getJSONObject("data").getJSONObject("extraction").getJSONObject("yield").getString("symbol");
            int yield = extracted.getJSONObject("data").getJSONObject("extraction").getJSONObject("yield").getInt("units");
            System.out.println("Extracted Mineral: " + yield + " " + extractedMineral);
            load += yield;
            int cooldown = extracted.getJSONObject("data").getJSONObject("cooldown").getInt("totalSeconds");
            System.out.println(shipSymbol + " sleepy time for: " + cooldown + "s");
            sleep(cooldown * 1000L);

            if(!extractedMineral.equals(contracts.getContractDeliveryGood(contractInfo).getSymbol())) {
                Good extractedGood = Good.builder().symbol(extractedMineral).units(yield).build();
                spacetraderClient.jettisonCargo(shipSymbol, extractedGood);
                System.out.println("Throwing: " + yield + " " + extractedMineral + " in the garbage collector.");

            }
        }

        String status = shipData.getJSONObject("nav").getString("status");

        if (!status.equals("DOCKED")) {
            spacetraderClient.dockShip(shipSymbol);
        }
    }
}
