package com.example.spacetraderspicyber.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class Cargo {

    private int capacity;
    private int units;
    private List<Inventory> inventory;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Inventory {
        private String symbol;
        private String name;
        private String description;
        private int units;
    }
}
