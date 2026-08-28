package uk.gov.companieshouse.companyprofile.search.consumer;

import consumer.exception.NonRetryableErrorException;
import org.jspecify.annotations.NonNull;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.companyprofile.search.logging.DataMapHolder;
import uk.gov.companieshouse.companyprofile.search.processor.SearchProcessor;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
public class SearchConsumer {

    private final Logger logger;
    public final KafkaTemplate<@NonNull String, @NonNull Object> kafkaTemplate;
    private final SearchProcessor searchProcessor;

    /**
     * Consumes messages from stream-company-profile.
     */
    public SearchConsumer(Logger logger, KafkaTemplate<@NonNull String, @NonNull Object> kafkaTemplate, SearchProcessor searchProcessor) {
        this.logger = logger;
        this.kafkaTemplate = kafkaTemplate;
        this.searchProcessor = searchProcessor;
    }

    /**
     * Receives messages from stream-company-profile.
     */
    @RetryableTopic(
            attempts = "${company-profile.search.retry-attempts}",
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            backOff = @BackOff(delayString = "${company-profile.search.backoff-delay}"),
            retryTopicSuffix = "-${company-profile.search.group-id}-retry",
            dltTopicSuffix = "-${company-profile.search.group-id}-error",
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            autoCreateTopics = "${company-profile.search.autocreate.topics:false}",
            exclude = NonRetryableErrorException.class
    )
    @KafkaListener(
            topics = "${company-profile.search.topic}",
            groupId = "${company-profile.search.group-id}",
            containerFactory = "listenerContainerFactory",
            autoStartup = "${company-profile.search.autostart.enabled}"
    )
    public void receive(final Message<@NonNull ResourceChangedData> message) {
        logger.info("receive(event_type=%s) method called.".formatted(
                message.getPayload().getEvent().getType()), DataMapHolder.getLogMap());

        final String eventType = message.getPayload().getEvent().getType();

        if (eventType.equals("changed")) {
            searchProcessor.processChangedMessage(message);

        } else if (eventType.equals("deleted")) {
            searchProcessor.processDeleteMessage(message);

        } else {
            logger.error("Incorrect event type", DataMapHolder.getLogMap());
            throw new NonRetryableErrorException("Incorrect event type");
        }
    }
}
