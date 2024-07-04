package uk.gov.companieshouse.companyprofile.search.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class TestHelper {

    private static final String MOCK_COMPANY_NUMBER = "1234567";
    private static final String MOCK_CONTEXT_ID = "context_id";

    public Message<ResourceChangedData> createCompanyProfileResourceChangedMessage() throws IOException {
        String data = FileCopyUtils.copyToString(new InputStreamReader(
                new FileInputStream("src/test/resources/resource-changed-message.json")));

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        ResourceChangedData mockResourceChangedData = objectMapper.readValue(data, ResourceChangedData.class);

        return MessageBuilder
                .withPayload(mockResourceChangedData)
                .setHeader(KafkaHeaders.RECEIVED_TOPIC, "stream-company-profile")
                .setHeader("CHANGED_RESOURCE_RETRY_COUNT", 1)
                .build();
    }

    public Message<ResourceChangedData> createCompanyProfileInvalidMessage(){
        return new GenericMessage<>(new ResourceChangedData());
    }

    public Data createCompanyProfileData() throws IOException {
        String data = FileCopyUtils.copyToString(new InputStreamReader(
                new FileInputStream("src/test/resources/company-profile-example.json")));
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        return objectMapper.readValue(data, Data.class);
    }
}
