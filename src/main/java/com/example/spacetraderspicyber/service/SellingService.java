package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
import com.example.spacetraderspicyber.model.Cargo.Inventory;
import com.example.spacetraderspicyber.model.Contracts.Contract;
import com.example.spacetraderspicyber.model.Good;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SellingService {

    @Autowired
    private SpacetraderClient spacetraderClient;
    @Autowired
    private ContractsService contractsService;
    @Autowired
    private MarketSearchService marketSearchService;


    public void selling(String shipSymbol) throws InterruptedException {
        List<Inventory> inventory = spacetraderClient.shipCargo(shipSymbol).getInventory();
        List<Contract> contracts = contractsService.getContractInfo();
        Contract lastContract = contracts.get(contracts.size() - 1);
        Good goodForContract = contractsService.getContractDeliveryGood(contracts);
        int unitsRequired = lastContract.getTerms().getDeliver().get(0).getUnitsRequired();
        int unitsFulfilled = lastContract.getTerms().getDeliver().get(0).getUnitsFulfilled();
        int remainingUnitsForContract = unitsRequired - unitsFulfilled;

        for (Inventory item : inventory) {
            Good goodInCargo = Good.builder()
                    .symbol(item.getSymbol())
                    .units(item.getUnits())
                    .build();

            if (!goodForContract.getSymbol().equals(goodInCargo.getSymbol()) && !goodInCargo.getSymbol().equals("FUEL")) {
                sellCargoAtMarket(shipSymbol, goodInCargo);
            } else if (goodForContract.getSymbol().equals(goodInCargo.getSymbol())) {
                int unitsToDeliver = Math.min(remainingUnitsForContract, goodInCargo.getUnits());
                if (unitsToDeliver > 0 && goodInCargo.getUnits() >= remainingUnitsForContract) {
                    contractsService.deliverGoodsForContract(shipSymbol, goodInCargo, lastContract);
                    log.info("Delivering {} units of {} for contract.", unitsToDeliver, goodInCargo.getSymbol());
                }
            }
        }
        spacetraderClient.orbitShip(shipSymbol);
    }

    private void sellCargoAtMarket(String shipSymbol, Good goodInCargo) throws InterruptedException {
        marketSearchService.goToMarketForGood(shipSymbol, goodInCargo);
        log.info("Selling: {}", goodInCargo);
        spacetraderClient.dockShip(shipSymbol);
        spacetraderClient.sellCargo(shipSymbol, goodInCargo);
    }

}
