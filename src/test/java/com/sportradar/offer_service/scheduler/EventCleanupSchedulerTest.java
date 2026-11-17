
package com.sportradar.offer_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportradar.offer_service.model.Event;
import com.sportradar.offer_service.repository.InMemoryOfferRepository;
import com.sportradar.offer_service.service.OfferService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class EventCleanupSchedulerTest {

    private InMemoryOfferRepository repository;
    private EventCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        repository = new InMemoryOfferRepository();

        OfferService offerService = new OfferService(
                repository,
                new ObjectMapper()
        );

        scheduler = new EventCleanupScheduler(offerService);
    }

    @Test
    void shouldAutomaticallyRemoveStartedEventsWhenSchedulerRuns() {

        Event startedEvent = new Event();
        startedEvent.setId("started-event");
        startedEvent.setStartsAt(
                LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5)
        );
        startedEvent.setStatus(1);

        Event upcomingEvent = new Event();
        upcomingEvent.setId("upcoming-event");
        upcomingEvent.setStartsAt(
                LocalDateTime.now(ZoneOffset.UTC).plusDays(1)
        );
        upcomingEvent.setStatus(1);

        Event eventWithoutStartDate = new Event();
        eventWithoutStartDate.setId("event-without-date");
        eventWithoutStartDate.setStartsAt(null);
        eventWithoutStartDate.setStatus(1);

        repository.saveEvent(startedEvent);
        repository.saveEvent(upcomingEvent);
        repository.saveEvent(eventWithoutStartDate);

        // Execute the actual scheduler method.
        scheduler.cleanupStartedEvents();

        assertNull(repository.findEventById("started-event"));
        assertNotNull(repository.findEventById("upcoming-event"));
        assertNotNull(repository.findEventById("event-without-date"));

        assertEquals(2, repository.findAllEvents().size());
    }
}
