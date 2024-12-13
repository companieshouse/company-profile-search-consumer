package uk.gov.companieshouse.companyprofile.search.logging;

import static org.springframework.kafka.retrytopic.RetryTopicHeaders.DEFAULT_HEADER_ATTEMPTS;
import static org.springframework.kafka.support.KafkaHeaders.OFFSET;
import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_PARTITION;
import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_TOPIC;
import static uk.gov.companieshouse.companyprofile.search.Application.NAMESPACE;

import consumer.exception.NonRetryableErrorException;
import consumer.exception.RetryableErrorException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
@Aspect
class StructuredLoggingKafkaListenerAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String LOG_MESSAGE_RECEIVED = "Processing delta";
    private static final String LOG_MESSAGE_DELETE_RECEIVED = "Processing DELETE delta";
    private static final String LOG_MESSAGE_PROCESSED = "Processed delta";
    private static final String LOG_MESSAGE_DELETE_PROCESSED = "Processed DELETE delta";
    private static final String EXCEPTION_MESSAGE = "%s exception thrown";

    private final int maxAttempts;

    StructuredLoggingKafkaListenerAspect(@Value("${company-profile.search.retry-attempts}") int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    @Around("@annotation(org.springframework.kafka.annotation.KafkaListener)")
    public Object manageStructuredLogging(ProceedingJoinPoint joinPoint) throws Throwable {
        int retryCount = 0;
        try {
            Message<?> message = (Message<?>) joinPoint.getArgs()[0];
            MessageHeaders headers = message.getHeaders();
            retryCount = Optional.ofNullable(headers.get(DEFAULT_HEADER_ATTEMPTS))
                    .map(attempts -> ByteBuffer.wrap((byte[]) attempts).getInt())
                    .orElse(1) - 1;

            ResourceChangedData resourceChangedData = extractResourceChangedData(message.getPayload());

            DataMapHolder.initialise(Optional.ofNullable(resourceChangedData.getContextId())
                    .orElse(UUID.randomUUID().toString()));

            DataMapHolder.get()
                    .retryCount(retryCount)
                    .topic((String) headers.get(RECEIVED_TOPIC))
                    .partition((Integer) headers.get(RECEIVED_PARTITION))
                    .offset((Long) headers.get(OFFSET));

            final String eventType = resourceChangedData.getEvent().getType();
            LOGGER.info("deleted".equals(eventType) ? LOG_MESSAGE_DELETE_RECEIVED : LOG_MESSAGE_RECEIVED,
                    DataMapHolder.getLogMap());

            Object result = joinPoint.proceed();

            LOGGER.info("deleted".equals(eventType) ? LOG_MESSAGE_DELETE_PROCESSED : LOG_MESSAGE_PROCESSED,
                    DataMapHolder.getLogMap());

            return result;
        } catch (RetryableErrorException ex) {
            // maxAttempts includes first attempt which is not a retry
            if (retryCount >= maxAttempts - 1) {
                LOGGER.error("Max retry attempts reached", ex, DataMapHolder.getLogMap());
            } else {
                LOGGER.info(EXCEPTION_MESSAGE.formatted(ex.getClass().getSimpleName()), DataMapHolder.getLogMap());
            }
            throw ex;
        } catch (Exception ex) {
            LOGGER.error("Exception thrown", ex, DataMapHolder.getLogMap());
            throw ex;
        } finally {
            DataMapHolder.clear();
        }
    }

    private ResourceChangedData extractResourceChangedData(Object payload) {
        if (payload instanceof ResourceChangedData resourceChangedData) {
            return resourceChangedData;
        }
        String errorMessage = "Invalid payload type, payload: [%s]".formatted(payload.toString());
        LOGGER.error(errorMessage, DataMapHolder.getLogMap());
        throw new NonRetryableErrorException(errorMessage);
    }
}
