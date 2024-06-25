package uk.gov.companieshouse.companyprofile.search.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class TestHelper {

    public Message<ResourceChangedData> createCompanyProfileResourceChanged(){
        ResourceChangedData resourceChangedData = new ResourceChangedData();
        resourceChangedData.setContextId("contextId");
        resourceChangedData.setResourceId("companyNumber");
        return new GenericMessage<>(resourceChangedData);
    }

    public Data createCompanyProfileData() throws IOException {
        String data = FileCopyUtils.copyToString(new InputStreamReader(
                new FileInputStream("src/test/resources/company-profile-example.json")));
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        return objectMapper.readValue(data, Data.class);
    }
}
