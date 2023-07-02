package producer.eventsproducer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import producer.dto.LibraryEvent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
// this going to act as a producer to basically produce a messages into the Kafka topic.
public class LibraryEventsProducer {

    private final KafkaTemplate<Integer, String> kafkaTemplate;
    ObjectMapper objectMapper;

    // explicitly providing the topic to which this message needs to be sent.
    @Value("${spring.kafka.topic}")
    public String topic;

    public LibraryEventsProducer(KafkaTemplate<Integer, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    // this call is going to send events to a kafka topic, then return a type named Completablefuture, that Completablefuture is of type future,
    // which means that something which is going to complete in the future and When that happens, we need to have a handle of both success and error scenarios.
    public CompletableFuture<SendResult<Integer, String>> sendLibraryEvent(LibraryEvent libraryEvent) throws JsonProcessingException {

        var key = libraryEvent.LibraryEventId();
        var value = objectMapper.writeValueAsString(libraryEvent);

        // So when we make kafkaTemplate.send call, what it returns is a completeable feature (asynchronous calls).
        // always remember these two steps that happens behind the scenes for you :
        // 1. first it will trigger (blocking call) - to get the metadata about the kafka cluster.
        // 2. then the Asynchronous Send message happens - Return a CompletableFuture.
        var completableFuture = kafkaTemplate.send(topic, key, value);  // kafkaTemplate.send() Send the data to the provided topic with the provided key and no partition.

        return completableFuture
                .whenComplete((sendResult, throwable) -> {
                    if (throwable != null) {
                        handleFailure(key, value, throwable);
                    } else {
                        handleSuccess(key, value, sendResult);
                    }
                });
    }


    // This represents (Synchronous - Blocking calls to the Kafka Cluster) - just for comparing purpose.
    // but remember the recommended option is always have these calls as (Asynchronous) calls so that your client is not going to experience any failures.
    // these two steps that happens behind the scenes for you in Synchronous calls:
    // 1. first it will trigger (blocking call) - to get the metadata about the kafka cluster.
    // 2. Block and wait until the message is sent to the kafka cluster and return.
    public SendResult<Integer, String> sendLibraryEventSynchronous(LibraryEvent libraryEvent) throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {

        Integer key = libraryEvent.LibraryEventId();
        String value = objectMapper.writeValueAsString(libraryEvent);
        SendResult<Integer, String> sendResult = null;

        try {
            // kafkaTemplate.sendDefault() Send the data to the default topic with the provided key and no partition.
            sendResult = kafkaTemplate.sendDefault(key, value).get(1, TimeUnit.SECONDS); // it's going to wait until the timeout from this call, then it's going to throw an exception.
        } catch (ExecutionException | InterruptedException e) {
            log.error("ExecutionException/InterruptedException Sending the Message and the exception is {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Exception Sending the Message and the exception is {}", e.getMessage());
            throw e;
        }

        return sendResult;
    }

    private void handleFailure(Integer key, String value, Throwable ex) {
        log.error("Error Sending the Message and the exception is {}", ex.getMessage(), ex);
    }

    private void handleSuccess(Integer key, String value, SendResult<Integer, String> result) {
        log.info("Message Sent SuccessFully for the key : {} and the value is {} , partition is {}", key, value, result.getRecordMetadata().partition());
    }
}
