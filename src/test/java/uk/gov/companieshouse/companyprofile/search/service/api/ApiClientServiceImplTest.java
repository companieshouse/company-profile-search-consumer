package uk.gov.companieshouse.companyprofile.search.service.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.handler.search.company.request.PrivateCompanySearchUpsert;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ApiClientServiceImplTest {

    private final String contextId = "testContext";
    private final String companyNumber = "test12345";
    private final String uri = "/company-search/companies/%s";

    private ApiClientServiceImpl apiClientService;

    @Mock
    private Logger logger;

    @BeforeEach
    void setUp(){
        apiClientService = new ApiClientServiceImpl(logger);
        ReflectionTestUtils.setField(apiClientService, "chsApiKey", "testKey");
        ReflectionTestUtils.setField(apiClientService, "apiUrl", "http://localhost:8888");
    }

    @Test
    void returnOkResponseWhenValidPutRequestSentToApi(){
        final ApiResponse<Void> expectedResponse = new ApiResponse<>(HttpStatus.OK.value(), null, null);
        String expectedUri = String.format(uri, companyNumber);
        ApiClientServiceImpl apiClientServiceSpy = Mockito.spy(apiClientService);
        doReturn(expectedResponse).when(apiClientServiceSpy).executeOp(anyString(), anyString(),
                anyString(),
                any(PrivateCompanySearchUpsert.class));

        ApiResponse<Void> response = apiClientServiceSpy.putSearchRecord(contextId,
                companyNumber,
                new Data());

        verify(apiClientServiceSpy).executeOp(anyString(), eq("putSearchRecord"),
                eq(expectedUri),
                any(PrivateCompanySearchUpsert.class));

        assertThat(response).isEqualTo(expectedResponse);
    }
}
