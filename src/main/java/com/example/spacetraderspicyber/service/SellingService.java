package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
import com.example.spacetraderspicyber.model.Contracts.Contract;
import com.example.spacetraderspicyber.model.Good;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
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
    private FuelingService fuelingService;
    @Autowired
    private MarketService marketService;
    @Autowired
    private MarketSearchService marketSearchService;



    public void selling(String shipSymbol) throws InterruptedException {
        JSONArray inventory = new JSONObject(spacetraderClient.shipCargo(shipSymbol)).getJSONObject("data").getJSONArray("inventory");

        List<Contract> contracts = contractsService.getContractInfo();
        Good goodForDelivery = contractsService.getContractDeliveryGood(contracts);

        for (int i = 0; i < inventory.length(); i++) {
            Good good = Good.builder()
                            .symbol(inventory.getJSONObject(i).getString("symbol"))
                            .units(inventory.getJSONObject(i).getInt("units"))
                            .build();

            if(!goodForDelivery.getSymbol().equals(good.getSymbol()) && !good.getSymbol().equals("FUEL")){
                marketSearchService.goToMarket(shipSymbol, good);
                log.info("Selling: {}", good);
                spacetraderClient.dockShip(shipSymbol);
                spacetraderClient.sellCargo(shipSymbol, good);
            }
            // TODO: Variable for JSONObject "deliver"
            Contract lastContract = contracts.get(contracts.size() - 1);
            int unitsRequired = lastContract.getTerms().getDeliver().get(0).getUnitsRequired();
            int unitsFulfilled = lastContract.getTerms().getDeliver().get(0).getUnitsFulfilled();
            int capacity = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data").getJSONObject("cargo").getInt("capacity");

            if(goodForDelivery.getSymbol().equals(good.getSymbol()) && (good.getUnits() >= (unitsRequired-unitsFulfilled) || good.getUnits() >= capacity-8)){
                contractsService.deliverGoodsForContract(shipSymbol, good, lastContract);
            }
        }
        spacetraderClient.orbitShip(shipSymbol);


    }

}
