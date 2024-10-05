package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.model.Waypoint;
import com.example.spacetraderspicyber.repositories.WaypointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WaypointService {
    private final WaypointRepository waypointRepository;

    @Autowired
    public WaypointService(WaypointRepository waypointRepository) {
        this.waypointRepository = waypointRepository;
    }

    public void saveWaypoints(Waypoint waypoint) {
        waypointRepository.save(waypoint);
    }
    public Waypoint findByName(String symbol){
        return waypointRepository.findBySymbol(symbol);
    }
    public List<Waypoint> findAll(){
        return waypointRepository.findAll();
    }
    public List<Waypoint> findByType(String type){
        return waypointRepository.findByTraits_Symbol(type);
    }
}
