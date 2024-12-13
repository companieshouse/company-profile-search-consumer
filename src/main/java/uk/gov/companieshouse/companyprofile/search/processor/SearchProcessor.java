package uk.gov.companieshouse.companyprofile.search.processor;

import consumer.exception.RetryableErrorException;
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

    /**
     * Process Company Profile ResourceChanged message.
     */
    public void processChangedMessage(Message<ResourceChangedData> resourceChangedMessage) {
        final ResourceChangedData payload = resourceChangedMessage.getPayload();
        final String contextId = payload.getContextId();
        final String companyNumber = payload.getResourceId();

        DataMapHolder.get().companyNumber(companyNumber);
        if (contextId == null || companyNumber == null) {
            throw new RetryableErrorException("Invalid message received");
        }

        Data companyProfileData = deserialiser.deserialiseCompanyProfile(payload.getData());

        apiClientService.putSearchRecord(companyNumber, companyProfileData);
        logger.infoContext(contextId, "Process Company Profile ResourceChanged message",
                DataMapHolder.getLogMap());
    }

    /**
     * Process Company Profile ResourceDeleted message.
     */
    public void processDeleteMessage(Message<ResourceChangedData> resourceChangedMessage) {
        final ResourceChangedData payload = resourceChangedMessage.getPayload();
        final String contextId = payload.getContextId();
        final String companyNumber = payload.getResourceId();

        DataMapHolder.get().companyNumber(companyNumber);
        if (contextId == null || companyNumber == null) {
            throw new RetryableErrorException("Invalid message received");
        }

        apiClientService.deleteCompanyProfileSearch(companyNumber);
        logger.infoContext(contextId, "Process Company Profile ResourceDeleted message",
                DataMapHolder.getLogMap());
    }
}