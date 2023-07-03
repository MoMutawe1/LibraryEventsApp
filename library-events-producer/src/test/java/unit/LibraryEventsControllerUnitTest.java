package unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import producer.dto.LibraryEvent;
import producer.eventsproducer.LibraryEventsProducer;
import util.TestUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import producer.controller.LibraryEventsController;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
    @SpringBootTest: (test the entire application context of your Spring application, including the web layer, service layer, and data access layer.)
        bring up the whole spring context, which means it's going to bring the whole application up,
        and then we are invoking the endpoint through the test rest template.

    @WebMvcTest: (test only the web layer of your application.)
        slice part of the application context and have that available for your test case.
        This concept is also called 'test slice'.

    @Autowired
    MockMvc:
    using MockMvc, allow us to inject the controller endpoints, so we can perform calls to those endpoints.

*/

@WebMvcTest(LibraryEventsController.class)
class LibraryEventsControllerUnitTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    // this is not going to load any Kafka components or any other related components
    // because we marked out the whole library events Producer layer to be mocked.
    @MockBean
    LibraryEventsProducer libraryEventsProducer;

    @Test
    void postEventSuccess() throws Exception {

        // given
        // here the json represent our request body.
        var json = objectMapper.writeValueAsString(TestUtil.libraryEventRecord());


        // when
        // this is not going to load any Kafka components, you won't find any logs related to kafka in the console, because we are just testing the web layer (controller) and mock all other layers.
        Mockito.when(libraryEventsProducer.sendLibraryEvent(ArgumentMatchers.isA(LibraryEvent.class))).thenReturn(null);
        //expect / then
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/libraryevent")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    void postEventFailure_4xx() throws Exception {  // use .bookRecordWithInvalidValues() then .is4xxClientError() is expected

        // given
        // here the json represent our request body.
        var json = objectMapper.writeValueAsString(TestUtil.bookRecordWithInvalidValues());
        var expectedErrorMessage = "book.bookId - must not be null, book.bookName - must not be blank";

        // when
        // this is not going to load any Kafka components, you won't find any logs related to kafka in the console, because we are just testing the web layer (controller) and mock all other layers.
        Mockito.when(libraryEventsProducer.sendLibraryEvent(ArgumentMatchers.isA(LibraryEvent.class))).thenReturn(null);

        //expect / then
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/libraryevent")
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON))
                        .andExpect(MockMvcResultMatchers.status().is4xxClientError())
                        .andExpect(MockMvcResultMatchers.content().string(expectedErrorMessage));
    }
}