package uk.gov.companieshouse.companyprofile.search.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import uk.gov.companieshouse.logging.Logger;

@TestConfiguration
public class WireMockTestConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    WireMockServer wireMockServer(final Logger logger, @Value("${wiremock.server.port}") final int wireMockPort) {
        logger.trace("Preparing to create WireMock Server: (port=%d)...".formatted(wireMockPort));

        return new WireMockServer(
                WireMockConfiguration.wireMockConfig().port(wireMockPort)
        );
    }

}