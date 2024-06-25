package uk.gov.companieshouse.companyprofile.search.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.companyprofile.search.service.CompanyProfileService;
import uk.gov.companieshouse.companyprofile.search.service.api.ApiClientService;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.io.IOException;
import java.io.InputStreamReader;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SearchProcessorTest {

    private SearchProcessor searchProcessor;
    @Mock
    private Logger logger;
    @Mock
    private ApiClientService apiClientService;
    @Mock
    private CompanyProfileService companyProfileService;

    @BeforeEach
    void setUp() {
        searchProcessor = new SearchProcessor(
                logger,
                apiClientService,
                companyProfileService);
    }

    @Test
    @DisplayName("Transforms a kafka message containing a delete payload into a search api request")
    void expectValidCompanyProfileDeleteWhenValidMessage() throws IOException {
        Message<ResourceChangedData> mockResourceChangedData = createChsMessage("deleted");

        searchProcessor.processChangedMessage(mockResourceChangedData);

        verify(apiClientService).deleteCompanyProfileSearch("context_id", "12345678");
    }

    private Message<ResourceChangedData> createChsMessage(String type) throws IOException {
        InputStreamReader exampleJsonPayload = new InputStreamReader(
                ClassLoader.getSystemClassLoader().getResourceAsStream("company-profile-search-example.json"));
        String data = FileCopyUtils.copyToString(exampleJsonPayload);

        EventRecord eventRecord = new EventRecord();
        eventRecord.setType(type);
        eventRecord.setPublishedAt("");

        String companyId = "12345678";

        ResourceChangedData mockResourceChangedData =
                ResourceChangedData.newBuilder()
                        .setData(data).setContextId("context_id")
                        .setResourceId(companyId)
                        .setResourceKind("company-profile")
                        .setResourceUri(String.format("/primary-search/companies/%s", companyId))
                        .setEvent(eventRecord)
                        .build();
        return MessageBuilder
                .withPayload(mockResourceChangedData)
                .setHeader(KafkaHeaders.RECEIVED_TOPIC, "test")
                .setHeader("CHANGED_RESOURCE_RETRY_COUNT", 1)
                .build();
    }
}
