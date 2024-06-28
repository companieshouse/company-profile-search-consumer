package uk.gov.companieshouse.companyprofile.search.consumer;

import consumer.exception.NonRetryableErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.messaging.Message;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.companyprofile.search.logging.DataMapHolder;
import uk.gov.companieshouse.companyprofile.search.processor.SearchProcessor;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;


@Component
public class SearchConsumer {

    private final Logger logger;
    public final KafkaTemplate<String, Object> kafkaTemplate;
    private final SearchProcessor searchProcessor;

    /**
     * Consumes messages from stream-company-profile.
     */
    @Autowired
    public SearchConsumer(Logger logger, KafkaTemplate<String, Object> kafkaTemplate,
                          SearchProcessor searchProcessor) {
        this.logger = logger;
        this.kafkaTemplate = kafkaTemplate;
        this.searchProcessor = searchProcessor;
    }

    /**
     * Receives messages from stream-company-profile.
     */
    @RetryableTopic(
            attempts = "${company-profile.search.retry-attempts}",
            backoff = @Backoff(delayExpression = "${company-profile.search.backoff-delay}"),
            retryTopicSuffix = "-${company-profile.search.group-id}-retry",
            dltTopicSuffix = "-${company-profile.search.group-id}-error",
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            autoCreateTopics = "false",
            exclude = NonRetryableErrorException.class)
    @KafkaListener(
            topics = "${company-profile.search.topic}",
            groupId = "${company-profile.search.group-id}",
            containerFactory = "listenerContainerFactory")
    public void receive(Message<ResourceChangedData> resourceChangedMessage) {
        logger.infoContext(resourceChangedMessage.getPayload().getContextId(),
                "Starting to process a message from stream-company-profile",
                DataMapHolder.getLogMap());
        String eventType = resourceChangedMessage.getPayload().getEvent().getType();
        try {
            if (eventType.equals("changed")) {
                searchProcessor.processChangedMessage(resourceChangedMessage);
            }
            if (eventType.equals("deleted")) {
                searchProcessor.processDeleteMessage(resourceChangedMessage);
            }
        } catch (Exception exception) {
            logger.error(String.format("Exception occurred while processing w"
                    + "ith message: %s", resourceChangedMessage), exception);
            throw exception;
        }
    }
}
