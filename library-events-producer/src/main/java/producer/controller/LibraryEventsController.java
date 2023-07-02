package producer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import producer.dto.LibraryEvent;
import producer.eventsproducer.LibraryEventsProducer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@RestController
@Slf4j
public class LibraryEventsController {

    @Autowired
    public LibraryEventsProducer libraryEventsProducer;

    @PostMapping("/v1/libraryevent")
    public ResponseEntity<LibraryEvent> postEvent(@RequestBody LibraryEvent libraryEvent) throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
        log.info("LibraryEvent: {} ", libraryEvent);

        // So when we receive the request body it's going to be forwarded to the sendLibraryEvent.
        // and this is a function which is going to take care of sending the messages into the Kafka topic using the KafkaTemplate (asynchronously).
        libraryEventsProducer.sendLibraryEvent(libraryEvent);

        // this is a function which is going to take care of sending the messages into the Kafka topic using the KafkaTemplate (synchronously).
        // earlier, the after sending event log below, was present before the message sent successfully from the LibraryEventsProducer class is completed.
        // but in this case, the "After Sending LibraryEvent" which is the log below, comes after the message sent successfully logger from the Kafka Producer call.
        libraryEventsProducer.sendLibraryEventSynchronous(libraryEvent);

        // this log file will be executed even before the above sendLibraryEven producer call fulfilled,
        // so this call got completed, and we got the 201 message also, So the 201 message has been sent even before the message is being sent to the Kafka cluster.
        // the last thing you will find the handleSuccess log message printed in the console after the post mapping is completed.
        log.info("After Sending LibraryEvent");

        return ResponseEntity.status(HttpStatus.CREATED).body(libraryEvent);
    }
}
