package com.example.spacetraderspicyber.client;

import com.example.spacetraderspicyber.model.Agent;
import com.example.spacetraderspicyber.model.Agent.AgentData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest
@AutoConfigureMockMvc
class SpacetraderClientIT {

    @Autowired
    SpacetraderClient spacetraderClient;

    @Test
    void testSeeAgent() {
        Agent response = spacetraderClient.seeAgent();
        AgentData agent = spacetraderClient.seeAgent().getData();
        String agentName = agent.getSymbol();

        assertNotNull(response);
        assertEquals(agentName, "SPICYBER");
    }

}