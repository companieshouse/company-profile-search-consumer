package uk.gov.companieshouse.companyprofile.search.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

public class TestHelper {

    private static final String MOCK_COMPANY_NUMBER = "1234567";
    private static final String MOCK_CONTEXT_ID = "context_id";

    public Message<@NonNull ResourceChangedData> createCompanyProfileMessage(String type) throws IOException {
        String data = FileCopyUtils.copyToString(new InputStreamReader(
                new FileInputStream("src/test/resources/company-profile-example.json")));

        EventRecord eventRecord = new EventRecord();
        eventRecord.setType(type);
        eventRecord.setPublishedAt("");

        ResourceChangedData mockResourceChangedData =
                ResourceChangedData.newBuilder()
                        .setData(data)
                        .setContextId(MOCK_CONTEXT_ID)
                        .setResourceId(MOCK_COMPANY_NUMBER)
                        .setResourceKind("company-profile")
                        .setResourceUri(String.format("/company-search/companies/%s", MOCK_COMPANY_NUMBER))
                        .setEvent(eventRecord)
                        .build();
        return MessageBuilder
                .withPayload(mockResourceChangedData)
                .setHeader(KafkaHeaders.RECEIVED_TOPIC, "test")
                .setHeader("CHANGED_RESOURCE_RETRY_COUNT", 1)
                .build();
    }

    public Message<@NonNull ResourceChangedData> createCompanyProfileInvalidMessage(){
        return new GenericMessage<>(new ResourceChangedData());
    }

    public Data createCompanyProfileData() throws IOException {
        String data = FileCopyUtils.copyToString(new InputStreamReader(
                new FileInputStream("src/test/resources/company-profile-example.json")));
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        return objectMapper.readValue(data, Data.class);
    }

    public ResourceChangedData createBasicPayload() {
        EventRecord event = new EventRecord();
        event.setFieldsChanged(List.of("field1", "field2"));
        event.setType("type");
        event.setPublishedAt("published_at");

        ResourceChangedData payload = new ResourceChangedData();
        payload.setResourceKind("resource_kind");
        payload.setResourceUri("resource_uri");
        payload.setResourceId("resource_id");
        payload.setContextId("context_id");
        payload.setData("");
        payload.setEvent(event);

        return payload;
    }
}
