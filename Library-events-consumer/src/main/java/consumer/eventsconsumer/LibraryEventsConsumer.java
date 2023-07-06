package consumer.eventsconsumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/*
topics = {"library-events"}        // Kafka consumer can read from multiple topics, but for our use case, we are just reading from one topic.
The producer has a concept of ProducerRecord, Likewise we have a ConsumerRecord too, and it's going to be of type, integer and string because that's what
is being published from our producer.

log.info("ConsumerRecord : {} ", consumerRecord) : It has the Topic name, which partition the message is being passed to, also it has the key and value
which is the actual message that is being passed from the producer.

// @KafkaListener annotation uses the ConcurrentMessageListenerContainer
                  with the ConcurrentMessageListenerContainer you can spin up multiple instances of the same Kafka MessageListenerContainer.
*/

@Component
@Slf4j
public class LibraryEventsConsumer {

    @KafkaListener(topics = {"library-events"})
    public void onMessage(ConsumerRecord<Integer, String> consumerRecord) {

        log.info("ConsumerRecord : {} ", consumerRecord);
    }
}
