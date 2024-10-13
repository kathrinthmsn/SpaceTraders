package com.example.spacetraderspicyber.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table
@Getter
@Setter
public class Waypoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String systemSymbol;

    @Column(unique = true)
    private String symbol;

    @Column
    private String type;

    @Column
    private int x;

    @Column
    private int y;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Trait> traits;

    @Column
    private boolean isUnderConstruction;

    @ElementCollection
    private List<Orbital> orbitals;
    private String orbits;
    private Faction faction;
    @ElementCollection
    private List<Modifier> modifiers;
    private Chart chart;

    @Getter
    @Setter
    @NoArgsConstructor
    @Embeddable
    public static class Orbital {
        @Column(name = "orbital_symbol")
        private String symbol;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Embeddable
    public static class Faction {
        @Column(name = "faction_symbol")
        private String symbol;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Embeddable
    public static class Modifier {
        @Column(name = "modifier_symbol")
        private String symbol;
        private String name;
        private String description;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Embeddable
    public static class Chart {
        private String waypointSymbol;
        private String submittedBy;
        private String submittedOn;
    }
}
