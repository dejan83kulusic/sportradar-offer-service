
package com.sportradar.offer_service.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportradar.offer_service.model.Event;
import com.sportradar.offer_service.model.Market;
import com.sportradar.offer_service.service.OfferService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final OfferService offerService;
    private final ObjectMapper objectMapper;

    public DataLoader(OfferService offerService, ObjectMapper objectMapper) {
        this.offerService = offerService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        loadMarkets();
        loadEvents();
    }

    private void loadMarkets() throws Exception {
        try (InputStream inputStream =
                     new ClassPathResource("data/markets.json").getInputStream()) {

            List<Market> markets = objectMapper.readValue(
                    inputStream,
                    new TypeReference<List<Market>>() {}
            );

            markets.forEach(offerService::saveMarket);
        }
    }

    private void loadEvents() throws Exception {
        try (InputStream inputStream =
                     new ClassPathResource("data/events.json").getInputStream()) {

            List<Event> events = objectMapper.readValue(
                    inputStream,
                    new TypeReference<List<Event>>() {}
            );

            events.forEach(offerService::saveEvent);
        }
    }
}
