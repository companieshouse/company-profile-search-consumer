package uk.gov.companieshouse.companyprofile.search.processor;

import consumer.exception.RetryableErrorException;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.companyprofile.search.deserialiser.CompanyProfileDeserialiser;
import uk.gov.companieshouse.companyprofile.search.logging.DataMapHolder;
import uk.gov.companieshouse.companyprofile.search.service.ApiClientService;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
public class SearchProcessor {

    private final Logger logger;
    private final ApiClientService apiClientService;
    private final CompanyProfileDeserialiser deserialiser;

    public SearchProcessor(Logger logger, ApiClientService apiClientService, CompanyProfileDeserialiser deserialiser) {
        this.logger = logger;
        this.apiClientService = apiClientService;
        this.deserialiser = deserialiser;
    }

    private ResourceChangedData validateMessage(final Message<@NonNull ResourceChangedData> message) {
        logger.info("validateMessage(resource_kind=%s) method called.".formatted(message.getPayload().getResourceKind()));

        final ResourceChangedData payload = message.getPayload();
        final String contextId = payload.getContextId();
        final String companyNumber = payload.getResourceId();

        DataMapHolder.get().companyNumber(companyNumber);
        if (contextId == null || companyNumber == null) {
            throw new RetryableErrorException("Invalid message received: contextId or companyNumber is null");
        }

        return payload;
    }

    /**
     * Process Company Profile ResourceChanged message.
     */
    public void processChangedMessage(Message<@NonNull ResourceChangedData> message) {
        logger.info("processChangedMessage() method called.");

        final ResourceChangedData payload = validateMessage(message);
        final String contextId = payload.getContextId();
        final String companyNumber = payload.getResourceId();

        Data companyProfileData = deserialiser.deserialiseCompanyProfile(payload.getData());

        apiClientService.putSearchRecord(companyNumber, companyProfileData);

        logger.infoContext(contextId, "Process Company Profile ResourceChanged message", DataMapHolder.getLogMap());
    }

    /**
     * Process Company Profile ResourceDeleted message.
     */
    public void processDeleteMessage(Message<@NonNull ResourceChangedData> message) {
        logger.info("processChangedMessage() method called.");

        final ResourceChangedData payload = validateMessage(message);
        final String contextId = payload.getContextId();
        final String companyNumber = payload.getResourceId();

        apiClientService.deleteCompanyProfileSearch(companyNumber);

        logger.infoContext(contextId, "Process Company Profile ResourceDeleted message", DataMapHolder.getLogMap());
    }
}