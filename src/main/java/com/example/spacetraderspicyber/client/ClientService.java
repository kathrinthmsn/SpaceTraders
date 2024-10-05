package com.example.spacetraderspicyber.client;

import com.example.spacetraderspicyber.client.*;
import com.example.spacetraderspicyber.model.Good;
import com.example.spacetraderspicyber.service.MarketService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.lang.Thread.sleep;

@Service
public class ClientService {

    @Autowired
    private Contracts contracts;
    @Autowired
    private MarketSearch marketSearch;
    @Autowired
    private Trading trading;
    @Autowired
    private TradeRoute tradeRoute;

    public void makeMoney() throws InterruptedException {

        //TODO: register agent each 2 weeks
      //  marketSearch.mapMarketData();
     //   scanning.scanAllWaypoint();
        contracts.checkContractValidity();


        Thread ship1Thread = new Thread(() -> {
            while (true) {
                try{
                    trading.buyItemForContract("SPICYBER-1");

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread ship2Thread = new Thread(() -> {
            while (true) {
                try {
                    marketSearch.marketResearch("SPICYBER-2");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread ship3Thread = new Thread(() -> {
            while (true) {
                try {
                    marketSearch.marketResearch("SPICYBER-3");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread ship6Thread = new Thread(() -> {
            while (true) {
                try {
                    tradeRoute.tradeRoute("SPICYBER-6");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        //Contracts
        ship1Thread.start();

        //Survey Markets
        ship2Thread.start();
        ship3Thread.start();
        //TradeRoute
        ship6Thread.start();
    }
}
