package com.sportradar.offer_service.service;

import com.sportradar.offer_service.model.Event;
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
        offerService = new OfferService(repository);
    }

    @Test
    void shouldReturnOnlyUpcomingEvents() {
        Event upcomingEvent = new Event();
        upcomingEvent.setId("1");
        upcomingEvent.setName("Real Barcelona");
        upcomingEvent.setStartsAt(LocalDateTime.now().plusDays(1));

        Event pastEvent = new Event();
        pastEvent.setId("2");
        pastEvent.setName("BiH Nigerija");
        pastEvent.setStartsAt(LocalDateTime.now().minusDays(1));

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

        Event startedEvent = new Event();
        startedEvent.setId("2");
        startedEvent.setStartsAt(LocalDateTime.now().minusDays(1));

        offerService.saveEvent(upcomingEvent);
        offerService.saveEvent(startedEvent);

        offerService.removeStartedEvents();

        assertNotNull(repository.findEventById("1"));
        assertNull(repository.findEventById("2"));
    }
}
