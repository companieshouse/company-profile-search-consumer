package uk.gov.companieshouse.companyprofile.search.processor;

import consumer.exception.NonRetryableErrorException;
import consumer.exception.RetryableErrorException;
import java.io.IOException;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.companyprofile.search.deserialiser.CompanyProfileDeserialiser;
import uk.gov.companieshouse.companyprofile.search.service.api.ApiClientService;
import uk.gov.companieshouse.companyprofile.search.util.TestHelper;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    CompanyProfileDeserialiser companyProfileDeserialiser;

    @BeforeEach
    void setUp() {
        searchProcessor = new SearchProcessor(logger, apiClientService, companyProfileDeserialiser);
        testHelper = new TestHelper();
    }

    @Test
    @DisplayName("Processes a Company Profile ResourceChanged message")
    void processResourceChangedMessage() throws IOException {

        Message<ResourceChangedData> resourceChangedMessage = testHelper.createCompanyProfileMessage("changed");
        String contextId = resourceChangedMessage.getPayload().getContextId();
        String companyNumber = resourceChangedMessage.getPayload().getResourceId();
        Data companyProfileData = testHelper.createCompanyProfileData();

        when(companyProfileDeserialiser.deserialiseCompanyProfile(any())).thenReturn(companyProfileData);

        searchProcessor.processChangedMessage(resourceChangedMessage);

        verify(apiClientService).putSearchRecord(contextId, companyNumber, companyProfileData);
    }

    @Test
    @DisplayName("Confirms a Non Retryable Error is throws when the ResourceChangedData message is invalid")
    void invalidResourceChangedMessageThrowsNonRetryableError() {
        Message<ResourceChangedData> invalidMessage = testHelper.createCompanyProfileInvalidMessage();

        Assertions.assertThrows(NonRetryableErrorException.class,
                () -> searchProcessor.processChangedMessage(invalidMessage));

        verifyNoInteractions(apiClientService);
    }

    @Test
    @DisplayName("Processes a delete Company Profile ResourceChanged message")
    void deleteResourceChangedMessage() throws IOException {

        Message<ResourceChangedData> resourceChangedMessage = testHelper.createCompanyProfileMessage("deleted");
        String contextId = resourceChangedMessage.getPayload().getContextId();
        String companyNumber = resourceChangedMessage.getPayload().getResourceId();

        when(apiClientService.deleteCompanyProfileSearch(contextId, companyNumber)).thenReturn(
                new ApiResponse<>(200, new HashMap<>()));

        searchProcessor.processDeleteMessage(resourceChangedMessage);

        verify(apiClientService).deleteCompanyProfileSearch(contextId, companyNumber);
    }

    @Test
    @DisplayName("Confirms a Non Retryable Error is throws when the delete message is invalid")
    void invalidResourceDeletedMessageThrowsNonRetryableError() {
        Message<ResourceChangedData> invalidMessage = testHelper.createCompanyProfileInvalidMessage();

        Assertions.assertThrows(NonRetryableErrorException.class,
                () -> searchProcessor.processDeleteMessage(invalidMessage));

        verifyNoInteractions(apiClientService);
    }

    @Test
    @DisplayName("Delete Retryable Exception test")
    void deleteRetryableExceptionTest() throws IOException {
        Message<ResourceChangedData> resourceChangedMessage = testHelper.createCompanyProfileMessage("deleted");
        String contextId = resourceChangedMessage.getPayload().getContextId();
        String companyNumber = resourceChangedMessage.getPayload().getResourceId();
        when(apiClientService.deleteCompanyProfileSearch(contextId, companyNumber)).thenThrow(new RetryableErrorException("Retries Exceeded"));
        for (int i = 0; i< 4; i++){
            try {
                searchProcessor.processDeleteMessage(resourceChangedMessage);
            } catch (RetryableErrorException ignored) {
            }
        }
        Assertions.assertThrows(RetryableErrorException.class,
                () -> searchProcessor.processDeleteMessage(resourceChangedMessage));
        verify(apiClientService, times(5)).deleteCompanyProfileSearch(anyString(),anyString());
    }

}
