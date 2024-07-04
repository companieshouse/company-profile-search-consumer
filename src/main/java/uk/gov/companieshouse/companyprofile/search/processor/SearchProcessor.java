package uk.gov.companieshouse.companyprofile.search.processor;

import consumer.exception.NonRetryableErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.companyprofile.search.logging.DataMapHolder;
import uk.gov.companieshouse.companyprofile.search.service.CompanyProfileService;
import uk.gov.companieshouse.companyprofile.search.service.api.ApiClientService;
import uk.gov.companieshouse.companyprofile.search.util.Helper;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
public class SearchProcessor {

    private final Logger logger;
    private final ApiClientService apiClientService;
    private final CompanyProfileService companyProfileService;


    /**
     * Constructor for processor.
     */
    @Autowired
    public SearchProcessor(Logger logger,
                           ApiClientService apiClientService,
                           CompanyProfileService companyProfileService) {
        this.logger = logger;
        this.apiClientService = apiClientService;
        this.companyProfileService = companyProfileService;
    }

    /**
     * Process Company Profile ResourceChanged message.
     */
    public void processChangedMessage(Message<ResourceChangedData> resourceChangedMessage) {
        final ResourceChangedData payload = resourceChangedMessage.getPayload();
        final String contextId = payload.getContextId();
        final String companyNumber = Helper.extractCompanyNumber(payload);

        if (contextId == null || companyNumber == null) {
            throw new NonRetryableErrorException("Invalid message received");
        }

        DataMapHolder.get()
                .companyNumber(companyNumber);
        Data companyProfileData = companyProfileService
                .getCompanyProfile(contextId, companyNumber).getData();

        apiClientService.putSearchRecord(contextId, companyNumber, companyProfileData);
    }

    /**
     * Delete Company Profile ResourceChanged message.
     */
    public void processDeleteMessage(Message<ResourceChangedData> resourceChangedMessage) {
        final ResourceChangedData payload = resourceChangedMessage.getPayload();
        final String contextId = payload.getContextId();
        final String companyNumber = payload.getResourceId();

        if (contextId == null || companyNumber == null) {
            throw new NonRetryableErrorException("Invalid message received");
        }

        DataMapHolder.get()
                .companyNumber(companyNumber);

        apiClientService.deleteCompanyProfileSearch(contextId, companyNumber);
    }
}