package com.example.spacetraderspicyber.model;

import com.example.spacetraderspicyber.model.Ship.ShipData.Frame.Requirements;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Ship {

    private ShipData data;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ShipData {
        private String symbol;
        private Registration registration;
        private Nav nav;
        private Crew crew;
        private Frame frame;
        private Reactor reactor;
        private Engine engine;
        private Cooldown cooldown;
        private List<Module> modules;
        private List<Mount> mounts;
        private Cargo cargo;
        private Fuel fuel;

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Registration {
            private String name;
            private String factionSymbol;
            private String role;
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
                private Destination destination;
                private Origin origin;
                private String departureTime;
                private String arrival;

                @Getter
                @Setter
                @NoArgsConstructor
                public static class Destination {
                    private String symbol;
                    private String type;
                    private String systemSymbol;
                    private int x;
                    private int y;
                }

                @Getter
                @Setter
                @NoArgsConstructor
                public static class Origin {
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
        public static class Crew {
            private int current;
            private int required;
            private int capacity;
            private String rotation;
            private int morale;
            private int wages;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Frame {
            private String symbol;
            private String name;
            private String description;
            private int condition;
            private int integrity;
            private int moduleSlots;
            private int mountingPoints;
            private int fuelCapacity;
            private Requirements requirements;

            @Getter
            @Setter
            @NoArgsConstructor
            public static class Requirements {
                private int power;
                private int crew;
                private int slots;
            }
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Reactor {
            private String symbol;
            private String name;
            private String description;
            private int condition;
            private int integrity;
            private int powerOutput;
            private Requirements requirements;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Engine {
            private String symbol;
            private String name;
            private String description;
            private int condition;
            private int integrity;
            private int speed;
            private Requirements requirements;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Cooldown {
            private String shipSymbol;
            private int totalSeconds;
            private int remainingSeconds;
            private String expiration;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Module {
            private String symbol;
            private int capacity;
            private int range;
            private String name;
            private String description;
            private Requirements requirements;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class Mount {
            private String symbol;
            private String name;
            private String description;
            private int strength;
            private List<String> deposits;
            private Requirements requirements;
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
                private String timestamp;
            }
        }

        public String getCurrentLocation() {
            return getNav().getWaypointSymbol();
        }

        public double calculateDistanceToCurrentLocation(int destinationX, int destinationY) {
            return Math.sqrt(Math.pow(nav.route.destination.x - nav.route.destination.y, 2) + Math.pow(destinationX - destinationY, 2));
        }

        public boolean shipNotFullyFueled() {
            return getFuel().getCurrent() != getFuel().getCapacity();
        }

        public int getRemainingFuelCapacity() {
            return getFuel().getCapacity() - getFuel().getCurrent();
        }

        public int getRemainingCargoCapacity() {
            return getCargo().getCapacity() - getCargo().getUnits();
        }

        public boolean cargoFull() {
            return getCargo().getCapacity() == getCargo().getUnits();
        }
    }
}
