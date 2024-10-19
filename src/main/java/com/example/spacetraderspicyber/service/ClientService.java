package com.example.spacetraderspicyber.service;

import com.example.spacetraderspicyber.client.SpacetraderClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
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

    public void makeMoney() {

        //TODO: register agent each 2 weeks
        marketSearchService.saveMarketDataToDb();
        scanningService.scanAllWaypoint();
        contractsService.checkContractValidity();


        Thread ship1Thread = new Thread(() -> {
            while (true) {
                try{
                    tradingService.buyItemForContract("SPICYBER-1");
                } catch (InterruptedException e) {
                    log.error("Error with ship 1.");
                }
            }
        });

        Thread ship2Thread = new Thread(() -> {
            while (true) {
                try {
                    marketSearchService.marketResearch("SPICYBER-2");
                } catch (InterruptedException e) {
                    log.error("Error with ship 2.");
                }
            }
        });

        ship1Thread.start();
        ship2Thread.start();
    }
}
