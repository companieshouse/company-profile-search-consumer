package uk.gov.companieshouse.companyprofile.search.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.companyprofile.search.service.CompanyProfileService;
import uk.gov.companieshouse.companyprofile.search.service.api.ApiClientService;
import uk.gov.companieshouse.companyprofile.search.util.TestHelper;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.io.IOException;
import java.util.HashMap;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SearchProcessorTest {

    private SearchProcessor searchProcessor;
    private TestHelper testHelper;
    @Mock
    private Logger logger;
    @Mock
    private ApiClientService apiClientService;
    @Mock
    private CompanyProfileService companyProfileService;

    @BeforeEach
    void setUp() {
        searchProcessor = new SearchProcessor(logger, apiClientService, companyProfileService);
        testHelper = new TestHelper();
    }

    @Test
    @DisplayName("Processes a Company Profile ResourceChanged message")
    void processResourceChangedMessage() throws IOException {

        Message<ResourceChangedData> resourceChangedMessage = testHelper.createCompanyProfileResourceChanged();
        String contextId = resourceChangedMessage.getPayload().getContextId();
        String companyNumber = resourceChangedMessage.getPayload().getResourceId();
        Data companyProfileData = testHelper.createCompanyProfileData();

        when(companyProfileService.getCompanyProfile(contextId, companyNumber)).thenReturn(
                new ApiResponse<>(200, new HashMap<>(), companyProfileData));

        searchProcessor.processChangedMessage(resourceChangedMessage);

        verify(apiClientService).putSearchRecord(contextId, companyNumber, companyProfileData);
    }
}
