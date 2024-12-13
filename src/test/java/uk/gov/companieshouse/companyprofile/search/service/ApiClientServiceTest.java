package uk.gov.companieshouse.companyprofile.search.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.search.PrivateSearchResourceHandler;
import uk.gov.companieshouse.api.handler.search.company.PrivateCompanySearchHandler;
import uk.gov.companieshouse.api.handler.search.company.request.PrivateCompanySearchDelete;
import uk.gov.companieshouse.api.handler.search.company.request.PrivateCompanySearchUpsert;
import uk.gov.companieshouse.api.model.ApiResponse;

@ExtendWith(MockitoExtension.class)
class ApiClientServiceTest {

    private static final String COMPANY_NUMBER = "12345678";
    private static final String URI = "/company-search/companies/%s".formatted(COMPANY_NUMBER);
    private static final ApiResponse<Void> SUCCESS_RESPONSE = new ApiResponse<>(200, null);

    @InjectMocks
    private ApiClientService apiClientService;

    @Mock
    private Supplier<InternalApiClient> internalApiClientSupplier;
    @Mock
    private ResponseHandler responseHandler;

    @Mock
    private InternalApiClient internalApiClient;
    @Mock
    private Data data;
    @Mock
    private PrivateSearchResourceHandler privateSearchResourceHandler;
    @Mock
    private PrivateCompanySearchHandler privateCompanySearchHandler;
    @Mock
    private PrivateCompanySearchUpsert privateCompanySearchUpsert;
    @Mock
    private PrivateCompanySearchDelete privateCompanySearchDelete;

    @Test
    void shouldSuccessfullySendPutRequest() throws Exception {
        // given
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateSearchResourceHandler()).thenReturn(privateSearchResourceHandler);
        when(privateSearchResourceHandler.companySearch()).thenReturn(privateCompanySearchHandler);
        when(privateCompanySearchHandler.upsertCompanyProfile(anyString(), any())).thenReturn(
                privateCompanySearchUpsert);
        when(privateCompanySearchUpsert.execute()).thenReturn(SUCCESS_RESPONSE);

        // when
        apiClientService.putSearchRecord(COMPANY_NUMBER, data);

        // then
        verify(privateCompanySearchHandler).upsertCompanyProfile(URI, data);
        verifyNoInteractions(responseHandler);
    }

    @Test
    void shouldSendPutRequestAndHandleNon200ResponseFromApi() throws Exception {
        // given
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateSearchResourceHandler()).thenReturn(privateSearchResourceHandler);
        when(privateSearchResourceHandler.companySearch()).thenReturn(privateCompanySearchHandler);
        when(privateCompanySearchHandler.upsertCompanyProfile(anyString(), any())).thenReturn(
                privateCompanySearchUpsert);
        when(privateCompanySearchUpsert.execute()).thenThrow(ApiErrorResponseException.class);

        // when
        apiClientService.putSearchRecord(COMPANY_NUMBER, data);

        // then
        verify(privateCompanySearchHandler).upsertCompanyProfile(URI, data);
        verify(responseHandler).handle(any(ApiErrorResponseException.class));
    }

    @Test
    void shouldSendPutRequestAndHandleURIValidationExceptionFromApi() throws Exception {
        // given
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateSearchResourceHandler()).thenReturn(privateSearchResourceHandler);
        when(privateSearchResourceHandler.companySearch()).thenReturn(privateCompanySearchHandler);
        when(privateCompanySearchHandler.upsertCompanyProfile(anyString(), any())).thenReturn(
                privateCompanySearchUpsert);
        when(privateCompanySearchUpsert.execute()).thenThrow(URIValidationException.class);

        // when
        apiClientService.putSearchRecord(COMPANY_NUMBER, data);

        // then
        verify(privateCompanySearchHandler).upsertCompanyProfile(URI, data);
        verify(responseHandler).handle(any(URIValidationException.class));
    }

    @Test
    void shouldSuccessfullySendDeleteRequest() throws Exception {
        // given
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateSearchResourceHandler()).thenReturn(privateSearchResourceHandler);
        when(privateSearchResourceHandler.companySearch()).thenReturn(privateCompanySearchHandler);
        when(privateCompanySearchHandler.deleteCompanyProfile(anyString())).thenReturn(privateCompanySearchDelete);
        when(privateCompanySearchDelete.execute()).thenReturn(SUCCESS_RESPONSE);

        // when
        apiClientService.deleteCompanyProfileSearch(COMPANY_NUMBER);

        // then
        verify(privateCompanySearchHandler).deleteCompanyProfile(URI);
        verifyNoInteractions(responseHandler);
    }

    @Test
    void shouldSendDeleteRequestAndHandleNon200ResponseFromApi() throws Exception {
        // given
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateSearchResourceHandler()).thenReturn(privateSearchResourceHandler);
        when(privateSearchResourceHandler.companySearch()).thenReturn(privateCompanySearchHandler);
        when(privateCompanySearchHandler.deleteCompanyProfile(anyString())).thenReturn(privateCompanySearchDelete);
        when(privateCompanySearchDelete.execute()).thenThrow(ApiErrorResponseException.class);

        // when
        apiClientService.deleteCompanyProfileSearch(COMPANY_NUMBER);

        // then
        verify(privateCompanySearchHandler).deleteCompanyProfile(URI);
        verify(responseHandler).handle(any(ApiErrorResponseException.class));
    }

    @Test
    void shouldSendDeleteRequestAndHandleURIValidationExceptionFromApi() throws Exception {
        // given
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateSearchResourceHandler()).thenReturn(privateSearchResourceHandler);
        when(privateSearchResourceHandler.companySearch()).thenReturn(privateCompanySearchHandler);
        when(privateCompanySearchHandler.deleteCompanyProfile(anyString())).thenReturn(privateCompanySearchDelete);
        when(privateCompanySearchDelete.execute()).thenThrow(URIValidationException.class);

        // when
        apiClientService.deleteCompanyProfileSearch(COMPANY_NUMBER);

        // then
        verify(privateCompanySearchHandler).deleteCompanyProfile(URI);
        verify(responseHandler).handle(any(URIValidationException.class));
    }
}