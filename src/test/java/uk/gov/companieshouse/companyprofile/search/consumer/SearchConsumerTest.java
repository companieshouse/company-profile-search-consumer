package uk.gov.companieshouse.companyprofile.search.consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import consumer.exception.NonRetryableErrorException;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import uk.gov.companieshouse.companyprofile.search.processor.SearchProcessor;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

class SearchConsumerTest {

    private Logger logger;
    private KafkaTemplate<String, Object> kafkaTemplate;
    private SearchProcessor searchProcessor;
    private SearchConsumer searchConsumer;

    @BeforeEach
    void setUp() {
        logger = mock(Logger.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        searchProcessor = mock(SearchProcessor.class);

        searchConsumer = new SearchConsumer(
                logger,
                kafkaTemplate,
                searchProcessor
        );

        setField(searchConsumer, "topic", "test-topic");
        setField(searchConsumer, "groupId", "test-group");
    }

    @Test
    void shouldLogWhenConsumerBeanIsCreated() {
        searchConsumer.init();

        verify(logger).info("***** SEARCH CONSUMER BEAN CREATED *****");
        verify(logger).info("***** TOPIC: test-topic");
        verify(logger).info("***** GROUP: test-group");
    }

    @Test
    void shouldProcessChangedMessage() {
        Message<ResourceChangedData> message = messageWithEventType("changed");

        searchConsumer.receive(message);

        verify(searchProcessor).processChangedMessage(message);
        verify(searchProcessor, never()).processDeleteMessage(message);
    }

    @Test
    void shouldProcessDeletedMessage() {
        Message<ResourceChangedData> message = messageWithEventType("deleted");

        searchConsumer.receive(message);

        verify(searchProcessor).processDeleteMessage(message);
        verify(searchProcessor, never()).processChangedMessage(message);
    }

    @Test
    void shouldThrowNonRetryableErrorExceptionForUnknownEventType() {
        Message<ResourceChangedData> message = messageWithEventType("created");

        assertThatThrownBy(() -> searchConsumer.receive(message))
                .isInstanceOf(NonRetryableErrorException.class)
                .hasMessage("Incorrect event type");

        verify(searchProcessor, never()).processChangedMessage(message);
        verify(searchProcessor, never()).processDeleteMessage(message);
    }

    @Test
    void shouldThrowNonRetryableErrorExceptionForNullEventType() {
        Message<ResourceChangedData> message = messageWithEventType(null);

        assertThatThrownBy(() -> searchConsumer.receive(message))
                .isInstanceOf(NullPointerException.class);

        verify(searchProcessor, never()).processChangedMessage(message);
        verify(searchProcessor, never()).processDeleteMessage(message);
    }

    private Message<ResourceChangedData> messageWithEventType(String eventType) {
        Message<ResourceChangedData> message = mock(Message.class);
        ResourceChangedData payload = mock(ResourceChangedData.class);
        EventRecord event = mock(EventRecord.class);

        when(message.getPayload()).thenReturn(payload);
        when(payload.getEvent()).thenReturn(event);
        when(event.getType()).thenReturn(eventType);

        return message;
    }

    private void setField(Object target, String fieldName, String value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }
}