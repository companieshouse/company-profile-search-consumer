package uk.gov.companieshouse.companyprofile.search.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.companyprofile.search.data.TestData;
import uk.gov.companieshouse.companyprofile.search.matcher.DeleteRequestMatcher;
import uk.gov.companieshouse.companyprofile.search.matcher.PutRequestMatcher;
import uk.gov.companieshouse.delta.ChsDelta;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.EventRecord;
import uk.gov.companieshouse.stream.ResourceChangedData;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;


public class CompanyProfileSearchConsumerSteps {

    private static WireMockServer wireMockServer;

    @Autowired
    private Logger logger;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    public KafkaConsumer<String, Object> kafkaConsumer;
    @Value("${company-profile.search.topic:stream-company-profile}")
    private String topic;
    @Value("${wiremock.port:8888}")
    private String port;

    private final String companyNumber = "1234567";

    public void sendMsgToKafkaTopic(String data) {
        kafkaTemplate.send(topic, data);
    }

    private int statusCode;

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
    public void theConsumerReceivesAChangedMessage() throws IOException {
        configureWireMock();
        stubFor(put(urlEqualTo(
                String.format("/company-search/companies/%s", companyNumber)))
                .willReturn(aResponse().withStatus(200)));

        String data = FileCopyUtils.copyToString(new InputStreamReader(
                new FileInputStream("src/itest/resources/json/resource-changed-message.json")));

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        ResourceChangedData resourceChangedData = objectMapper.readValue(data, ResourceChangedData.class);

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
                "src/itest/resources/json/company-profile-example.json", "deleted");

        kafkaTemplate.send(topic, delta);
        countDown();

    }

    @Then("a DELETE request is sent to the search Api")
    public void aDELETERequestIsSentToTheSearchApi() throws Exception {
        verify(requestMadeFor(
                new DeleteRequestMatcher(
                        String.format("/company-search/companies/%s", companyNumber))));
        countDown();
    }


    private void countDown() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await(5, TimeUnit.SECONDS );
    }

    @When("the consumer receives an invalid delete payload")
    public void theConsumerReceivesAnInvalidDeletePayload() throws Exception {
        configureWireMock();
        ResourceChangedData delta = TestData.getResourceChangedData(
                "src/itest/resources/json/company-profile-invalid.json", "deleted");
        kafkaTemplate.send("stream-company-profile-company-profile-search-consumer-invalid", delta);

        countDown();
    }

    @Then("^the message should be moved to topic stream-company-profile-company-profile-search-consumer-invalid")
    public void theMessageShouldBeMovedToTopic() {
        ConsumerRecord<String, Object> singleRecord = KafkaTestUtils
                .getSingleRecord(kafkaConsumer, "stream-company-profile-company-profile-search-consumer-invalid");

        assertThat(singleRecord.value()).isNotNull();
    }

    private void stubDeleteStatement(int responseCode) {
        stubFor(delete(urlEqualTo(
                "/company-search/companies/1234567"))
                .willReturn(aResponse().withStatus(responseCode)));
    }

    @And("The user is unauthorized")
    public void stubUnauthorizedPatchRequest() {
        statusCode = HttpStatus.UNAUTHORIZED.value();
    }


    @When("the consumer receives a delete message but the api returns a 401")
    public void theConsumerReceivesADeleteMessageButTheApiReturnsA() throws Exception {
        configureWireMock();
        stubDeleteStatement(statusCode);
        logger.info("ST: " + statusCode);
        ResourceChangedData delta = TestData.getResourceChangedData(
                "src/itest/resources/json/company-profile-invalid.json", "deleted");
        kafkaTemplate.send("stream-company-profile-company-profile-search-consumer-invalid", delta);

        countDown();
    }

    @When("the consumer receives a delete message but the api will return 400")
    public void theConsumerReceivesADeleteMessageButTheApiWillReturn() throws Exception {
        configureWireMock();
        stubDeleteStatement(statusCode);
        ResourceChangedData delta = TestData.getResourceChangedData(
                "src/itest/resources/json/company-profile-invalid.json", "deleted");
        kafkaTemplate.send("stream-company-profile-company-profile-search-consumer-invalid", delta);

        countDown();
    }

    @And("the search API and the api.ch.gov.uk is unavailable")
    public void theSearchAPIAndTheApiChGovUkIsUnavailable() {
        statusCode = HttpStatus.UNAUTHORIZED.value();
    }

    @When("the consumer receives a delete message but the api returns a 503")
    public void theConsumerReceivesADeleteMessageButTheApiReturnsA503() throws Exception {
        configureWireMock();
        stubDeleteStatement(statusCode);
        ResourceChangedData delta = TestData.getResourceChangedData(
                "src/itest/resources/json/company-profile-invalid.json", "deleted");
        kafkaTemplate.send("stream-company-profile-company-profile-search-consumer-retry", delta);

        countDown();
    }

    @Then("the message should be moved to topic stream-company-profile-company-profile-search-consumer-retry")
    public void theMessageShouldBeMovedToTopicStreamCompanyProfileCompanyProfileSearchConsumerRetry() {
        ConsumerRecord<String, Object> singleRecord = KafkaTestUtils
                .getSingleRecord(kafkaConsumer, "stream-company-profile-company-profile-search-consumer-retry");

        assertThat(singleRecord.value()).isNotNull();

    }


    @When("^the consumer receives a delete message but the api should return a (\\d*)$")
    public void theConsumerReceivesDeleteMessageButDataApiShouldReturn(int responseCode) throws Exception{
        configureWireMock();
        stubDeleteStatement(responseCode);
        ResourceChangedData delta = TestData.getResourceChangedData(
                "src/itest/resources/json/company-profile-example.json", "deleted");
        kafkaTemplate.send(topic, delta);

        countDown();
    }

    @Then("the message should retry {int} times and then error")
    public void theMessageShouldRetryTimesAndThenError(int retries) {
        ConsumerRecords<String, Object> records = KafkaTestUtils.getRecords(kafkaConsumer);
        Iterable<ConsumerRecord<String, Object>> retryRecords =  records.records("stream-company-profile-company-profile-search-consumer-retry");
        Iterable<ConsumerRecord<String, Object>> errorRecords =  records.records("stream-company-profile-company-profile-search-consumer-error");

        int actualRetries = (int) StreamSupport.stream(retryRecords.spliterator(), false).count();
        int errors = (int) StreamSupport.stream(errorRecords.spliterator(), false).count();

        assertThat(actualRetries).isEqualTo(retries);
        assertThat(errors).isEqualTo(1);

    }

    @When("the consumer receives a message that causes an error")
    public void theConsumerReceivesAMessageThatCausesAnError() throws Exception {
        configureWireMock();
        ResourceChangedData delta = TestData.getResourceChangedData(
                "src/itest/resources/json/company-profile-example.json", "deleted");
        kafkaTemplate.send(topic, delta);


        countDown();

    }
}
