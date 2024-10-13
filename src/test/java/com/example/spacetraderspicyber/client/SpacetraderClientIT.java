package com.example.spacetraderspicyber.client;

import org.json.JSONException;
import org.json.JSONObject;
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
    void testSeeAgent() throws JSONException {
        String response = spacetraderClient.seeAgent();
        JSONObject agentJson = new JSONObject(spacetraderClient.seeAgent()).getJSONObject("data");
        String agentName = agentJson.getString("symbol");

        assertNotNull(response);
        assertEquals(agentName, "SPICYBER");
    }

}