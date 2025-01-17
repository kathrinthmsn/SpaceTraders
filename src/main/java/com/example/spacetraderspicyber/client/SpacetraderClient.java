package com.example.spacetraderspicyber.client;

import com.example.spacetraderspicyber.api.FeignConfig;
import com.example.spacetraderspicyber.model.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "api", url = "${spacetrader.api.base-url}", configuration = FeignConfig.class)
public interface SpacetraderClient {

    //TODO: get system info from endpoint
    String STARTING_SYSTEM = "X1-HJ59";

    @PostMapping(value = "/v2/register", produces = "application/json")
    String registerAgent(Agent agent);

    @GetMapping(value = "/v2/my/agent", produces = "application/json")
    Agent seeAgent();

    @GetMapping(value = "/v2/my/ships", produces = "application/json")
    String seeShips();

    //Ship

    @GetMapping(value = "/v2/my/ships/{ship}", produces = "application/json")
    Ship seeShipDetails(@PathVariable("ship") String ship);

    @RequestMapping(value =  "/v2/my/ships/{ship}/nav",
            method = RequestMethod.PATCH,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    void changeFlightMode(@PathVariable("ship") String ship, @RequestBody FlightMode flightMode);

    @PostMapping(value = "/v2/my/ships/{ship}/survey", produces = "application/json")
    SurveyResponse survey(@PathVariable("ship") String ship);

    @PostMapping(value = "/v2/my/ships/{ship}/dock")
    void dockShip(@PathVariable("ship") String ship);

    @PostMapping(value = "/v2/my/ships/{ship}/extract")
    ExtractionReport extractMinerals(@PathVariable("ship") String ship);

    @PostMapping(value = "/v2/my/ships/{ship}/refuel")
    void fuelShip(@PathVariable("ship") String ship);

    @PostMapping(value = "/v2/my/ships/{ship}/refuel", produces = "application/json")
    void fuelShip(@PathVariable("ship") String ship, Fuel fuel);

    @PostMapping(value = "/v2/my/ships/{ship}/orbit")
    void orbitShip(@PathVariable("ship") String ship);

    @PostMapping(value = "/v2/my/ships/{ship}/scan/systems", produces = "application/json")
    String scanSystems(@PathVariable("ship") String ship);

    @GetMapping(value = "/v2/my/ships/{ship}/cargo")
    Cargo shipCargo(@PathVariable("ship") String ship);

    @PostMapping(value = "/v2/my/ships/{ship}/sell", produces = "application/json")
    void sellCargo(@PathVariable("ship") String ship, Good good);

    @PostMapping(value = "/v2/my/ships/{ship}/purchase", produces = "application/json")
    Purchase purchaseCargo(@PathVariable("ship") String ship, Good good);

    @PostMapping(value = "/v2/my/ships/{ship}/jettison", produces = "application/json")
    void jettisonCargo(@PathVariable("ship") String ship, Good good);

    //Contracts

    @GetMapping(value = "/v2/my/contracts", produces = "application/json")
    Contracts getContracts(@RequestParam(value = "page", defaultValue = "1") int page,
                           @RequestParam(value = "limit", defaultValue = "20") int limit);

    default Contracts getContracts(Integer page) {
        return getContracts(page, 20);
    }

    @PostMapping(value = "/v2/my/contracts/{contractId}/accept")
    void acceptContracts(@PathVariable("contractId") String contractId);

    @PostMapping(value = "/v2/my/ships/{ship}/navigate", produces = "application/json")
    ShipNavigation navigateToWaypoint(@PathVariable("ship") String ship, WaypointSymbol waypointSymbol);

    @PostMapping(value = "/v2/my/contracts/{contractId}/deliver", produces = "application/json")
    void deliverGoodsForContracts(@PathVariable("contractId") String contractId, Good good);

    @PostMapping(value = "/v2/my/contracts/{contractId}/fulfill")
    void fulfillContracts(@PathVariable("contractId") String contractId);

    @PostMapping(value = "/v2/my/ships/{ship}/negotiate/contract", produces = "application/json")
    Contract negotiateNewContract(@PathVariable("ship") String ship);


    //System

    @GetMapping(value = "/v2/systems/{system}/waypoints", produces = "application/json")
    Waypoints getWaypoints(
            @PathVariable("system") String system,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit
    );

    default Waypoints getWaypoints(int page) {
        return getWaypoints(STARTING_SYSTEM, page, 20);
    }

    @GetMapping(value = "/v2/systems/{system}/waypoints", produces = "application/json")
    Waypoints getAsteroidFieldLocation(@PathVariable(value = "system", required = false) String system, @RequestParam(value = "type") String type);

    default Waypoints getAsteroidFieldLocation() {
        return getAsteroidFieldLocation(STARTING_SYSTEM, "ENGINEERED_ASTEROID");
    }

    @GetMapping(value = "/v2/systems/{system}/waypoints", produces = "application/json")
    String getWaypointByTraits(@PathVariable("system") String system, @RequestParam(value = "traits") String trait);

    default String getWaypointByTraits(String trait) {
        return getWaypointByTraits(STARTING_SYSTEM, trait);
    }

    @GetMapping(value = "/v2/systems/{system}/waypoints/{waypoint}/market", produces = "application/json")
    Market viewMarketData(@PathVariable("system") String system, @PathVariable("waypoint") String waypoint);

    default Market viewMarketData(String waypoint) {
        return viewMarketData(STARTING_SYSTEM, waypoint);
    }

    @GetMapping(value = "/v2/systems/{system}/waypoints/{waypoint}", produces = "application/json")
    Waypoint getWaypoint(@PathVariable("system") String system, @PathVariable("waypoint") String waypoint);

    default Waypoint getWaypoint(String waypoint) {
        return getWaypoint(STARTING_SYSTEM, waypoint);
    }
}
