
package com.sportradar.offer_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportradar.offer_service.model.Event;
import com.sportradar.offer_service.model.EventMarketOutcome;
import com.sportradar.offer_service.repository.InMemoryOfferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OfferServiceTest {

    private InMemoryOfferRepository repository;
    private OfferService offerService;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOfferRepository();
        offerService = new OfferService(
                repository,
                new ObjectMapper()
        );
    }

    @Test
    void shouldReturnOnlyUpcomingEvents() {
        Event upcomingEvent = new Event();
        upcomingEvent.setId("1");
        upcomingEvent.setName("Real Barcelona");
        upcomingEvent.setStartsAt(LocalDateTime.now().plusDays(1));
        upcomingEvent.setStatus(1);

        Event pastEvent = new Event();
        pastEvent.setId("2");
        pastEvent.setName("BiH Nigerija");
        pastEvent.setStartsAt(LocalDateTime.now().minusDays(1));
        pastEvent.setStatus(1);

        offerService.saveEvent(upcomingEvent);
        offerService.saveEvent(pastEvent);

        List<Event> result = offerService.getUpcomingEvents();

        assertEquals(1, result.size());
        assertEquals("Real Barcelona", result.get(0).getName());
    }

    @Test
    void shouldRemoveStartedEvents() {
        Event upcomingEvent = new Event();
        upcomingEvent.setId("1");
        upcomingEvent.setStartsAt(LocalDateTime.now().plusDays(1));
        upcomingEvent.setStatus(1);

        Event startedEvent = new Event();
        startedEvent.setId("2");
        startedEvent.setStartsAt(LocalDateTime.now().minusDays(1));
        startedEvent.setStatus(1);

        offerService.saveEvent(upcomingEvent);
        offerService.saveEvent(startedEvent);

        offerService.removeStartedEvents();

        assertNotNull(repository.findEventById("1"));
        assertNull(repository.findEventById("2"));
    }

    @Test
    void shouldReplaceOldOutcomesWhenNewEventUpdateArrives() throws Exception {
        String initialEventJson = """
                {
                  "id": "event-1",
                  "name": "Real Barcelona",
                  "status": 1,
                  "markets": [
                    {
                      "id": "event-market-1",
                      "marketId": "market-1",
                      "status": 1,
                      "outcomes": [
                        {
                          "id": "outcome-1",
                          "outcomeId": "home",
                          "status": 1,
                          "odds": 1.5
                        },
                        {
                          "id": "outcome-2",
                          "outcomeId": "draw",
                          "status": 1,
                          "odds": 3.0
                        },
                        {
                          "id": "outcome-3",
                          "outcomeId": "away",
                          "status": 1,
                          "odds": 2.5
                        }
                      ]
                    }
                  ]
                }
                """;

        String updatedEventJson = """
                {
                  "id": "event-1",
                  "markets": [
                    {
                      "id": "event-market-1",
                      "outcomes": [
                        {
                          "id": "outcome-1",
                          "outcomeId": "home",
                          "status": 1,
                          "odds": 1.7
                        },
                        {
                          "id": "outcome-3",
                          "outcomeId": "away",
                          "status": 1,
                          "odds": 2.2
                        }
                      ]
                    }
                  ]
                }
                """;

        offerService.mergeEvent(
                new ObjectMapper().readTree(initialEventJson)
        );

        offerService.mergeEvent(
                new ObjectMapper().readTree(updatedEventJson)
        );

        Event event = repository.findEventById("event-1");

        assertNotNull(event);
        assertEquals(1, event.getMarkets().size());

        List<EventMarketOutcome> outcomes =
                event.getMarkets().get(0).getOutcomes();

        assertEquals(2, outcomes.size());

        assertTrue(
                outcomes.stream()
                        .anyMatch(o -> o.getId().equals("outcome-1"))
        );

        assertTrue(
                outcomes.stream()
                        .anyMatch(o -> o.getId().equals("outcome-3"))
        );

        assertFalse(
                outcomes.stream()
                        .anyMatch(o -> o.getId().equals("outcome-2"))
        );

        assertEquals(
                1.7,
                outcomes.stream()
                        .filter(o -> o.getId().equals("outcome-1"))
                        .findFirst()
                        .orElseThrow()
                        .getOdds()
        );

        assertEquals(
                2.2,
                outcomes.stream()
                        .filter(o -> o.getId().equals("outcome-3"))
                        .findFirst()
                        .orElseThrow()
                        .getOdds()
        );
    }
}
