package uk.gov.companieshouse.companyprofile.search.service;

import static uk.gov.companieshouse.companyprofile.search.Application.NAMESPACE;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
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
        LOGGER.info("Sending PUT request to API", DataMapHolder.getLogMap());

        final String formattedUri = String.format(URI, companyNumber);
        try {
            internalApiClientSupplier.get()
                    .privateSearchResourceHandler()
                    .companySearch()
                    .upsertCompanyProfile(formattedUri, data)
                    .execute();
        } catch (ApiErrorResponseException ex) {
            responseHandler.handle(ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(ex);
        }
    }

    public void deleteCompanyProfileSearch(String companyNumber) {
        LOGGER.info("Sending DELETE request to API", DataMapHolder.getLogMap());

        final String formattedUri = String.format(URI, companyNumber);
        try {
            internalApiClientSupplier.get()
                    .privateSearchResourceHandler()
                    .companySearch()
                    .deleteCompanyProfile(formattedUri)
                    .execute();
        } catch (ApiErrorResponseException ex) {
            responseHandler.handle(ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(ex);
        }
    }
}
