package com.example.spacetraderspicyber.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static java.lang.Thread.sleep;


@Component
public class Start {
    @Autowired
    private SpacetraderClient spacetraderClient;
    @Autowired
    private ClientService clientService;


    @EventListener
    public void onAppReady(ApplicationReadyEvent applicationReadyEvent) throws InterruptedException {

        clientService.makeMoney();

    }




}
