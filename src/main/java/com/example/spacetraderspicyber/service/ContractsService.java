package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
import com.example.spacetraderspicyber.model.Cargo;
import com.example.spacetraderspicyber.model.Contract.ContractData;
import com.example.spacetraderspicyber.model.Contracts.Contract;
import com.example.spacetraderspicyber.model.Good;
import com.example.spacetraderspicyber.model.Transaction;
import com.example.spacetraderspicyber.model.Waypoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class ContractsService {

    @Autowired
    private SpacetraderClient spacetraderClient;
    @Autowired
    private FuelingService fuelingService;
    @Autowired
    private MarketSearchService marketSearchService;
    @Autowired
    private WaypointService waypointService;

    //TODO: Variable for deliver Array
    public Good getContractDeliveryGood(List<Contract> contracts) {
        Contract lastContract = contracts.get(contracts.size() - 1);
        int unitsRequired = lastContract.getTerms().getDeliver().get(0).getUnitsRequired();

        return Good.builder()
                .symbol(lastContract.getTerms().getDeliver().get(0).getTradeSymbol())
                .units(unitsRequired)
                .build();
    }

    public Good getGoodLeftToDeliverForContract(List<Contract> contracts) {
        Contract lastContract = contracts.get(contracts.size() - 1);
        int unitsRequired = lastContract.getTerms().getDeliver().get(0).getUnitsRequired();
        int unitsFulfilled = lastContract.getTerms().getDeliver().get(0).getUnitsFulfilled();

        return Good.builder()
                .symbol(lastContract.getTerms().getDeliver().get(0).getTradeSymbol())
                .units(unitsRequired - unitsFulfilled)
                .build();
    }


    public void checkContractValidity() {
        List<Contract> contracts = this.getContractInfo();
        Contract lastContract = contracts.get(contracts.size() - 1);
        Instant deadline = Instant.parse(lastContract.getTerms().getDeadline());
        boolean accepted = lastContract.isAccepted();
        String contractId = lastContract.getId();

        if (deadline.isAfter(Instant.now()) && !accepted) {
            this.acceptContract(contractId);
        }

    }

    public List<Contract> getContractInfo() {
        int numberOfContracts = spacetraderClient.getContracts(1).getData().size();
        int pageSize = 20;
        int pageNumber = (numberOfContracts + pageSize - 1) / pageSize;
        return spacetraderClient.getContracts(pageNumber).getData();
    }

    public void acceptContract(String contractId) {
        log.info("Accepting new Contract: {}", contractId);
        spacetraderClient.acceptContracts(contractId);
    }


    public void purchaseCargoForContract(String shipSymbol) throws InterruptedException {
        Cargo cargo = spacetraderClient.seeShipDetails(shipSymbol).getData().getCargo();
        int capacity = cargo.getCapacity();
        int load = cargo.getUnits();
        List<Contract> contracts = this.getContractInfo();
        Good goodForDelivery = this.getGoodLeftToDeliverForContract(contracts);
        spacetraderClient.dockShip(shipSymbol);
        if (goodForDelivery.getUnits() > capacity - load) {
            goodForDelivery.setUnits(capacity - load);
        }
        Transaction purchase = spacetraderClient.purchaseCargo(shipSymbol, goodForDelivery).getData().getTransaction();
        int purchasePrice = purchase.getTotalPrice();
        log.info("Purchasing Good for Contract: {} for {}$", goodForDelivery, purchasePrice);
        this.deliverGoodsForContract(shipSymbol, goodForDelivery, contracts.get(contracts.size() - 1));
    }

    public void deliverGoodsForContract(String shipSymbol, Good good, Contract contract) throws InterruptedException {
        spacetraderClient.orbitShip(shipSymbol);
        fuelingService.fuelShip(shipSymbol);
        String destination = contract.getTerms().getDeliver().get(0).getDestinationSymbol();
        String currentLocation = spacetraderClient.seeShipDetails(shipSymbol).getData().getCurrentLocation();
        if (!currentLocation.equals(destination)) {
            Waypoint waypoint = waypointService.findByName(destination);
            marketSearchService.navigateToWaypoint(waypoint, shipSymbol);
        }

        Good GoodToDeliver = Good.builder()
                .units(good.getUnits())
                .tradeSymbol(good.getSymbol())
                .build();
        this.deliverGoodsForContracts(shipSymbol, GoodToDeliver, contract);

        spacetraderClient.orbitShip(shipSymbol);
    }

    //TODO: Variable Shipsymbol
    public void deliverGoodsForContracts(String shipSymbol, Good good, Contract contract) {
        String contractId = contract.getId();
        good.setShipSymbol(shipSymbol);
        good.setUnits(good.getUnits());
        spacetraderClient.dockShip(shipSymbol);
        log.info("{} is delivering Good for Contract: {}", shipSymbol, good.toStringContracts());
        spacetraderClient.deliverGoodsForContracts(contractId, good);

        List<Contract> contracts = this.getContractInfo();
        Contract lastContract = contracts.get(contracts.size() - 1);
        int unitsRequired = lastContract.getTerms().getDeliver().get(0).getUnitsRequired();
        int unitsFulfilled = lastContract.getTerms().getDeliver().get(0).getUnitsFulfilled();

        if (unitsFulfilled >= unitsRequired) {
            spacetraderClient.fulfillContracts(contractId);
            log.info("Contract fulfilled: {}", contractId);
            ContractData newContract = spacetraderClient.negotiateNewContract(shipSymbol).getData();
            String newContractId = newContract.getId();
            this.acceptContract(newContractId);
        }
    }
}
