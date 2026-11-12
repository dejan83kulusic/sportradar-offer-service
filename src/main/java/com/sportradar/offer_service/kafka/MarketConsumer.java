
package com.sportradar.offer_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportradar.offer_service.model.Market;
import com.sportradar.offer_service.service.OfferService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MarketConsumer {

    private final OfferService offerService;
    private final ObjectMapper objectMapper;

    public MarketConsumer(
            OfferService offerService,
            ObjectMapper objectMapper) {
        this.offerService = offerService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "market",
            groupId = "offer-service-group"
    )
    public void consume(String message) throws Exception {
        Market market = objectMapper.readValue(
                message,
                Market.class
        );

        offerService.saveMarket(market);

        System.out.println(
                "Market successfully updated: " + market.getId()
        );
    }
}
