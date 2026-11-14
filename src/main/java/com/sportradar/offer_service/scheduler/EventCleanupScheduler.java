package com.sportradar.offer_service.scheduler;

import com.sportradar.offer_service.service.OfferService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventCleanupScheduler {

    private final OfferService offerService;

    public EventCleanupScheduler(OfferService offerService) {
        this.offerService = offerService;
    }

    @Scheduled(fixedDelayString = "${offer.cleanup.interval:60000}")
    public void cleanupStartedEvents() {
        offerService.removeStartedEvents();
    }
}