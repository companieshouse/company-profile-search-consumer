package uk.gov.companieshouse.companyprofile.search.service;

import consumer.exception.RetryableErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.companyprofile.search.logging.DataMapHolder;
import uk.gov.companieshouse.companyprofile.search.service.api.ApiClientServiceImpl;
import uk.gov.companieshouse.companyprofile.search.service.api.BaseApiClientServiceImpl;
import uk.gov.companieshouse.logging.Logger;

@Service
public class CompanyProfileService extends BaseApiClientServiceImpl {

    @Autowired
    private final ApiClientServiceImpl apiClientService;

    /**
     * Construct a company profile service - used to retrieve a company profile record.
     *
     * @param logger the CH logger
     */
    @Autowired
    public CompanyProfileService(Logger logger, ApiClientServiceImpl apiClientService) {
        super(logger);
        this.apiClientService = apiClientService;
    }

    /**
     * Retrieve a company profile given a company number from company-profile-api.
     *
     * @param companyNumber the company's company number
     * @return an ApiResponse containing the CompanyProfileApi data model
     */
    public ApiResponse<Data> getCompanyProfile(String contextId, String companyNumber)
            throws RetryableErrorException {
        logger.trace(String.format("Call to GET company profile with contextId %s "
                + "and company number %s", contextId, companyNumber), DataMapHolder.getLogMap());

        String uri = String.format("/company/%s", companyNumber);

        InternalApiClient internalApiClient = apiClientService.getApiClient(contextId);
        internalApiClient.getHttpClient().setRequestId(contextId);

        return executeOp(contextId, "getCompanyProfile", uri, internalApiClient
                .privateCompanyResourceHandler()
                .getCompanyFullProfile(uri));
    }
}