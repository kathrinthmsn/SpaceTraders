package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
import com.example.spacetraderspicyber.model.Contracts.Contract;
import com.example.spacetraderspicyber.model.ExtractionReport.ExtractedResource;
import com.example.spacetraderspicyber.model.Good;
import com.example.spacetraderspicyber.model.Ship.ShipData;
import lombok.extern.slf4j.Slf4j;
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
        while (lastContract.isAccepted() && !lastContract.isFulfilled()) {
            Good goodForDelivery = contractsService.getContractDeliveryGood(contracts);
            if (!Good.isNotMinable(goodForDelivery)) {
                mining.flyToAsteroid(shipSymbol, goodForDelivery);
                if (scanningService.goodFoundInSurvey(shipSymbol, goodForDelivery)) {
                    extractMinerals(shipSymbol, contracts);
                }
            }
            sellingService.selling(shipSymbol);
        }
    }

    private void extractMinerals(String shipSymbol, List<Contract> contracts) throws InterruptedException {
        ShipData shipData = spacetraderClient.seeShipDetails(shipSymbol).getData();
        while (!shipData.cargoFull()) {
            ExtractedResource extractedResource = extractingMineralsUntilShipCargoIsFull(shipSymbol);
            shipData = spacetraderClient.seeShipDetails(shipSymbol).getData();
            Good deliveryGood = contractsService.getContractDeliveryGood(contracts);
            if (!extractedResource.getExtraction().getYield().getSymbol().equals(deliveryGood.getSymbol())) {
                throwCargoIntoSpace(extractedResource, shipSymbol);
            }
        }
        if (!shipData.getNav().getStatus().equals("DOCKED")) {
            spacetraderClient.dockShip(shipSymbol);
        }
    }

    private void throwCargoIntoSpace(ExtractedResource extractedResource, String shipSymbol) {
        String extractedMineral = extractedResource.getExtraction().getYield().getSymbol();
        int yield = extractedResource.getExtraction().getYield().getUnits();
        Good extractedGood = Good.builder().symbol(extractedMineral).units(yield).build();
        spacetraderClient.jettisonCargo(shipSymbol, extractedGood);
        log.info("Throwing: {} {} in the garbage collector.", yield, extractedMineral);
    }

    private ExtractedResource extractingMineralsUntilShipCargoIsFull(String shipSymbol) throws InterruptedException {
        ExtractedResource extractedResource = spacetraderClient.extractMinerals(shipSymbol).getData();
        String extractedMineral = extractedResource.getExtraction().getYield().getSymbol();
        int yield = extractedResource.getExtraction().getYield().getUnits();
        log.info("Extracted Mineral: {} {}", yield, extractedMineral);
        int cooldown = extractedResource.getCooldown().getTotalSeconds();
        log.info("{} sleepy time for: {}s", shipSymbol, cooldown);
        sleep(cooldown * 1000L);
        return extractedResource;
    }
}
