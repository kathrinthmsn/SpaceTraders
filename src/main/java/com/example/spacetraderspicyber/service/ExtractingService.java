package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
import com.example.spacetraderspicyber.model.Contracts.Contract;
import com.example.spacetraderspicyber.model.Good;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static java.lang.Thread.sleep;

@Slf4j
@Component
public class ExtractingService {

    @Autowired
    private SpacetraderClient spacetraderClient;
    @Autowired
    private FuelingService fuelingService;
    @Autowired
    private MiningService mining;
    @Autowired
    private ScanningService scanningService;
    @Autowired
    private SellingService sellingService;
    @Autowired
    private ContractsService contractsService;

    public void goToExtractMinerals(String shipSymbol) throws InterruptedException {

        List<Contract> contracts = contractsService.getContractInfo();
        Contract lastContract = contracts.get(contracts.size() - 1);
        boolean fulfilled = lastContract.isFulfilled();
        boolean accepted = lastContract.isAccepted();


        while(accepted && !fulfilled) {
            mining.flyToAsteroid(shipSymbol);
            if (scanningService.surveySuccessful(shipSymbol)) {
                this.extractMinerals(shipSymbol, contracts);
            }
            sellingService.selling(shipSymbol);
        }
    }

    public void extractMinerals(String shipSymbol, List<Contract> contracts) throws InterruptedException {

        String shipInfo = spacetraderClient.seeShipDetails(shipSymbol);

        JSONObject shipData = new JSONObject(shipInfo).getJSONObject("data");
        JSONObject cargo = shipData.getJSONObject("cargo");
        log.info("Cargo before extracting: {}", cargo);
        int load = cargo.getInt("units");
        int capacity = cargo.getInt("capacity");

        while (load < capacity) {
            JSONObject extracted = new JSONObject(spacetraderClient.extractMinerals(shipSymbol));
            String extractedMineral = extracted.getJSONObject("data").getJSONObject("extraction").getJSONObject("yield").getString("symbol");
            int yield = extracted.getJSONObject("data").getJSONObject("extraction").getJSONObject("yield").getInt("units");
            log.info("Extracted Mineral: {} {}", yield, extractedMineral);
            load += yield;
            int cooldown = extracted.getJSONObject("data").getJSONObject("cooldown").getInt("totalSeconds");
            log.info("{} sleepy time for: {}s", shipSymbol, cooldown);
            sleep(cooldown * 1000L);

            if (!extractedMineral.equals(contractsService.getContractDeliveryGood(contracts).getSymbol())) {
                Good extractedGood = Good.builder().symbol(extractedMineral).units(yield).build();
                spacetraderClient.jettisonCargo(shipSymbol, extractedGood);
                log.info("Throwing: {} {} in the garbage collector.", yield, extractedMineral);

            }
        }

        String status = shipData.getJSONObject("nav").getString("status");

        if (!status.equals("DOCKED")) {
            spacetraderClient.dockShip(shipSymbol);
        }
    }
}
