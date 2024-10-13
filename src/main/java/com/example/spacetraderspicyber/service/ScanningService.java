package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
import com.example.spacetraderspicyber.model.Cargo;
import com.example.spacetraderspicyber.model.Contracts.Contract;
import com.example.spacetraderspicyber.model.Good;
import com.example.spacetraderspicyber.model.SurveyResponse.Deposit;
import com.example.spacetraderspicyber.model.SurveyResponse.SurveyData;
import com.example.spacetraderspicyber.model.Waypoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        Cargo cargo = spacetraderClient.seeShipDetails(shipSymbol).getData().getCargo();
        int load = cargo.getUnits();
        int capacity = cargo.getCapacity();

        if (load < capacity) {

            log.info("Goods needed for Contract: {}", goodForDelivery.getSymbol());
            int goodForDeliveryCount = 0;
            while(goodForDeliveryCount <= 2) {
                List<Deposit> deposits = this.seeDeposits(shipSymbol);
                if (deposits.toString().contains(goodForDelivery.getSymbol())) {
                    goodForDeliveryCount = 0;
                    for (Deposit deposit : deposits) {
                        if (goodForDelivery.getSymbol().equals(deposit.getSymbol())) {
                            goodForDeliveryCount++;
                        }
                        if (goodForDeliveryCount >= 2 && deposits.size() <= 5) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        return false;
    }

    public List<Deposit> seeDeposits(String shipSymbol) throws InterruptedException {
        SurveyData survey = spacetraderClient.survey(shipSymbol).getData();
        List<Deposit> deposits = survey.getSurveys().get(0).getDeposits();
        int cooldown = survey.getCooldown().getTotalSeconds();
        log.info("deposits surveyed: {}", deposits);
        log.info("{} sleepy time for {}s", shipSymbol, cooldown);
        sleep(cooldown * 1000L);
        return deposits;
    }

    public void scanAllWaypoint() {
        for(int i=1; i <=5; i++){
            List<Waypoint> waypoints = spacetraderClient.getWaypoints(i).getData();
            for(Waypoint waypoint: waypoints){
                if (waypointService.findByName(waypoint.getSymbol()) != null) {
                    log.info("Waypoint with symbol {} already exists. Skipping.", waypoint.getSymbol());
                }
                else {
                    waypointService.saveWaypoints(waypoint);
                }
            }

        }
    }
}

