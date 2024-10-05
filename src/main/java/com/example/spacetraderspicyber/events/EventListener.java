package com.example.spacetraderspicyber.events;

public interface EventListener {
    void startedExtracting(String waypointSymbol) throws InterruptedException;
}
