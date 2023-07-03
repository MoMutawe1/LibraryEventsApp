package producer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import producer.dto.LibraryEvent;
import producer.dto.LibraryEventType;
import producer.eventsproducer.LibraryEventsProducer;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@RestController
@Slf4j
public class LibraryEventsController {

    @Autowired
    public LibraryEventsProducer libraryEventsProducer;

    @PostMapping("/v1/libraryevent")
    public ResponseEntity<LibraryEvent> postEvent(@RequestBody @Valid LibraryEvent libraryEvent) throws JsonProcessingException, ExecutionException, InterruptedException, TimeoutException {
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

    @PutMapping("/v1/libraryevent")
    public ResponseEntity<?> updateLibraryEvent(@RequestBody @Valid LibraryEvent libraryEvent) throws JsonProcessingException {

        // check if the id is not equal to null & the request type is UPDATE.
        ResponseEntity<String> BAD_REQUEST = validateLibraryEvent(libraryEvent);
        if (BAD_REQUEST != null) return BAD_REQUEST;

        libraryEventsProducer.sendLibraryEvent(libraryEvent);
        log.info("after produce call");
        return ResponseEntity.status(HttpStatus.OK).body(libraryEvent);
    }

    // do the updateLibraryEvent validation in a separate method, for a cleaner impl.
    private static ResponseEntity<String> validateLibraryEvent(LibraryEvent libraryEvent) {

        // LibraryEventId should not be null for PUT request.
        if (libraryEvent.LibraryEventId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please pass the LibraryEventId");
        }

        // LibraryEventType should be UPDATE for PUT request.
        if (!LibraryEventType.UPDATE.equals(libraryEvent.libraryEventType()))  {
            log.info("Inside the if block");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Only UPDATE event type is supported");
        }

        return null;
    }
}
