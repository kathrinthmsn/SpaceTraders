package com.example.spacetraderspicyber.client;

import com.example.spacetraderspicyber.model.*;
import com.example.spacetraderspicyber.service.MarketService;
import com.example.spacetraderspicyber.service.WaypointService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;

@Component
public class Contracts {

    @Autowired
    private SpacetraderClient spacetraderClient;
    @Autowired
    private Fueling fueling;
    @Autowired
    private MarketSearch marketSearch;
    @Autowired
    private WaypointService waypointService;

    //TODO: Variable for deliver Array
    public Good getContractDeliveryGood(JSONObject contractInfo){
        JSONArray contractsArray = contractInfo.getJSONArray("data");
        JSONObject contractData = contractInfo.getJSONArray("data").getJSONObject(contractsArray.length()-1);
        int unitsRequired = contractData.getJSONObject("terms").getJSONArray("deliver").getJSONObject(0).getInt("unitsRequired");

        return Good.builder()
                .symbol(contractInfo.getJSONArray("data").getJSONObject(contractsArray.length()-1).getJSONObject("terms").getJSONArray("deliver").getJSONObject(0).getString("tradeSymbol"))
                .units(unitsRequired)
                .build();
    }

    public Good getGoodLeftToDeliverForContract(JSONObject contractInfo){
        JSONArray contractsArray = contractInfo.getJSONArray("data");
        JSONObject contractData = contractInfo.getJSONArray("data").getJSONObject(contractsArray.length()-1);
        int unitsRequired = contractData.getJSONObject("terms").getJSONArray("deliver").getJSONObject(0).getInt("unitsRequired");
        int unitsFulfilled = contractData.getJSONObject("terms").getJSONArray("deliver").getJSONObject(0).getInt("unitsFulfilled");

        return Good.builder()
                .symbol(contractInfo.getJSONArray("data").getJSONObject(contractsArray.length()-1).getJSONObject("terms").getJSONArray("deliver").getJSONObject(0).getString("tradeSymbol"))
                .units(unitsRequired - unitsFulfilled)
                .build();
    }


    public void checkContractValidity() throws InterruptedException {
        JSONObject contractInfo = this.getContractInfo();
        JSONArray contractsArray = contractInfo.getJSONArray("data");
        Instant deadline = Instant.parse(contractsArray.getJSONObject(contractsArray.length()-1).getJSONObject("terms").getString("deadline"));
        boolean accepted = contractsArray.getJSONObject(contractsArray.length()-1).getBoolean("accepted");
        String contractId = contractsArray.getJSONObject(contractsArray.length()-1).getString("id");

        if(deadline.isAfter(Instant.now()) && !accepted){
            this.acceptContract(contractId);
        }

    }

    public JSONObject getContractInfo() throws InterruptedException {
        int numberOfContracts = new JSONObject(spacetraderClient.getContracts(1)).getJSONObject("meta").getInt("total");
        int pageSize = 20;
        int pageNumber = (numberOfContracts + pageSize - 1) / pageSize;
       return new JSONObject(spacetraderClient.getContracts(pageNumber));
    }

    public void acceptContract(String contractId){
        System.out.println("Accepting new Contract: " + contractId);
        spacetraderClient.acceptContracts(contractId);
    }


    public void purchaseCargoForContract(String shipSymbol) throws InterruptedException {
        JSONObject cargo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data").getJSONObject("cargo");
        int capacity = cargo.getInt("capacity");
        int load = cargo.getInt("units");
        JSONObject contractInfo = this.getContractInfo();
        JSONArray contractsArray = contractInfo.getJSONArray("data");
        Good goodForDelivery = this.getGoodLeftToDeliverForContract(contractInfo);
        spacetraderClient.dockShip(shipSymbol);
        if(goodForDelivery.getUnits() > capacity - load){
            goodForDelivery.setUnits(capacity - load);
        }
        JSONObject purchase = new JSONObject(spacetraderClient.purchaseCargo(shipSymbol, goodForDelivery)).getJSONObject("data").getJSONObject("transaction");
        int purchasePrice = purchase.getInt("totalPrice");

        System.out.println("Purchasing Good for Contract: " + goodForDelivery + " for " + purchasePrice + "$");
        JSONObject data = contractInfo.getJSONArray("data").getJSONObject(contractsArray.length()-1);

        this.deliverGoodsForContract(shipSymbol, goodForDelivery, data);

    }

    public void deliverGoodsForContract (String shipSymbol, Good good, JSONObject data) throws InterruptedException {
        spacetraderClient.orbitShip(shipSymbol);
        fueling.fuelShip(shipSymbol);
        String destination = data.getJSONObject("terms").getJSONArray("deliver").getJSONObject(0).getString("destinationSymbol");
        JSONObject shipInfo = new JSONObject(spacetraderClient.seeShipDetails(shipSymbol)).getJSONObject("data").getJSONObject("nav");
        String currentLocation = shipInfo.getString("waypointSymbol");
        if(!currentLocation.equals(destination)) {
            Waypoint waypoint = waypointService.findByName(destination);
            marketSearch.navigateToWaypoint(waypoint, shipSymbol);
        }

        Good GoodToDeliver = Good.builder()
                .units(good.getUnits())
                .tradeSymbol(good.getSymbol())
                .build();
        this.deliverGoodsForContracts(shipSymbol, GoodToDeliver, data);

        spacetraderClient.orbitShip(shipSymbol);
        return;
    }

    //TODO: Variable Shipsymbol
    public void deliverGoodsForContracts(String shipSymbol, Good good, JSONObject data) throws InterruptedException {
        String contractId = data.getString("id");
        good.setShipSymbol(shipSymbol);
        good.setUnits(good.getUnits());
        spacetraderClient.dockShip(shipSymbol);
        System.out.println(shipSymbol + " is delivering Good for Contract: " + good.toStringContracts());
        spacetraderClient.deliverGoodsForContracts(contractId, good);

        JSONObject contractInfo = this.getContractInfo();
        JSONArray contractsArray = contractInfo.getJSONArray("data");
        JSONObject ContractData = contractInfo.getJSONArray("data").getJSONObject(contractsArray.length()-1);
        int unitsRequired = ContractData.getJSONObject("terms").getJSONArray("deliver").getJSONObject(0).getInt("unitsRequired");
        int unitsFulfilled = ContractData.getJSONObject("terms").getJSONArray("deliver").getJSONObject(0).getInt("unitsFulfilled");

        if(unitsFulfilled >= unitsRequired) {
            spacetraderClient.fulfillContracts(contractId);
            System.out.println("Contract fulfilled: " + contractId);
            JSONObject newContract = new JSONObject(spacetraderClient.negotiateNewContract(shipSymbol)).getJSONObject("data").getJSONObject("contract");
            String newContractId = newContract.getString("id");
            this.acceptContract(newContractId);
        }
    }
}
