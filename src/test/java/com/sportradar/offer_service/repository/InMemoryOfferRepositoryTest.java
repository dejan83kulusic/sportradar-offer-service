
package com.sportradar.offer_service.repository;

import com.sportradar.offer_service.model.Event;
import com.sportradar.offer_service.model.Market;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryOfferRepositoryTest {

    private InMemoryOfferRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOfferRepository();
    }

    @Test
    void shouldSaveAndFindEvent() {
        Event event = new Event();
        event.setId("1");
        event.setName("Real Barcelona");

        repository.saveEvent(event);

        Event result = repository.findEventById("1");

        assertNotNull(result);
        assertEquals("Real Barcelona", result.getName());
    }

    @Test
    void shouldDeleteEvent() {
        Event event = new Event();
        event.setId("1");

        repository.saveEvent(event);
        repository.deleteEvent("1");

        assertNull(repository.findEventById("1"));
    }

    @Test
    void shouldSaveAndFindMarket() {
        Market market = new Market();
        market.setId("3way");
        market.setName("3 way");

        repository.saveMarket(market);

        Market result = repository.findMarketById("3way");

        assertNotNull(result);
        assertEquals("3 way", result.getName());
    }
}
