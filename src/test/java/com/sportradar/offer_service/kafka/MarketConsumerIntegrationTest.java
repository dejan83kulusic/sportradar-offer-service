
package com.sportradar.offer_service.kafka;

import com.sportradar.offer_service.model.Market;
import com.sportradar.offer_service.repository.InMemoryOfferRepository;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = "market",
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
class MarketConsumerIntegrationTest {

    @Autowired
    private InMemoryOfferRepository repository;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Test
    void shouldAddAndUpdateMarketFromKafkaMessage() throws Exception {

        String marketId = "integration-market-test";

        String firstMessage = """
                {
                  "id": "%s",
                  "name": "Winner",
                  "status": 1,
                  "outcomes": [
                    {
                      "id": "home",
                      "name": "Home",
                      "status": 1
                    },
                    {
                      "id": "away",
                      "name": "Away",
                      "status": 1
                    }
                  ]
                }
                """.formatted(marketId);

        String updatedMessage = """
                {
                  "id": "%s",
                  "name": "Updated Winner",
                  "status": 1,
                  "outcomes": [
                    {
                      "id": "home",
                      "name": "Home Updated",
                      "status": 1
                    }
                  ]
                }
                """.formatted(marketId);

        Properties properties = new Properties();
        properties.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
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
                    new ProducerRecord<>("market", marketId, firstMessage)
            ).get(10, TimeUnit.SECONDS);

            waitForMarket(marketId, "Winner");

            producer.send(
                    new ProducerRecord<>("market", marketId, updatedMessage)
            ).get(10, TimeUnit.SECONDS);
        }

        Market updatedMarket = waitForMarket(
                marketId,
                "Updated Winner"
        );

        assertEquals("Updated Winner", updatedMarket.getName());
        assertEquals(1, updatedMarket.getOutcomes().size());
        assertEquals(
                "Home Updated",
                updatedMarket.getOutcomes().get(0).getName()
        );

        long matchingMarkets = repository.findAllMarkets()
                .stream()
                .filter(market -> marketId.equals(market.getId()))
                .count();

        assertEquals(
                1,
                matchingMarkets,
                "Updating a market must not create a duplicate"
        );
    }

    private Market waitForMarket(
            String marketId,
            String expectedName) throws InterruptedException {

        long deadline = System.currentTimeMillis() + 10_000;

        while (System.currentTimeMillis() < deadline) {
            Market market = repository.findMarketById(marketId);

            if (market != null && expectedName.equals(market.getName())) {
                return market;
            }

            Thread.sleep(100);
        }

        fail("Market was not processed by Kafka consumer: " + expectedName);
        return null;
    }
}
