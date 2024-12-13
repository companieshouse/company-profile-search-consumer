package uk.gov.companieshouse.companyprofile.search.steps;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.requestMadeFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import uk.gov.companieshouse.companyprofile.search.data.TestData;
import uk.gov.companieshouse.companyprofile.search.matcher.DeleteRequestMatcher;
import uk.gov.companieshouse.companyprofile.search.matcher.PutRequestMatcher;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;

public class CompanyProfileSearchConsumerSteps {

    private static WireMockServer wireMockServer;
    private static final int WIREMOCK_PORT = 8888;
    private static final String TOPIC = "stream-company-profile";
    private static final String COMPANY_NUMBER = "1234567";
    private static final String TOPIC_PREFIX = "stream-company-profile-company-profile-search-consumer-%s";

    @Autowired
    private Logger logger;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    public KafkaConsumer<String, Object> kafkaConsumer;

    @Value("${company-profile.search.backoff-delay}")
    private int backoff;

    @Given("the application is running")
    public void theApplicationRunning() {
        assertThat(kafkaTemplate).isNotNull();
    }

    @When("the consumer receives a {string} message and the Api returns a {int}")
    public void theConsumerReceivesAMessage(String messageType, int statusCode) throws Exception {
        configureWireMock();

        if (messageType.equals("changed")) {
            stubPutStatement(statusCode);
        } else if (messageType.equals("deleted")) {
            stubDeleteStatement(statusCode);
        }

        ResourceChangedData delta = TestData.getResourceChangedData(
                "src/itest/resources/json/company-profile-example.json", messageType);
        kafkaTemplate.send(TOPIC, delta);
        countDown();
    }

    @When("the consumer receives an invalid payload")
    public void theConsumerReceivesAnInvalidPayload() throws Exception {
        configureWireMock();
        kafkaTemplate.send(TOPIC, "invalid data");
        countDown();
    }

    @Then("a PutSearchRecord request is sent to the SearchApi")
    public void aPutSearchRecordRequestIsSent() {
        verify(requestMadeFor(
                new PutRequestMatcher(
                        String.format("/company-search/companies/%s", COMPANY_NUMBER),
                        TestData.getCompanyDelta("company-profile-example.json"))));
    }

    @Then("a DeleteSearchRecord request is sent to the SearchApi")
    public void aDeleteSearchRecordRequestIsSent() {
        verify(requestMadeFor(
                new DeleteRequestMatcher(
                        String.format("/company-search/companies/%s", COMPANY_NUMBER))));
    }

    @Then("^the message should be moved to the Invalid topic")
    public void theMessageShouldBeMovedToInvalidTopic() {
        ConsumerRecord<String, Object> singleRecord = KafkaTestUtils
                .getSingleRecord(kafkaConsumer, String.format(TOPIC_PREFIX, "invalid"));
        assertThat(singleRecord.value()).isNotNull();
    }

    @Then("the message should retry {int} times and then error")
    public void theMessageShouldRetryTimesAndThenError(int retries) {
        ConsumerRecords<String, Object> records = KafkaTestUtils.getRecords(kafkaConsumer, Duration.ofSeconds(30L), 6);
        Iterable<ConsumerRecord<String, Object>> retryRecords = records.records(String.format(TOPIC_PREFIX, "retry"));
        Iterable<ConsumerRecord<String, Object>> errorRecords = records.records(String.format(TOPIC_PREFIX, "error"));

        int actualRetries = (int) StreamSupport.stream(retryRecords.spliterator(), false).count();
        int errors = (int) StreamSupport.stream(errorRecords.spliterator(), false).count();

        assertThat(actualRetries).isEqualTo(retries);
        assertThat(errors).isEqualTo(1);
    }

    private void configureWireMock() {
        wireMockServer = new WireMockServer(WIREMOCK_PORT);
        wireMockServer.start();
        configureFor("localhost", WIREMOCK_PORT);
    }

    private void stubPutStatement(int responseCode) {
        stubFor(put(urlEqualTo(
                String.format("/company-search/companies/%s", COMPANY_NUMBER)))
                .willReturn(aResponse().withStatus(responseCode)));
    }

    private void stubDeleteStatement(int responseCode) {
        stubFor(delete(urlEqualTo(
                String.format("/company-search/companies/%s", COMPANY_NUMBER)))
                .willReturn(aResponse()
                        .withStatus(responseCode)
                        .withHeader("Content-Type", "application/json")));
    }

    private void countDown() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(5, TimeUnit.SECONDS);
    }

    @After
    public void shutdownWiremock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }
}
