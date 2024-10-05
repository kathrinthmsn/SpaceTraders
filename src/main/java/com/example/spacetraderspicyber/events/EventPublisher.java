package com.example.spacetraderspicyber.events;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EventPublisher {
    private List<EventListener> listeners = new ArrayList<>();

    public void addListener(EventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(EventListener listener) {
        listeners.remove(listener);
    }

    public void publishEvent(String waypointSymbol) throws InterruptedException {
        for (EventListener listener : listeners) {
            listener.startedExtracting(waypointSymbol);
        }
    }
}
