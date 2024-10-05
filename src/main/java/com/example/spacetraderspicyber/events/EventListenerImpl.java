package com.example.spacetraderspicyber.events;

import com.example.spacetraderspicyber.client.Extracting;
import com.example.spacetraderspicyber.client.MarketSearch;
import com.example.spacetraderspicyber.model.Waypoint;
import com.example.spacetraderspicyber.service.WaypointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EventListenerImpl implements EventListener {

    @Autowired
    MarketSearch marketSearch;
    @Autowired
    WaypointService waypointService;

    @Override
    public void startedExtracting(String waypointSymbol) throws InterruptedException {
        Waypoint waypoint = waypointService.findByName(waypointSymbol);
        marketSearch.navigateToWaypoint(waypoint, "SPICYBER-4");
    }
}
