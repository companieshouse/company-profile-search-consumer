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
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
    public void theConsumerReceivesAChangedMessage() throws Exception {
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
                TestData.getCompanyDelta("company-profile-example.json"),
                eventRecord);

        kafkaTemplate.send(topic, resourceChangedData);
        countDown();
    }

    @Then("a putSearchRecord request is sent")
    public void aPutSearchRecordRequestIsSent() {
        verify(requestMadeFor(
                new PutRequestMatcher(
                        String.format("/company-search/companies/%s", companyNumber),
                        TestData.getCompanyDelta("company-profile-example.json"))));
    }

    private void countDown() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(5, TimeUnit.SECONDS );
    }

    @After
    public void shutdownWiremock(){
        if (wireMockServer != null)
            wireMockServer.stop();
    }
}
