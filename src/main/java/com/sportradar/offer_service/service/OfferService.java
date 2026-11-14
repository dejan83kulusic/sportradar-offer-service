
package com.sportradar.offer_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.sportradar.offer_service.dto.EventResponse;
import com.sportradar.offer_service.dto.MarketResponse;
import com.sportradar.offer_service.dto.OfferResponse;
import com.sportradar.offer_service.dto.OutcomeResponse;

import com.sportradar.offer_service.model.Event;
import com.sportradar.offer_service.model.EventMarket;
import com.sportradar.offer_service.model.EventMarketOutcome;
import com.sportradar.offer_service.model.Market;
import com.sportradar.offer_service.model.Outcome;

import com.sportradar.offer_service.repository.InMemoryOfferRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class OfferService {

    private final InMemoryOfferRepository repository;
    private final ObjectMapper objectMapper;

    public OfferService(
            InMemoryOfferRepository repository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public List<Event> getUpcomingEvents() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        return repository.findAllEvents()
                .stream()
                .filter(event -> event.getStatus() == 1)
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

    public void mergeEvent(JsonNode update)
            throws JsonProcessingException {

        if (update == null || !update.isObject()
                || !update.hasNonNull("id")) {
            throw new IllegalArgumentException(
                    "Event message must contain a valid id"
            );
        }

        String eventId = update.get("id").asText();

        Event event = repository.findEventById(eventId);

        if (event == null) {
            event = new Event();
            event.setId(eventId);
        }

        if (update.hasNonNull("name")) {
            event.setName(update.get("name").asText());
        }

        if (update.hasNonNull("startsAt")) {
            event.setStartsAt(
                    objectMapper.treeToValue(
                            update.get("startsAt"),
                            LocalDateTime.class
                    )
            );
        }

        if (update.hasNonNull("status")) {
            event.setStatus(update.get("status").asInt());
        }

        if (update.has("markets")
                && update.get("markets").isArray()) {
            mergeMarkets(event, update.get("markets"));
        }

        repository.saveEvent(event);
    }

    private void mergeMarkets(Event event, JsonNode marketsUpdate) {
        List<EventMarket> markets = event.getMarkets() == null
                ? new ArrayList<>()
                : new ArrayList<>(event.getMarkets());

        for (JsonNode marketUpdate : marketsUpdate) {
            if (!marketUpdate.hasNonNull("id")) {
                continue;
            }

            String marketId = marketUpdate.get("id").asText();

            EventMarket market = markets.stream()
                    .filter(existing -> marketId.equals(existing.getId()))
                    .findFirst()
                    .orElse(null);

            if (market == null) {
                market = new EventMarket();
                market.setId(marketId);
                markets.add(market);
            }

            if (marketUpdate.hasNonNull("marketId")) {
                market.setMarketId(
                        marketUpdate.get("marketId").asText()
                );
            }

            if (marketUpdate.hasNonNull("status")) {
                market.setStatus(
                        marketUpdate.get("status").asInt()
                );
            }

            if (marketUpdate.has("outcomes")
                    && marketUpdate.get("outcomes").isArray()) {
                mergeOutcomes(market, marketUpdate.get("outcomes"));
            }
        }

        event.setMarkets(markets);
    }

    private void mergeOutcomes(
            EventMarket market,
            JsonNode outcomesUpdate) {

        List<EventMarketOutcome> outcomes = new ArrayList<>();

        for (JsonNode outcomeUpdate : outcomesUpdate) {
            if (!outcomeUpdate.hasNonNull("id")) {
                continue;
            }

            EventMarketOutcome outcome = new EventMarketOutcome();

            outcome.setId(outcomeUpdate.get("id").asText());

            if (outcomeUpdate.hasNonNull("outcomeId")) {
                outcome.setOutcomeId(
                        outcomeUpdate.get("outcomeId").asText()
                );
            }

            if (outcomeUpdate.hasNonNull("status")) {
                outcome.setStatus(
                        outcomeUpdate.get("status").asInt()
                );
            }

            if (outcomeUpdate.hasNonNull("odds")) {
                outcome.setOdds(
                        outcomeUpdate.get("odds").asDouble()
                );
            }

            outcomes.add(outcome);
        }

        market.setOutcomes(outcomes);
    }

    public void removeStartedEvents() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        List<String> startedEventIds = repository.findAllEvents()
                .stream()
                .filter(event -> event.getStartsAt() != null)
                .filter(event -> !event.getStartsAt().isAfter(now))
                .map(Event::getId)
                .toList();

        startedEventIds.forEach(repository::deleteEvent);
    }

    public OfferResponse getOffer() {

        List<Event> events = getUpcomingEvents();

        Map<String, Market> usedMarkets = new LinkedHashMap<>();
        Map<String, Outcome> usedOutcomes = new LinkedHashMap<>();
        Map<String, Set<String>> marketOutcomeIds = new LinkedHashMap<>();

        List<EventResponse> eventResponses = new ArrayList<>();

        for (Event event : events) {

            List<EventResponse.EventMarketResponse> marketResponses =
                    new ArrayList<>();

            if (event.getMarkets() != null) {

                for (EventMarket eventMarket : event.getMarkets()) {

                    if (eventMarket.getStatus() != 1) {
                        continue;
                    }

                    String marketId = eventMarket.getMarketId();

                    Market market = marketId == null
                            ? null
                            : repository.findMarketById(marketId);

                    if (market == null || market.getStatus() != 1) {
                        continue;
                    }

                    usedMarkets.put(market.getId(), market);

                    marketOutcomeIds.putIfAbsent(
                            market.getId(),
                            new LinkedHashSet<>()
                    );

                    List<EventResponse.EventMarketOutcomeResponse>
                            outcomeResponses = new ArrayList<>();

                    if (eventMarket.getOutcomes() != null
                            && market.getOutcomes() != null) {

                        for (EventMarketOutcome eventOutcome
                                : eventMarket.getOutcomes()) {

                            if (eventOutcome.getStatus() != 1) {
                                continue;
                            }

                            String outcomeId = eventOutcome.getOutcomeId();

                            if (outcomeId == null) {
                                continue;
                            }

                            Outcome outcome = market.getOutcomes()
                                    .stream()
                                    .filter(o -> outcomeId.equals(o.getId()))
                                    .findFirst()
                                    .orElse(null);

                            if (outcome == null || outcome.getStatus() != 1) {
                                continue;
                            }

                            usedOutcomes.putIfAbsent(
                                    outcome.getId(),
                                    outcome
                            );

                            marketOutcomeIds.get(market.getId())
                                    .add(outcome.getId());

                            outcomeResponses.add(
                                    new EventResponse.EventMarketOutcomeResponse(
                                            eventOutcome.getId(),
                                            eventOutcome.getOutcomeId(),
                                            eventOutcome.getStatus(),
                                            eventOutcome.getOdds()
                                    )
                            );
                        }
                    }

                    marketResponses.add(
                            new EventResponse.EventMarketResponse(
                                    eventMarket.getId(),
                                    eventMarket.getMarketId(),
                                    eventMarket.getStatus(),
                                    outcomeResponses
                            )
                    );
                }
            }

            eventResponses.add(
                    new EventResponse(
                            event.getId(),
                            event.getName(),
                            event.getStartsAt(),
                            event.getStatus(),
                            marketResponses
                    )
            );
        }

        List<MarketResponse> marketResponses = usedMarkets.values()
                .stream()
                .map(market -> new MarketResponse(
                        market.getId(),
                        market.getName(),
                        market.getStatus(),
                        new ArrayList<>(marketOutcomeIds.get(market.getId()))
                ))
                .toList();

        List<OutcomeResponse> outcomeResponses = usedOutcomes.values()
                .stream()
                .map(outcome -> new OutcomeResponse(
                        outcome.getId(),
                        outcome.getName(),
                        outcome.getStatus()
                ))
                .toList();

        return new OfferResponse(
                eventResponses,
                marketResponses,
                outcomeResponses
        );
    }
}
