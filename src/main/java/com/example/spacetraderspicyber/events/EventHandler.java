package com.example.spacetraderspicyber.events;

import com.example.spacetraderspicyber.service.WaypointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EventHandler {

    @Autowired
    WaypointService waypointService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void startedExtracting(ExtractingEvent extractingEvent) throws InterruptedException {
        waypointService.goToWaypoint(extractingEvent.getWaypoint(), "SPICYBER-4");
    }

}
