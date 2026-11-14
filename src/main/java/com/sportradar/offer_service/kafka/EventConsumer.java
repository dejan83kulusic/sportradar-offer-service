

package com.sportradar.offer_service.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportradar.offer_service.service.OfferService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventConsumer {

    private final OfferService offerService;
    private final ObjectMapper objectMapper;

    public EventConsumer(
            OfferService offerService,
            ObjectMapper objectMapper) {
        this.offerService = offerService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "event",
            groupId = "offer-service-group"
    )
    public void consume(String message) throws Exception {

        JsonNode eventUpdate = objectMapper.readTree(message);

        offerService.mergeEvent(eventUpdate);

        System.out.println(
                "Event successfully processed: "
                        + eventUpdate.get("id").asText()
        );
    }
}
