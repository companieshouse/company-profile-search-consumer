package uk.gov.companieshouse.companyprofile.search.service;

import static uk.gov.companieshouse.companyprofile.search.Application.NAMESPACE;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.search.PrivateSearchResourceHandler;
import uk.gov.companieshouse.api.handler.search.company.PrivateCompanySearchHandler;
import uk.gov.companieshouse.api.handler.search.company.request.PrivateCompanySearchDelete;
import uk.gov.companieshouse.api.handler.search.company.request.PrivateCompanySearchUpsert;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.companyprofile.search.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class ApiClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String URI = "/company-search/companies/%s";

    private final Supplier<InternalApiClient> internalApiClientSupplier;
    private final ResponseHandler responseHandler;

    public ApiClientService(Supplier<InternalApiClient> internalApiClientSupplier, ResponseHandler responseHandler) {
        this.internalApiClientSupplier = internalApiClientSupplier;
        this.responseHandler = responseHandler;
    }

    public void putSearchRecord(String companyNumber, Data data) {
        LOGGER.info("putSearchRecord(companyNumber=%s, data) method called".formatted(companyNumber), DataMapHolder.getLogMap());

        final String formattedUri = String.format(URI, companyNumber);
        try {
            InternalApiClient apiClient = internalApiClientSupplier.get();
            PrivateSearchResourceHandler resourceHandler = apiClient.privateSearchResourceHandler();
            PrivateCompanySearchHandler searchHandler = resourceHandler.companySearch();
            PrivateCompanySearchUpsert searchUpsert = searchHandler.upsertCompanyProfile(formattedUri, data);

            ApiResponse<Void> apiResponse = searchUpsert.execute();

            LOGGER.trace("API Response(Status=%d): URI -> '%s'".formatted(apiResponse.getStatusCode(), formattedUri));

        } catch (ApiErrorResponseException ex) {
            responseHandler.handle(ex);

        } catch (URIValidationException ex) {
            responseHandler.handle(ex);
        }
    }

    public void deleteCompanyProfileSearch(String companyNumber) {
        LOGGER.info("deleteCompanyProfileSearch(companyNumber=%s) method called".formatted(companyNumber), DataMapHolder.getLogMap());

        final String formattedUri = String.format(URI, companyNumber);
        try {
            InternalApiClient apiClient = internalApiClientSupplier.get();
            PrivateSearchResourceHandler resourceHandler = apiClient.privateSearchResourceHandler();
            PrivateCompanySearchHandler searchHandler = resourceHandler.companySearch();
            PrivateCompanySearchDelete searchDelete = searchHandler.deleteCompanyProfile(formattedUri);

            ApiResponse<Void> apiResponse = searchDelete.execute();

            LOGGER.trace("API Response(Status=%d): URI -> '%s'".formatted(apiResponse.getStatusCode(), formattedUri));

        } catch (ApiErrorResponseException ex) {
            responseHandler.handle(ex);

        } catch (URIValidationException ex) {
            responseHandler.handle(ex);
        }
    }
}
