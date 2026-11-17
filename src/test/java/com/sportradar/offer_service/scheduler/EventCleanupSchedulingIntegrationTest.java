package com.sportradar.offer_service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportradar.offer_service.model.Event;
import com.sportradar.offer_service.repository.InMemoryOfferRepository;
import com.sportradar.offer_service.service.OfferService;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class EventCleanupSchedulingIntegrationTest {

    @Test
    void shouldAutomaticallyRemoveStartedEvents() throws InterruptedException {

        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext()) {

            context.getEnvironment()
                    .getPropertySources()
                    .addFirst(new MapPropertySource(
                            "testProperties",
                            Map.of("offer.cleanup.interval", "100")
                    ));

            context.register(TestConfiguration.class);
            context.refresh();

            InMemoryOfferRepository repository =
                    context.getBean(InMemoryOfferRepository.class);

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

            repository.saveEvent(startedEvent);
            repository.saveEvent(upcomingEvent);

            // Wait for Spring's scheduler to execute automatically.
            long deadline = System.nanoTime()
                    + TimeUnit.SECONDS.toNanos(3);

            while (repository.findEventById("started-event") != null
                    && System.nanoTime() < deadline) {
                Thread.sleep(20);
            }

            assertNull(repository.findEventById("started-event"),
                    "Started event should be removed automatically");

            assertNotNull(repository.findEventById("upcoming-event"),
                    "Upcoming event should remain in the repository");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    static class TestConfiguration {

        @Bean
        InMemoryOfferRepository inMemoryOfferRepository() {
            return new InMemoryOfferRepository();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        OfferService offerService(
                InMemoryOfferRepository repository,
                ObjectMapper objectMapper) {
            return new OfferService(repository, objectMapper);
        }

        @Bean
        EventCleanupScheduler eventCleanupScheduler(
                OfferService offerService) {
            return new EventCleanupScheduler(offerService);
        }
    }
}