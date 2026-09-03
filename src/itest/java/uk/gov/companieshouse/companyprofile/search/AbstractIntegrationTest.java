package uk.gov.companieshouse.companyprofile.search;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.companieshouse.companyprofile.search.config.KafkaTestContainerConfig;
import uk.gov.companieshouse.companyprofile.search.config.WireMockTestConfig;

/**
 * Loads the application context.
 * Best place to mock your downstream calls.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({KafkaTestContainerConfig.class, WireMockTestConfig.class})
@ActiveProfiles({"test"})
public abstract class AbstractIntegrationTest {

}
