package intg.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import producer.dto.LibraryEvent;
import util.TestUtil;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/*

// Configure embeddedKafkaBroker, so our test case is going to interact with the
// embedded Kafka broker instead of the actual broker that's running in our local.

@EmbeddedKafka(topics = {"library-events"}, partitions = 3)
    So we are providing the Kafka topic named "library-events" to the embedded Kafka.
    So this is going to create that topic into our virtual embedded Kafka broker.


// Then, Override the kafka producer bootstrap address to the embedded broker ones.
// the kafka producer bootstrap address from our application.yml file.

@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
                                  "spring.kafka.admin.properties.bootstrap.servers=${spring.embedded.kafka.brokers}"})
    Here we are providing TestPropertySource annotation to override application.yml properties dynamically from our test case.
    So this property ${spring.embedded.kafka.brokers} is something which is automatically set into the application context,
    So when you add this annotation, this annotation is going to automatically inject these values into the test case itself.
    So it's NOT going to use the following properties from our application.yml file :-

    kafka:
        topic:  library-events
        producer:
            bootstrap-servers: localhost:9092,localhost:9093,localhost:9094

Note:
    Now I would like to bring the local cluster down by running the following command in the terminal :-
    "docker-compose -f docker-compose-multi-broker.yml down"
    So in this case, we don't want the local cluster to be up and running to make sure this test case still
    succeeds after adding (@EmbeddedKafka / @TestPropertySource) without the local cluster.
*/

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = {"library-events"}, partitions = 3)
@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
                                   "spring.kafka.admin.properties.bootstrap.servers=${spring.embedded.kafka.brokers}"})
class LibraryEventsControllerIntegrationTest {

    // make calls to the LibraryEventsController endpoints using RestTemplate.
    @Autowired
    TestRestTemplate restTemplate;

    // Configure a Kafka consumer in the test case
    // Wire KafkaConsumer and EmbeddedKafkaBroker
    // Consume the record from the EmbeddedKafkaBroker and then assert on it.
    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    ObjectMapper objectMapper;

    private Consumer<Integer, String> consumer;

    // instantiate a new consumer before each and every test case.
    @BeforeEach
    void setUp() {

        // 1. So group is primarily important to identify a consumer, It's a unique ID for a given consumer.
        // 2. then I'm going to pass the Autocommit value as "true", This means as soon as the record is read, I'm going to commit the values.
        // 3. then we are going to pass the embedded Kafka broker, So this is where the embedded Kafka broker comes into handy, and then we will
        //    map the properties from the embedded Kafka broker into the consumer properties automatically.
        // Finally : so now I'm going to create a new HashMap and then pass this value as an input argument to the HashMap so we can use it in the DefaultKafkaConsumerFactory.
        var configs = new HashMap<>(KafkaTestUtils.consumerProps("group1", "true", embeddedKafkaBroker));

        // Read only the new messages.
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");

        //  DefaultKafkaConsumerFactory used to pass some consumer configurations it takes 3 params :-
        // (1.map<String, object> to provide consumer configurations, 2.key deserializer 3.value deserializer)
        consumer = new DefaultKafkaConsumerFactory<>(configs, new IntegerDeserializer(), new StringDeserializer()).createConsumer();

        // this is going to make sure this consumer is going to be read the data from the embedded Kafka broker.
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void postEvent() {

        // (given)
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("content-type", MediaType.APPLICATION_JSON.toString());
        var httpEntity = new HttpEntity<>(TestUtil.libraryEventRecord(), httpHeaders);

        // restTemplate.exchange() this exchange function is going to invoke "/v1/libraryevent" post endpoint from our controller.
        // when we make the post call, the data is being published into the embedded Kafka broker.
        // (when)
        ResponseEntity responseEntity =
                restTemplate.exchange("/v1/libraryevent", HttpMethod.POST,
                httpEntity, LibraryEvent.class);

        // (then)
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        // here we got the consumer records that's been published.
        ConsumerRecords<Integer, String> consumerRecords = KafkaTestUtils.getRecords(consumer);
        assert consumerRecords.count() == 1;

        // and then we were able to iterate through them, and then we assert the record that's been published.
        consumerRecords.forEach( record -> {
                    var libraryEventActual = TestUtil.parseLibraryEventRecord(objectMapper, record.value());
                    System.out.println("libraryEventActual: " + libraryEventActual);
                    assertEquals(libraryEventActual, TestUtil.libraryEventRecord());
                }
        );
    }
}