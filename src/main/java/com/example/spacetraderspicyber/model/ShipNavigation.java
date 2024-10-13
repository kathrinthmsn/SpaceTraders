package com.example.spacetraderspicyber.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ShipNavigation {
    private Navigation data;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Navigation {
        private Fuel fuel;
        private Nav nav;
        private List<Event> events;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Fuel {
        private int current;
        private int capacity;
        private Consumed consumed;

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Consumed {
            private int amount;
            private LocalDateTime timestamp;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Nav {
        private String systemSymbol;
        private String waypointSymbol;
        private Route route;
        private String status;
        private String flightMode;

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Route {
            private Location destination;
            private Location origin;
            private LocalDateTime departureTime;
            private LocalDateTime arrival;

            @Getter
            @Setter
            @NoArgsConstructor
            public static class Location {
                private String symbol;
                private String type;
                private String systemSymbol;
                private int x;
                private int y;
            }
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Event {
        private String symbol;
        private String component;
        private String name;
        private String description;
    }

    public long calculateRouteTime() {
        Duration duration = Duration.between(data.getNav().getRoute().getDepartureTime(), data.getNav().getRoute().getArrival());
        return duration.getSeconds();
    }
}
