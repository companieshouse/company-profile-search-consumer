package uk.gov.companieshouse.companyprofile.search.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import consumer.exception.RetryableErrorException;
import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.companyprofile.search.deserialiser.CompanyProfileDeserialiser;
import uk.gov.companieshouse.companyprofile.search.service.ApiClientService;
import uk.gov.companieshouse.companyprofile.search.util.TestHelper;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;

@ExtendWith(MockitoExtension.class)
class SearchProcessorTest {

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
        String companyNumber = resourceChangedMessage.getPayload().getResourceId();
        Data companyProfileData = testHelper.createCompanyProfileData();

        when(companyProfileDeserialiser.deserialiseCompanyProfile(any())).thenReturn(companyProfileData);

        searchProcessor.processChangedMessage(resourceChangedMessage);

        verify(apiClientService).putSearchRecord(companyNumber, companyProfileData);
    }

    @Test
    @DisplayName("Confirms a Retryable Error is throws when the ResourceChangedData message is invalid")
    void invalidResourceChangedMessageThrowsRetryableError() {
        Message<ResourceChangedData> invalidMessage = testHelper.createCompanyProfileInvalidMessage();

        Assertions.assertThrows(RetryableErrorException.class,
                () -> searchProcessor.processChangedMessage(invalidMessage));

        verifyNoInteractions(apiClientService);
    }

    @Test
    @DisplayName("Processes a delete Company Profile ResourceChanged message")
    void deleteResourceChangedMessage() throws IOException {
        Message<ResourceChangedData> resourceChangedMessage = testHelper.createCompanyProfileMessage("deleted");
        String companyNumber = resourceChangedMessage.getPayload().getResourceId();

        searchProcessor.processDeleteMessage(resourceChangedMessage);

        verify(apiClientService).deleteCompanyProfileSearch(companyNumber);
    }

    @Test
    @DisplayName("Confirms a Retryable Error is throws when the delete message is invalid")
    void invalidResourceDeletedMessageThrowsRetryableError() {
        Message<ResourceChangedData> invalidMessage = testHelper.createCompanyProfileInvalidMessage();

        Assertions.assertThrows(RetryableErrorException.class,
                () -> searchProcessor.processDeleteMessage(invalidMessage));

        verifyNoInteractions(apiClientService);
    }

    @Test
    @DisplayName("Delete Retryable Exception test")
    void deleteRetryableExceptionTest() throws IOException {
        Message<ResourceChangedData> resourceChangedMessage = testHelper.createCompanyProfileMessage("deleted");
        String companyNumber = resourceChangedMessage.getPayload().getResourceId();
        doThrow(RetryableErrorException.class).when(apiClientService).deleteCompanyProfileSearch(anyString());
        for (int i = 0; i < 4; i++) {
            try {
                searchProcessor.processDeleteMessage(resourceChangedMessage);
            } catch (RetryableErrorException ignored) {
            }
        }
        Assertions.assertThrows(RetryableErrorException.class,
                () -> searchProcessor.processDeleteMessage(resourceChangedMessage));
        verify(apiClientService, times(5)).deleteCompanyProfileSearch(companyNumber);
    }

}
