package uk.gov.companieshouse.companyprofile.search.steps;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.requestMadeFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Duration;
import java.util.stream.StreamSupport;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import uk.gov.companieshouse.companyprofile.search.data.TestData;
import uk.gov.companieshouse.companyprofile.search.matcher.PutRequestMatcher;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.stream.ResourceChangedData;

public class CompanyProfileSearchConsumerSteps {

    private static final String COMPANY_NUMBER = "1234567";

    private final WireMockServer wireMockServer;
    private final Logger logger;
    private final KafkaTemplate<@NonNull String, @NonNull Object> kafkaTemplate;
    private final KafkaConsumer<String, Object> kafkaConsumer;

    public CompanyProfileSearchConsumerSteps(WireMockServer wireMockServer,
            Logger logger,
            KafkaTemplate<@NonNull String, @NonNull Object> kafkaTemplate,
            KafkaConsumer<String, Object> kafkaConsumer) {
        this.wireMockServer = wireMockServer;
        this.logger = logger;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaConsumer = kafkaConsumer;
    }

    @Value("${company-profile.search.topic}")
    private String topic;

    @Value("${company-profile.search.group-id}")
    private String groupId;

    @Before
    public void resetWireMock() {
        wireMockServer.resetAll();
    }

    @Given("the application is running")
    public void theApplicationRunning() {
        assertThat(kafkaTemplate).isNotNull();
        assertThat(wireMockServer.isRunning()).isTrue();
    }

    @When("the consumer receives a {string} message and the Api returns a {int}")
    public void theConsumerReceivesAMessage(String messageType, int statusCode) throws Exception {
        logger.info("theConsumerReceivesAMessage(type=%s, code=%d) method called...".formatted(messageType, statusCode));

        if (messageType.equals("changed")) {
            stubPutStatement(statusCode);

        } else if (messageType.equals("deleted")) {
            stubDeleteStatement(statusCode);
        }

        ResourceChangedData delta = TestData.getResourceChangedData(
                "src/itest/resources/json/company-profile-example.json", messageType);

        kafkaTemplate.send(topic, delta);
    }

    @When("the consumer receives an invalid payload")
    public void theConsumerReceivesAnInvalidPayload() throws Exception {
        kafkaTemplate.send(topic, "invalid data");
    }

    @Then("a PutSearchRecord request is sent to the SearchApi")
    public void aPutSearchRecordRequestIsSent() {
        String companySearchUri = String.format("/company-search/companies/%s", COMPANY_NUMBER);
        String companyDelta = TestData.getCompanyDelta("company-profile-example.json");

        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        wireMockServer.verify(requestMadeFor(
                                new PutRequestMatcher(companySearchUri, companyDelta))
                        )
                );
    }

    @Then("a DeleteSearchRecord request is sent to the SearchApi")
    public void aDeleteSearchRecordRequestIsSent() {
        String companySearchUri = String.format("/company-search/companies/%s", COMPANY_NUMBER);

        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() ->
                        wireMockServer.verify(
                                1,
                                deleteRequestedFor(urlEqualTo(companySearchUri))
                        )
                );
    }

    @Then("^the message should be moved to the Invalid topic")
    public void theMessageShouldBeMovedToInvalidTopic() {
        ConsumerRecord<String, Object> singleRecord = KafkaTestUtils.getSingleRecord(
                kafkaConsumer, "%s-%s-invalid".formatted(topic, groupId));

        assertThat(singleRecord.value()).isNotNull();
    }

    @Then("the message should retry {int} times and then error")
    public void theMessageShouldRetryTimesAndThenError(int retries) {
        ConsumerRecords<String, Object> records = KafkaTestUtils.getRecords(kafkaConsumer, Duration.ofSeconds(10L), 6);
        Iterable<ConsumerRecord<String, Object>> retryRecords = records.records("%s-%s-retry".formatted(topic, groupId));
        Iterable<ConsumerRecord<String, Object>> errorRecords = records.records("%s-%s-error".formatted(topic, groupId));

        int actualRetries = (int) StreamSupport.stream(retryRecords.spliterator(), false).count();
        int errors = (int) StreamSupport.stream(errorRecords.spliterator(), false).count();

        assertThat(actualRetries).isEqualTo(retries);
        assertThat(errors).isEqualTo(1);
    }

    private void stubPutStatement(int responseCode) {
        String companySearchUri = String.format("/company-search/companies/%s", COMPANY_NUMBER);

        wireMockServer.stubFor(put(urlEqualTo(companySearchUri))
                .willReturn(aResponse()
                        .withStatus(responseCode)
                )
        );
    }

    private void stubDeleteStatement(int responseCode) {
        String companySearchUri = String.format("/company-search/companies/%s", COMPANY_NUMBER);

        wireMockServer.stubFor(delete(urlEqualTo(companySearchUri))
                .willReturn(aResponse()
                        .withStatus(responseCode)
                        .withHeader("Content-Type", "application/json")
                )
        );
    }

}
