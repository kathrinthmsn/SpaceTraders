package com.example.spacetraderspicyber.controller;

import com.example.spacetraderspicyber.model.Market;
import com.example.spacetraderspicyber.service.MarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "api/v1")
public class Controller {

    private final MarketService marketService;

    @Autowired
    public Controller(MarketService marketService){
        this.marketService = marketService;
    }

    @GetMapping
    public String hello(){
        return "Hello";
    }

    @GetMapping(path = "markets")
    public List<Market> getAllMarkets(){
        return marketService.getAllMarkets();
    }
}
