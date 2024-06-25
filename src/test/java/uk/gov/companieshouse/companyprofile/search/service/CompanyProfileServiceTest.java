package uk.gov.companieshouse.companyprofile.search.service;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import consumer.exception.RetryableErrorException;
import java.util.Collections;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.company.PrivateCompanyResourceHandler;
import uk.gov.companieshouse.api.handler.company.request.PrivateCompanyFullProfileGet;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.http.HttpClient;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.logging.Logger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CompanyProfileServiceTest {

    private static final String MOCK_CONTEXT_ID = "context_id";
    private static final String MOCK_COMPANY_NUMBER = "1234567";
    private static final String MOCK_COMPANY_LINKS_URI = String.format("/company/%s",
            MOCK_COMPANY_NUMBER);
    private CompanyProfileService companyProfileService;
    @Mock
    private Data data;
    @Mock
    private Logger logger;
    @Mock
    private Supplier<InternalApiClient> internalApiClientSupplier;
    @Mock
    private InternalApiClient internalApiClient;
    @Mock
    private HttpClient httpClient;
    @Mock
    private PrivateCompanyResourceHandler companyResourceHandler;
    @Mock
    private PrivateCompanyFullProfileGet privateCompanyFullProfileGet;

    @BeforeEach
    void setup() {
        companyProfileService = spy(new CompanyProfileService(logger, internalApiClientSupplier));
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privateCompanyResourceHandler()).thenReturn(companyResourceHandler);
    }

    @Test
    @DisplayName("Successfully retrieve a company profile")
    void getCompanyProfile() throws ApiErrorResponseException, URIValidationException {
        final ApiResponse<Data> expected = new ApiResponse<>(
                HttpStatus.OK.value(), Collections.emptyMap(), data);

        when(companyResourceHandler.getCompanyFullProfile(MOCK_COMPANY_LINKS_URI)).thenReturn(privateCompanyFullProfileGet);
        when(privateCompanyFullProfileGet.execute()).thenReturn(expected);

        final ApiResponse<Data> response = companyProfileService.getCompanyProfile(
                MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER);

        assertThat(response).isSameAs(expected);
    }

    @Test
    @DisplayName("Given a bad URI when retrieving company profile, return 404 not found")
    void getCompanyProfileBadUri() throws ApiErrorResponseException, URIValidationException {
        when(companyResourceHandler.getCompanyFullProfile(MOCK_COMPANY_LINKS_URI)).thenReturn(privateCompanyFullProfileGet);
        when(privateCompanyFullProfileGet.execute()).thenThrow(new URIValidationException("expected"));

        assertThrows(
                RetryableErrorException.class,
                () -> companyProfileService.getCompanyProfile(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER)
        );
    }

    @Test
    @DisplayName("Given a company number with no matching company profile, return 404 not found")
    void getCompanyProfileNotFound() throws ApiErrorResponseException, URIValidationException {
        final HttpResponseException httpResponseException = new HttpResponseException.Builder(
                HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase(), new
                HttpHeaders()).build();

        when(companyResourceHandler.getCompanyFullProfile(MOCK_COMPANY_LINKS_URI)).thenReturn(privateCompanyFullProfileGet);
        when(privateCompanyFullProfileGet.execute()).thenThrow(
                ApiErrorResponseException.fromHttpResponseException(httpResponseException));

        ApiResponse<Data>  response = companyProfileService.getCompanyProfile(MOCK_CONTEXT_ID,
                MOCK_COMPANY_NUMBER);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Given an internal server error when retrieving a company profile, return 500")
    void getCompanyProfileInternalServerError() throws ApiErrorResponseException,
            URIValidationException {
        final HttpResponseException httpResponseException = new HttpResponseException.Builder(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                new HttpHeaders()).build();

        when(companyResourceHandler.getCompanyFullProfile(MOCK_COMPANY_LINKS_URI)).thenReturn(privateCompanyFullProfileGet);
        when(privateCompanyFullProfileGet.execute()).thenThrow(
                ApiErrorResponseException.fromHttpResponseException(httpResponseException));

        ApiResponse<Data>  response = companyProfileService.getCompanyProfile(MOCK_CONTEXT_ID,
                MOCK_COMPANY_NUMBER);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
