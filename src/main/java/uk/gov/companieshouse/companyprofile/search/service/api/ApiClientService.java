package uk.gov.companieshouse.companyprofile.search.service.api;

import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.model.ApiResponse;

/**
 * The {@code ApiClientService} interface provides an abstraction that can be
 * used when testing {@code ApiClientManager} static methods, without imposing
 * the use of a test framework that supports mocking of static methods.
 */
public interface ApiClientService {

    InternalApiClient getApiClient(String contextId);

    /**
     * Submit Search Information.
     */
    ApiResponse<Void> putSearchRecord(
            final String log,
            final String companyId,
            final Data companyProfileData
    );

    /**
     * Delete Company Profile.
     */
    ApiResponse<Void> deleteCompanyProfileSearch(
            final String log,
            final String companyId);
}
