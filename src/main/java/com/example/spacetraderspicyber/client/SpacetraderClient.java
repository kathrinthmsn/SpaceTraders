package com.example.spacetraderspicyber.client;

import com.example.spacetraderspicyber.FeignConfig;
import com.example.spacetraderspicyber.model.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "api", url = "https://api.spacetraders.io/", configuration = FeignConfig.class)
public interface SpacetraderClient {

    @PostMapping(value = "/v2/register", produces = "application/json")
    String registerAgent(Agent agent);

    @GetMapping(value = "/v2/my/agent", produces = "application/json")
    String seeAgent();

    @GetMapping(value = "/v2/my/ships", produces = "application/json")
    String seeShips();

    //Ship

    @GetMapping(value = "/v2/my/ships/{ship}", produces = "application/json")
    String seeShipDetails(@PathVariable("ship") String ship);

    @RequestMapping(value =  "/v2/my/ships/{ship}/nav",
            method = RequestMethod.PATCH,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    String changeFlightMode(@PathVariable("ship") String ship, @RequestBody FlightMode flightMode);

    @PostMapping(value = "/v2/my/ships/{ship}/survey", produces = "application/json")
    String survey(@PathVariable("ship") String ship);

    @PostMapping(value = "/v2/my/ships/{ship}/dock")
    void dockShip(@PathVariable("ship") String ship);

    @PostMapping(value = "/v2/my/ships/{ship}/extract")
    String extractMinerals(@PathVariable("ship") String ship);

    @PostMapping(value = "/v2/my/ships/{ship}/refuel")
    void fuelShip(@PathVariable("ship") String ship);

    @PostMapping(value = "/v2/my/ships/{ship}/refuel", produces = "application/json")
    void fuelShip(@PathVariable("ship") String ship, Fuel fuel);

    @PostMapping(value = "/v2/my/ships/{ship}/orbit")
    void orbitShip(@PathVariable("ship") String ship);

    @PostMapping(value = "/v2/my/ships/{ship}/scan/systems", produces = "application/json")
    String scanSystems(@PathVariable("ship") String ship);

    @GetMapping(value = "/v2/my/ships/{ship}/cargo")
    String shipCargo(@PathVariable("ship") String ship);

    @PostMapping(value = "/v2/my/ships/{ship}/sell", produces = "application/json")
    void sellCargo(@PathVariable("ship") String ship, Good good);

    @PostMapping(value = "/v2/my/ships/{ship}/purchase", produces = "application/json")
    String purchaseCargo(@PathVariable("ship") String ship, Good good);

    @PostMapping(value = "/v2/my/ships/{ship}/jettison", produces = "application/json")
    void jettisonCargo(@PathVariable("ship") String ship, Good good);

    //Contracts

    @GetMapping(value = "/v2/my/contracts?page={page}&limit=20")
    String getContracts(@PathVariable("page") Integer page);

    @PostMapping(value = "/v2/my/contracts/{contractId}/accept")
    void acceptContracts(@PathVariable("contractId") String contractId);

    @PostMapping(value = "/v2/my/ships/{ship}/navigate", produces = "application/json")
    String navigateToWaypoint(@PathVariable("ship") String ship, WaypointSymbol waypointSymbol);

    @PostMapping(value = "/v2/my/contracts/{contractId}/deliver", produces = "application/json")
    void deliverGoodsForContracts(@PathVariable("contractId") String contractId, Good good);

    @PostMapping(value = "/v2/my/contracts/{contractId}/fulfill")
    void fulfillContracts(@PathVariable("contractId") String contractId);

    @PostMapping(value = "/v2/my/ships/{ship}/negotiate/contract", produces = "application/json")
    String negotiateNewContract(@PathVariable("ship") String ship);


    //System

    //TODO: Variable System
    @GetMapping(value = "/v2/systems/X1-NN7/waypoints?page={page}&limit=20")
    String getWaypoints(@PathVariable("page") Integer page);

    //TODO: Variable System
    @GetMapping(value = "/v2/systems/X1-NN7/waypoints?type=ENGINEERED_ASTEROID")
    String getAsteroidFieldLocation();

    //TODO: Variable System
    @GetMapping(value = "/v2/systems/X1-NN7/waypoints?traits={trait}")
    String getWaypointByTratits(@PathVariable("trait") String trait);

    //TODO: Variable
    @GetMapping(value = "/v2/systems/X1-NN7/waypoints/{waypoint}/market")
    String viewMarketData(@PathVariable("waypoint") String waypoint);

    //TODO: Variable
    @GetMapping(value = "/v2/systems/X1-NN7/waypoints/{waypoint}")
    String getWaypoint(@PathVariable("waypoint") String waypoint);
}
