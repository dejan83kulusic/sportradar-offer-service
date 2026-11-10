
package com.sportradar.offer_service.service;

import com.sportradar.offer_service.model.Event;
import com.sportradar.offer_service.model.Market;
import com.sportradar.offer_service.repository.InMemoryOfferRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
public class OfferService {

    private final InMemoryOfferRepository repository;

    public OfferService(InMemoryOfferRepository repository) {
        this.repository = repository;
    }

    public List<Event> getUpcomingEvents() {
        LocalDateTime now = LocalDateTime.now();

        return repository.findAllEvents()
                .stream()
                .filter(event -> event.getStartsAt() != null)
                .filter(event -> event.getStartsAt().isAfter(now))
                .toList();
    }

    public Collection<Market> getAllMarkets() {
        return repository.findAllMarkets();
    }

    public void saveEvent(Event event) {
        repository.saveEvent(event);
    }

    public void saveMarket(Market market) {
        repository.saveMarket(market);
    }

    public void removeStartedEvents() {
        LocalDateTime now = LocalDateTime.now();

        repository.findAllEvents().stream()
                .filter(event -> event.getStartsAt() != null)
                .filter(event -> !event.getStartsAt().isAfter(now))
                .map(Event::getId)
                .toList()
                .forEach(repository::deleteEvent);
    }
}
