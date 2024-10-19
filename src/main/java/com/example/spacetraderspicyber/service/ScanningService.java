package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
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


    public boolean goodFoundInSurvey(String shipSymbol, Good good) throws InterruptedException {
        log.info("Goods needed for Contract: {}", good.getSymbol());
        long goodForDeliveryCount = 0;
        while (goodForDeliveryCount <= 2) {
            List<Deposit> deposits = seeDeposits(shipSymbol);
            goodForDeliveryCount = deposits.stream()
                    .filter(deposit -> good.getSymbol().equals(deposit.getSymbol()))
                    .count();
            if (goodForDeliveryCount >= 2 && deposits.size() <= 5) {
                return true;
            }
        }
        return false;
    }


    private List<Deposit> seeDeposits(String shipSymbol) throws InterruptedException {
        SurveyData survey = spacetraderClient.survey(shipSymbol).getData();
        List<Deposit> deposits = survey.getSurveys().get(0).getDeposits();
        int cooldown = survey.getCooldown().getTotalSeconds();
        log.info("deposits surveyed: {}", deposits);
        log.info("{} sleepy time for {}s", shipSymbol, cooldown);
        sleep(cooldown * 1000L);
        return deposits;
    }

    public void scanAllWaypoint() {
        for (int i = 1; i <= 5; i++) {
            List<Waypoint> waypoints = spacetraderClient.getWaypoints(i).getData();
            for (Waypoint waypoint : waypoints) {
                if (waypointService.findByName(waypoint.getSymbol()) != null) {
                    log.info("Waypoint with symbol {} already exists. Skipping.", waypoint.getSymbol());
                } else {
                    waypointService.saveWaypoints(waypoint);
                    log.info("Waypoint with symbol {} added.", waypoint.getSymbol());
                }
            }

        }
    }
}

