package uk.gov.companieshouse.companyprofile.search.config;

import consumer.deserialization.AvroDeserializer;
import consumer.serialization.AvroSerializer;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;
import uk.gov.companieshouse.companyprofile.search.logging.DataMapHolder;
import uk.gov.companieshouse.environment.EnvironmentReader;
import uk.gov.companieshouse.environment.impl.EnvironmentReaderImpl;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Configuration
public class ApplicationConfig implements WebMvcConfigurer {

    private final String chsApiKey;
    private final String apiUrl;
    private final String internalApiUrl;

    public ApplicationConfig(@Value("${api.api-key}") String chsApiKey, @Value("${api.api-url}") String apiUrl,
            @Value("${api.internal-api-url}") String internalApiUrl) {
        this.chsApiKey = chsApiKey;
        this.apiUrl = apiUrl;
        this.internalApiUrl = internalApiUrl;
    }

    @Bean
    SerializerFactory serializerFactory() {
        return new SerializerFactory();
    }

    @Bean
    EnvironmentReader environmentReader() {
        return new EnvironmentReaderImpl();
    }

    @Bean
    AvroSerializer serializer() {
        return new AvroSerializer();
    }

    @Bean
    AvroDeserializer<ResourceChangedData> deserializer() {
        return new AvroDeserializer<>(ResourceChangedData.class);
    }

    @Bean
    public Supplier<InternalApiClient> internalApiClientSupplier() {
        return () -> {
            ApiKeyHttpClient apiKeyHttpClient = new ApiKeyHttpClient(chsApiKey);
            apiKeyHttpClient.setRequestId(DataMapHolder.getRequestId());

            InternalApiClient internalApiClient = new InternalApiClient(apiKeyHttpClient);
            internalApiClient.setBasePath(apiUrl);
            internalApiClient.setInternalBasePath(internalApiUrl);

            return internalApiClient;
        };
    }
}