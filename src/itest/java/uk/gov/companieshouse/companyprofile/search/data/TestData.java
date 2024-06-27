package uk.gov.companieshouse.companyprofile.search.data;

import org.springframework.util.FileCopyUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class TestData {

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
}
