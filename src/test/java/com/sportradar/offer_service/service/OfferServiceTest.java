
package com.sportradar.offer_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportradar.offer_service.dto.OfferResponse;
import com.sportradar.offer_service.model.Event;
import com.sportradar.offer_service.model.EventMarket;
import com.sportradar.offer_service.model.EventMarketOutcome;
import com.sportradar.offer_service.model.Market;
import com.sportradar.offer_service.model.Outcome;
import com.sportradar.offer_service.repository.InMemoryOfferRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        upcomingEvent.setStartsAt(
                LocalDateTime.now(ZoneOffset.UTC).plusDays(1)
        );
        upcomingEvent.setStatus(1);

        Event startedEvent = new Event();
        startedEvent.setId("2");
        startedEvent.setStartsAt(
                LocalDateTime.now(ZoneOffset.UTC).minusDays(1)
        );
        startedEvent.setStatus(1);

        Event inactiveEvent = new Event();
        inactiveEvent.setId("3");
        inactiveEvent.setStartsAt(
                LocalDateTime.now(ZoneOffset.UTC).plusDays(1)
        );
        inactiveEvent.setStatus(0);

        Event eventWithoutStartDate = new Event();
        eventWithoutStartDate.setId("4");
        eventWithoutStartDate.setStartsAt(null);
        eventWithoutStartDate.setStatus(1);

        offerService.saveEvent(upcomingEvent);
        offerService.saveEvent(startedEvent);
        offerService.saveEvent(inactiveEvent);
        offerService.saveEvent(eventWithoutStartDate);

        List<Event> result = offerService.getUpcomingEvents();

        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getId());
    }

    @Test
    void shouldRemoveStartedEvents() {
        Event startedEvent = new Event();
        startedEvent.setId("1");
        startedEvent.setStartsAt(
                LocalDateTime.now(ZoneOffset.UTC).minusMinutes(10)
        );
        startedEvent.setStatus(1);

        Event upcomingEvent = new Event();
        upcomingEvent.setId("2");
        upcomingEvent.setStartsAt(
                LocalDateTime.now(ZoneOffset.UTC).plusDays(1)
        );
        upcomingEvent.setStatus(1);

        Event eventWithoutStartDate = new Event();
        eventWithoutStartDate.setId("3");
        eventWithoutStartDate.setStartsAt(null);
        eventWithoutStartDate.setStatus(1);

        offerService.saveEvent(startedEvent);
        offerService.saveEvent(upcomingEvent);
        offerService.saveEvent(eventWithoutStartDate);

        offerService.removeStartedEvents();

        assertEquals(2, repository.findAllEvents().size());
        assertTrue(repository.findEventById("1") == null);
        assertFalse(repository.findEventById("2") == null);
        assertFalse(repository.findEventById("3") == null);
    }

    @Test
    void shouldReplaceOldOutcomesWhenNewEventUpdateArrives() throws Exception {
        Event event = new Event();
        event.setId("1");

        EventMarket market = new EventMarket();
        market.setId("market-1");

        EventMarketOutcome oldOutcome = new EventMarketOutcome();
        oldOutcome.setId("old-outcome");
        oldOutcome.setOutcomeId("outcome-1");

        market.setOutcomes(List.of(oldOutcome));
        event.setMarkets(List.of(market));

        offerService.saveEvent(event);

        String update = """
                {
                  "id": "1",
                  "markets": [
                    {
                      "id": "market-1",
                      "outcomes": [
                        {
                          "id": "new-outcome",
                          "outcomeId": "outcome-2",
                          "status": 1,
                          "odds": 2.5
                        }
                      ]
                    }
                  ]
                }
                """;

        offerService.mergeEvent(
                new ObjectMapper().readTree(update)
        );

        Event updatedEvent = repository.findEventById("1");

        assertEquals(
                1,
                updatedEvent.getMarkets().get(0).getOutcomes().size()
        );

        assertEquals(
                "new-outcome",
                updatedEvent.getMarkets().get(0).getOutcomes().get(0).getId()
        );
    }

    @Test
    void shouldReturnOnlyUsedActiveMarketsAndOutcomesWithoutDuplicates() {
        Event event1 = createEvent("event-1", 1);
        Event event2 = createEvent("event-2", 1);
        Event inactiveEvent = createEvent("event-3", 0);

        Market market1 = createMarket("market-1", 1);
        Market inactiveMarket = createMarket("market-2", 0);
        Market unusedMarket = createMarket("market-3", 1);

        Outcome activeOutcome = createOutcome("outcome-1", 1);
        Outcome inactiveOutcome = createOutcome("outcome-2", 0);
        Outcome unusedOutcome = createOutcome("outcome-3", 1);

        market1.setOutcomes(List.of(activeOutcome, inactiveOutcome));
        inactiveMarket.setOutcomes(List.of(activeOutcome));

        repository.saveMarket(market1);
        repository.saveMarket(inactiveMarket);
        repository.saveMarket(unusedMarket);

        event1.setMarkets(List.of(
                createEventMarket("event-market-1", "market-1",
                        createEventOutcome("ref-1", "outcome-1", 1),
                        createEventOutcome("ref-2", "outcome-2", 1)),
                createEventMarket("event-market-2", "market-2",
                        createEventOutcome("ref-3", "outcome-1", 1))
        ));

        event2.setMarkets(List.of(
                createEventMarket("event-market-3", "market-1",
                        createEventOutcome("ref-4", "outcome-1", 1))
        ));

        inactiveEvent.setMarkets(List.of(
                createEventMarket("event-market-4", "market-1",
                        createEventOutcome("ref-5", "outcome-1", 1))
        ));

        offerService.saveEvent(event1);
        offerService.saveEvent(event2);
        offerService.saveEvent(inactiveEvent);

        repository.saveMarket(unusedMarket);
        assertEquals(1, unusedOutcome.getStatus());

        OfferResponse response = offerService.getOffer();

        assertEquals(2, response.events().size());
        assertEquals(1, response.markets().size());
        assertEquals("market-1", response.markets().get(0).id());

        assertEquals(1, response.outcomes().size());
        assertEquals("outcome-1", response.outcomes().get(0).id());
    }

    @Test
    void shouldFilterUpcomingEventsByStartDate() {
        LocalDate targetDate = LocalDate.now(ZoneOffset.UTC).plusDays(2);

        Event matchingEvent = new Event();
        matchingEvent.setId("1");
        matchingEvent.setName("Matching event");
        matchingEvent.setStartsAt(targetDate.atTime(18, 0));
        matchingEvent.setStatus(1);

        Event otherDateEvent = new Event();
        otherDateEvent.setId("2");
        otherDateEvent.setName("Other date event");
        otherDateEvent.setStartsAt(
                targetDate.plusDays(1).atTime(18, 0)
        );
        otherDateEvent.setStatus(1);

        Event inactiveEvent = new Event();
        inactiveEvent.setId("3");
        inactiveEvent.setName("Inactive event");
        inactiveEvent.setStartsAt(targetDate.atTime(20, 0));
        inactiveEvent.setStatus(0);

        offerService.saveEvent(matchingEvent);
        offerService.saveEvent(otherDateEvent);
        offerService.saveEvent(inactiveEvent);

        List<Event> result = offerService.getUpcomingEvents(targetDate);

        assertEquals(1, result.size());
        assertEquals("Matching event", result.get(0).getName());
    }

    @Test
    void shouldReturnAllUpcomingEventsWhenStartDateIsNull() {
        Event firstEvent = new Event();
        firstEvent.setId("1");
        firstEvent.setStartsAt(
                LocalDateTime.now(ZoneOffset.UTC).plusDays(1)
        );
        firstEvent.setStatus(1);

        Event secondEvent = new Event();
        secondEvent.setId("2");
        secondEvent.setStartsAt(
                LocalDateTime.now(ZoneOffset.UTC).plusDays(2)
        );
        secondEvent.setStatus(1);

        offerService.saveEvent(firstEvent);
        offerService.saveEvent(secondEvent);

        List<Event> result = offerService.getUpcomingEvents(null);

        assertEquals(2, result.size());
    }

    private Event createEvent(String id, int status) {
        Event event = new Event();
        event.setId(id);
        event.setName(id);
        event.setStartsAt(
                LocalDateTime.now(ZoneOffset.UTC).plusDays(2)
        );
        event.setStatus(status);
        return event;
    }

    private Market createMarket(String id, int status) {
        Market market = new Market();
        market.setId(id);
        market.setName(id);
        market.setStatus(status);
        return market;
    }

    private Outcome createOutcome(String id, int status) {
        Outcome outcome = new Outcome();
        outcome.setId(id);
        outcome.setName(id);
        outcome.setStatus(status);
        return outcome;
    }

    private EventMarket createEventMarket(
            String id,
            String marketId,
            EventMarketOutcome... outcomes) {

        EventMarket market = new EventMarket();
        market.setId(id);
        market.setMarketId(marketId);
        market.setStatus(1);
        market.setOutcomes(List.of(outcomes));
        return market;
    }

    private EventMarketOutcome createEventOutcome(
            String id,
            String outcomeId,
            int status) {

        EventMarketOutcome outcome = new EventMarketOutcome();
        outcome.setId(id);
        outcome.setOutcomeId(outcomeId);
        outcome.setStatus(status);
        outcome.setOdds(2.0);
        return outcome;
    }
}
