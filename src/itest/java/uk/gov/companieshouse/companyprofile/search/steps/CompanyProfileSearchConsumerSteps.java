package uk.gov.companieshouse.companyprofile.search.steps;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import uk.gov.companieshouse.companyprofile.search.data.TestData;
import uk.gov.companieshouse.companyprofile.search.matcher.PutRequestMatcher;
import uk.gov.companieshouse.delta.ChsDelta;
import uk.gov.companieshouse.logging.Logger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

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
        ChsDelta delta = new ChsDelta(TestData.getCompanyDelta("company-profile-delta.json"), 1, contextId, false);
        kafkaTemplate.send(topic, delta);
    }

    @Then("a putSearchRecord request is sent")
    public void aPutSearchRecordRequestIsSent() {
        verify(requestMadeFor(
                new PutRequestMatcher(
                        String.format("/company-search/companies/%s", companyNumber),
                        TestData.getCompanyDelta("company-profile-delta.json"))));
    }

    @After
    public void shutdownWiremock(){
        if (wireMockServer != null)
            wireMockServer.stop();
    }
}
