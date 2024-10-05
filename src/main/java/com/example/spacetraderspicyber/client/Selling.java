package com.example.spacetraderspicyber.client;

import com.example.spacetraderspicyber.model.Good;
import com.example.spacetraderspicyber.service.MarketService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.Thread.sleep;

@Component
public class Selling {

    @Autowired
    private SpacetraderClient spacetraderClient;
    @Autowired
    private Contracts contracts;
    @Autowired
    private Fueling fueling;
    @Autowired
    private MarketService marketService;
    @Autowired
    private MarketSearch marketSearch;



    public void selling(String shipSymbol) throws InterruptedException {
        JSONArray inventory = new JSONObject(spacetraderClient.shipCargo(shipSymbol)).getJSONObject("data").getJSONArray("inventory");

        JSONObject contractInfo = contracts.getContractInfo();
        Good goodForDelivery = contracts.getContractDeliveryGood(contractInfo);

        for (int i = 0; i < inventory.length(); i++) {
            Good good = Good.builder()
                            .symbol(inventory.getJSONObject(i).getString("symbol"))
                            .units(inventory.getJSONObject(i).getInt("units"))
                            .build();

            if(!goodForDelivery.getSymbol().equals(good.getSymbol()) && !good.getSymbol().equals("FUEL")){
                marketSearch.goToMarket(shipSymbol, good);
                System.out.println("Selling: " + good);
                spacetraderClient.dockShip(shipSymbol);
                spacetraderClient.sellCargo(shipSymbol, good);
            }
            // TODO: Variable for JSONObject "deliver"
            JSONArray contractsArray = contractInfo.getJSONArray("data");
            int unitsFulfilled = contractsArray.getJSONObject(contractsArray.length()-1).getJSONObject("terms").getJSONArray("deliver").getJSONObject(0).getInt("unitsFulfilled");
            int unitsRequired = contractsArray.getJSONObject(contractsArray.length()-1).getJSONObject("terms").getJSONArray("deliver").getJSONObject(0).getInt("unitsRequired");
            int capacity = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data").getJSONObject("cargo").getInt("capacity");

            if(goodForDelivery.getSymbol().equals(good.getSymbol()) && (good.getUnits() >= (unitsRequired-unitsFulfilled) || good.getUnits() >= capacity-8)){
                JSONObject data = contractInfo.getJSONArray("data").getJSONObject(contractsArray.length()-1);
                contracts.deliverGoodsForContract(shipSymbol, good, data);
            }
        }
        spacetraderClient.orbitShip(shipSymbol);


    }

}
