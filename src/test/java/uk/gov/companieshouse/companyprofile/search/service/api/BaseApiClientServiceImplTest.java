package uk.gov.companieshouse.companyprofile.search.service.api;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException.Builder;
import consumer.exception.NonRetryableErrorException;
import consumer.exception.RetryableErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.Executor;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.logging.Logger;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseApiClientServiceImplTest {

    private BaseApiClientServiceImpl service;

    @Mock
    private Logger logger;
    @Mock
    private Executor<ApiResponse<Data>> executor;

    @BeforeEach
    void setup() {
        service = new BaseApiClientServiceImpl(logger) {};
    }

    @Test
    void returnsApiResponse() throws Exception {
        ApiResponse<Data> expectedResponse = new ApiResponse<>(200, new HashMap<>());
        when(executor.execute()).thenReturn(expectedResponse);

        ApiResponse<Data> actualResponse = service.executeOp(null, null, null, executor);

        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    void throwsRetryableErrorOn404() throws Exception {
        when(executor.execute()).thenThrow(new URIValidationException("Not Found"));

        RetryableErrorException thrown = assertThrows(RetryableErrorException.class,
                () -> service.executeOp(null, null, null, executor));

        assertThat(thrown.getMessage()).isEqualTo("404 NOT_FOUND response received");
    }

    @Test
    void throwsRetryableErrorOn500() throws Exception {
        when(executor.execute()).thenThrow(
                new ApiErrorResponseException(new Builder(500, "500", new HttpHeaders())));

        RetryableErrorException thrown = assertThrows(RetryableErrorException.class,
                () -> service.executeOp(null, null, null, executor));

        assertThat(thrown.getMessage()).isEqualTo("Non-Successful response received");
    }

    @Test
    void throwsNonRetryableErrorOn400() throws Exception {
        when(executor.execute()).thenThrow(
                new ApiErrorResponseException(new Builder(400, "400", new HttpHeaders())));

        NonRetryableErrorException thrown = assertThrows(NonRetryableErrorException.class,
                () -> service.executeOp(null, null, null, executor));

        assertThat(thrown.getMessage()).isEqualTo("400 BAD_REQUEST response received");
    }

}
