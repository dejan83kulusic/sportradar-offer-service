package com.sportradar.offer_service.repository;

import com.sportradar.offer_service.model.Event;
import com.sportradar.offer_service.model.Market;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryOfferRepository {

    private final Map<String, Event> events = new ConcurrentHashMap<>();
    private final Map<String, Market> markets = new ConcurrentHashMap<>();

    public void saveEvent(Event event) {
        events.put(event.getId(), event);
    }

    public void saveMarket(Market market) {
        markets.put(market.getId(), market);
    }

    public Collection<Event> findAllEvents() {
        return events.values();
    }

    public Collection<Market> findAllMarkets() {
        return markets.values();
    }

    public Event findEventById(String id) {
        return events.get(id);
    }

    public Market findMarketById(String id) {
        return markets.get(id);
    }

    public void deleteEvent(String id) {
        events.remove(id);
    }
}