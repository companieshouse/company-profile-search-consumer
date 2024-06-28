package uk.gov.companieshouse.companyprofile.search.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.companieshouse.api.handler.search.company.request.PrivateCompanySearchDelete;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.companyprofile.search.service.api.ApiClientServiceImpl;
import uk.gov.companieshouse.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ApiClientServiceImplTest {

    @Mock
    Logger logger;

    private ApiClientServiceImpl apiClientService;

    @BeforeEach
    void setUp() {
        apiClientService = new ApiClientServiceImpl(logger);
        ReflectionTestUtils.setField(apiClientService, "chsApiKey", "apiKey");
        ReflectionTestUtils.setField(apiClientService, "apiUrl",
                "https://api.companieshouse.gov.uk");
    }

    @Test
    void deleteCompanyProfileSearch() {
        final ApiResponse<Void> expectedResponse = new ApiResponse<>
                (HttpStatus.OK.value(), null, null);
        ApiClientServiceImpl apiClientServiceSpy = Mockito.spy(apiClientService);
        doReturn(expectedResponse).when(apiClientServiceSpy).executeOp(anyString(), anyString(), anyString(), any(PrivateCompanySearchDelete.class));

        ApiResponse<Void> response =
                apiClientServiceSpy.deleteCompanyProfileSearch("context_id", "12345678");
        verify(apiClientServiceSpy).executeOp(anyString(),
                eq("deleteCompanyProfileSearch"),
                eq("/primary-search/companies/" + "12345678"),
                any(PrivateCompanySearchDelete.class));
        assertThat(response).isEqualTo(expectedResponse);
    }
}
