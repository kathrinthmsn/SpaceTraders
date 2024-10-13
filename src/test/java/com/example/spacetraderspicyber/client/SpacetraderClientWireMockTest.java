package com.example.spacetraderspicyber.client;

import com.example.spacetraderspicyber.model.Contracts;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"spacetrader.api.base-url=http://localhost:8081"})
class SpacetraderClientWireMockTest {

    public WireMockServer wireMockServer;

    @Autowired
    private SpacetraderClient spacetraderClient;

    @BeforeEach
    public void setUp() {
        wireMockServer = new WireMockServer(8081);
        wireMockServer.start();
        configureFor("127.0.0.1", wireMockServer.port());
    }

    @AfterEach
    public void tearDown() {
        wireMockServer.stop();
    }


    @Test
    void testGetContracts() {
        stubFor(get(urlPathMatching("/v2/my/contracts"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":[{\"id\":\"1\",\"factionSymbol\":\"COSMIC\",\"type\":\"PROCUREMENT\",\"terms\":{\"deadline\":\"someDeadline\",\"payment\":{\"onAccepted\":100,\"onFulfilled\":200},\"deliver\":[]},\"accepted\":false,\"fulfilled\":false,\"expiration\":\"someExpiration\",\"deadlineToAccept\":\"someDeadlineToAccept\"}]}")));

        Contracts contracts = spacetraderClient.getContracts(1);

        assertNotNull(contracts.getData());
        assertEquals("COSMIC", contracts.getData().get(0).getFactionSymbol());
        assertEquals("PROCUREMENT", contracts.getData().get(0).getType());
        assertEquals("1", contracts.getData().get(0).getId());
        assertFalse(contracts.getData().get(0).isAccepted());
        assertFalse(contracts.getData().get(0).isFulfilled());
        assertNotNull(contracts.getData().get(0).getTerms());
        assertEquals(100, contracts.getData().get(0).getTerms().getPayment().getOnAccepted());
        assertEquals(200, contracts.getData().get(0).getTerms().getPayment().getOnFulfilled());
    }

}