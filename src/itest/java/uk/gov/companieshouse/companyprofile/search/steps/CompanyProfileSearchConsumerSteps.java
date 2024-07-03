package uk.gov.companieshouse.companyprofile.search.steps;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import uk.gov.companieshouse.companyprofile.search.data.TestData;
import uk.gov.companieshouse.companyprofile.search.matcher.DeleteRequestMatcher;
import uk.gov.companieshouse.companyprofile.search.matcher.PutRequestMatcher;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class CompanyProfileSearchConsumerSteps {

    private static WireMockServer wireMockServer;

    @Autowired
    private Logger logger;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    public KafkaConsumer<String, Object> kafkaConsumer;
    @Value("${company-profile.search.topic}")
    private String topic;
    @Value("${wiremock.server.port:8888}")
    private String port;

    private final String contextId = "123456789";
    private final String companyNumber = "1234567";

    public void sendMsgToKafkaTopic(String data) {
        kafkaTemplate.send(topic, data);
    }

    private void configureWireMock() {
        wireMockServer = new WireMockServer(Integer.parseInt(port));
        wireMockServer.start();
        configureFor("localhost", Integer.parseInt(port));
    }

    @Given("the application is running")
    public void theApplicationRunning() {
        assertThat(kafkaTemplate).isNotNull();
    }

    @When("the consumer receives a changed message")
    public void theConsumerReceivesAChangedMessage() {
        configureWireMock();
        stubFor(put(urlEqualTo(
                String.format("/company-search/companies/%s", companyNumber)))
                .willReturn(aResponse().withStatus(200)));

        EventRecord eventRecord = new EventRecord(
                "published_at",
                "changed",
                List.of("fields_changed"));
        ResourceChangedData resourceChangedData = new ResourceChangedData(
                "resource_kind",
                "resource_uri",
                contextId,
                companyNumber,
                TestData.getCompanyDelta("company-profile-delta.json"),
                eventRecord);

        kafkaTemplate.send(topic, resourceChangedData);
    }

    @Then("a putSearchRecord request is sent")
    public void aPutSearchRecordRequestIsSent() {
        verify(requestMadeFor(
                new PutRequestMatcher(
                        String.format("/company-search/companies/%s", companyNumber),
                        TestData.getCompanyDelta("company-profile-example.json"))));
    }

    @After
    public void shutdownWiremock(){
        if (wireMockServer != null)
            wireMockServer.stop();
    }

    @When("the consumer receives a delete payload")
    public void theConsumerReceivesADeletePayload() throws Exception {
        configureWireMock();
        stubFor(delete(urlEqualTo("/company-search/companies/" + companyNumber))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")));
        ResourceChangedData delta = TestData.getResourceChangedData(
                "src/itest/resources/json/company-profile-delta.json", "deleted");

        //ChsDelta delta = new ChsDelta(TestData.getCompanyDelta("company-profile-delta.json"), 1, contextId, true);

        kafkaTemplate.send(topic, delta);
        countDown();

    }

/*
    @Then("a DELETE request is sent to the search Api")
    public void aDELETERequestIsSentToTheSearchApi() {
        verify(requestMadeFor(
                new DeleteRequestMatcher(
                        String.format("/company-search/companies/%s", companyNumber))));
    }
*/

    private List<ServeEvent> getServeEvents() {
        return wireMockServer != null ? wireMockServer.getAllServeEvents() :
                new ArrayList<>();
    }

    @Then("a DELETE request is sent to the search Api")
    public void deleteRequestIsSentToTheSearchApi() {
        List<ServeEvent> serverEvents = getServeEvents();
        assertThat(serverEvents.isEmpty()).isFalse();
        assertThat(serverEvents.size()).isEqualTo(1);
        //assertThat(serverEvents.get(0).getRequest().getUrl())
                //.isEqualTo(String.format("/company-search/companies/%s", companyNumber));
        assertEquals(serverEvents.get(0).getRequest().getUrl()
                ,String.format("/company-search/companies/%s", companyNumber));

        verify(1, deleteRequestedFor(urlMatching("/company-search/companies/" + companyNumber)));
        assertThat(serverEvents.get(0).getResponse().getStatus()).isEqualTo(200);
    }

    private void countDown() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        //countDownLatch.await(5, TimeUnit.SECONDS);
    }
}
