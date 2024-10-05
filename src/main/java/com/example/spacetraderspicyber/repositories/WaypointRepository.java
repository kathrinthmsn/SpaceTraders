package com.example.spacetraderspicyber.repositories;

import com.example.spacetraderspicyber.model.Market;
import com.example.spacetraderspicyber.model.Waypoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WaypointRepository  extends JpaRepository<Waypoint, Long> {

    Waypoint findBySymbol(String symbol);

    List<Waypoint> findByTraits_Symbol(String type);
}
