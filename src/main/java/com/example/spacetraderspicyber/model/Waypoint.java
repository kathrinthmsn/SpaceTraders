package com.example.spacetraderspicyber.model;

import jakarta.persistence.*;
import lombok.Getter;
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
}
