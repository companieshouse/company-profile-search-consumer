package uk.gov.companieshouse.companyprofile.search.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.gov.companieshouse.stream.ResourceChangedData;

public class Helper {

    /**
     * Extracts the company number from the company profile ResourceURI.
     */
    public static String extractCompanyNumber(ResourceChangedData resourceChangedData) {
        String companyNumber = null;
        if (resourceChangedData.getResourceUri() != null) {
            Pattern pattern = Pattern.compile("(?<=company/)(.*)");
            Matcher matcher = pattern.matcher(resourceChangedData.getResourceUri());
            if (matcher.find()) {
                companyNumber = matcher.group();
            }
        }
        return companyNumber;
    }
}
