package uk.gov.companieshouse.companyprofile.search.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
public class LoggingConfigTest {

    private static final String NAMESPACE = "logging-config-unit-test";

    private final LoggingConfig loggingConfig =
            new LoggingConfig(NAMESPACE);

    @Test
    void testCreateLogger() {
        Logger logger = loggingConfig.logger();

        assertThat(logger).isNotNull();
    }
}
