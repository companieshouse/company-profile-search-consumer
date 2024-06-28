package uk.gov.companieshouse.companyprofile.search.matcher;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.MatchResult;
import com.github.tomakehurst.wiremock.matching.ValueMatcher;
import uk.gov.companieshouse.api.company.Data;

import java.net.http.HttpRequest;


public class DeleteRequestMatcher implements ValueMatcher<Request> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(Include.NON_EMPTY)
            .registerModule(new JavaTimeModule());
    private final String expectedUrl;

    public DeleteRequestMatcher(String expectedUrl) {
        this.expectedUrl = expectedUrl;

    }
    private MatchResult matchUrl(String actualUrl) {
        return MatchResult.of(expectedUrl.equals(actualUrl));
    }
    private MatchResult matchMethod(RequestMethod actualMethod) {
        return MatchResult.of(RequestMethod.DELETE.equals(actualMethod));
    }

    @Override
    public MatchResult match(Request value) {
        return MatchResult.aggregate(
                matchUrl(value.getUrl()),
                matchMethod(value.getMethod()));
    }
}
