package uk.gov.companieshouse.companyprofile.search.data;

import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import static org.codehaus.plexus.util.FileUtils.loadFile;

public class TestData {

    public static final String CONTEXT_ID = "context_id";
    public static final String RESOURCE_ID = "1234567";
    public static final String RESOURCE_KIND = "primary-search";
    public static final String COMPANY_SEARCH_RESOURCE_URI = "company-search/companies/1234567";

    public static String getCompanyDelta(String inputFile) {
        String path = "src/itest/resources/json/" + inputFile;
        return readFile(path);
    }

    private static String readFile(String path) {
        String data;
        try {
            data = FileCopyUtils.copyToString(new InputStreamReader(new FileInputStream(path)));
        } catch (IOException e) {
            data = null;
        }
        return data;
    }

    public static ResourceChangedData getResourceChangedData(String fileName, String type) {
        EventRecord event = EventRecord.newBuilder()
                .setType(type)
                .setPublishedAt("2022-02-22T10:51:30")
                .setFieldsChanged(Arrays.asList("address", "court_name"))
                .build();
        return createResourceChangedData(event, readFile(fileName));
    }

    private static ResourceChangedData createResourceChangedData(EventRecord event, String disqOfficerData) {
        return ResourceChangedData.newBuilder()
                .setContextId(CONTEXT_ID)
                .setResourceId(RESOURCE_ID)
                .setResourceKind(RESOURCE_KIND)
                .setResourceUri(COMPANY_SEARCH_RESOURCE_URI)
                .setData(disqOfficerData)
                .setEvent(event)
                .build();
    }
}
