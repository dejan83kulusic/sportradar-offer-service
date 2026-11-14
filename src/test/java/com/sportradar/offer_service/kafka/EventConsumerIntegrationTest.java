
package com.sportradar.offer_service.kafka;

import com.sportradar.offer_service.model.Event;
import com.sportradar.offer_service.repository.InMemoryOfferRepository;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EventConsumerIntegrationTest {

    @Autowired
    private InMemoryOfferRepository repository;

    @Test
    void shouldConsumeEventMessageFromKafka() throws Exception {

        String eventId = "integration-test-" + UUID.randomUUID();

        String message = """
                {
                  "id": "%s",
                  "name": "Real Barcelona",
                  "startsAt": "%s",
                  "status": 1,
                  "markets": [
                    {
                      "id": "event-market-1",
                      "marketId": "market-1",
                      "status": 1,
                      "outcomes": [
                        {
                          "id": "outcome-1",
                          "outcomeId": "home",
                          "status": 1,
                          "odds": 1.5
                        },
                        {
                          "id": "outcome-2",
                          "outcomeId": "away",
                          "status": 1,
                          "odds": 2.5
                        }
                      ]
                    }
                  ]
                }
                """.formatted(
                eventId,
                LocalDateTime.now().plusDays(2)
        );

        Properties properties = new Properties();
        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "localhost:9092"
        );
        properties.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName()
        );
        properties.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName()
        );

        try (KafkaProducer<String, String> producer =
                     new KafkaProducer<>(properties)) {

            producer.send(
                    new ProducerRecord<>("event", eventId, message)
            ).get(10, TimeUnit.SECONDS);
        }

        Event consumedEvent = null;

        long deadline = System.currentTimeMillis() + 10_000;

        while (System.currentTimeMillis() < deadline) {

            consumedEvent = repository.findEventById(eventId);

            if (consumedEvent != null) {
                break;
            }

            Thread.sleep(100);
        }

        assertNotNull(
                consumedEvent,
                "EventConsumer should process the Kafka message"
        );

        assertEquals(eventId, consumedEvent.getId());
        assertEquals("Real Barcelona", consumedEvent.getName());
        assertEquals(1, consumedEvent.getStatus());

        assertNotNull(consumedEvent.getMarkets());
        assertEquals(1, consumedEvent.getMarkets().size());

        assertEquals(
                2,
                consumedEvent.getMarkets().get(0).getOutcomes().size()
        );
    }
}
