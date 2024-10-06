package com.example.spacetraderspicyber.events;

public class ExtractingEvent {

    private String waypoint;

    public ExtractingEvent(String waypoint) {
        this.waypoint = waypoint;
    }

    public String getWaypoint() {
        return waypoint;
    }
}
