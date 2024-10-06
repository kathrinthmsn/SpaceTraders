package com.example.spacetraderspicyber.events;

import com.example.spacetraderspicyber.model.Waypoint;
import com.example.spacetraderspicyber.service.MarketSearchService;
import com.example.spacetraderspicyber.service.WaypointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EventHandler {

    @Autowired
    MarketSearchService marketSearchService;
    @Autowired
    WaypointService waypointService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void startedExtracting(ExtractingEvent extractingEvent) throws InterruptedException {
        Waypoint waypoint = waypointService.findByName(extractingEvent.getWaypoint());
        marketSearchService.navigateToWaypoint(waypoint, "SPICYBER-4");
    }

}
