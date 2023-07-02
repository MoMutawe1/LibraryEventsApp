package producer.dto;

public record LibraryEvent(Integer LibraryEventId, LibraryEventType libraryEventType, Book book) {
    
}
