package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class ClientService {

    @Autowired
    private ContractsService contractsService;
    @Autowired
    private MarketSearchService marketSearchService;
    @Autowired
    private TradingService tradingService;
    @Autowired
    private SpacetraderClient spacetraderClient;
    @Autowired
    private ScanningService scanningService;

    @EventListener
    public void onAppReady(ApplicationReadyEvent applicationReadyEvent) throws InterruptedException {
        makeMoney();
    }

    public void makeMoney() throws InterruptedException {

        //TODO: register agent each 2 weeks
        marketSearchService.mapMarketData();
        scanningService.scanAllWaypoint();
        contractsService.checkContractValidity();


        Thread ship1Thread = new Thread(() -> {
            while (true) {
                try{
                    tradingService.buyItemForContract("SPICYBER-1");

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        //Contracts
        ship1Thread.start();
    }
}
