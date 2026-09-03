package uk.gov.companieshouse.companyprofile.search.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import consumer.deserialization.AvroDeserializer;
import consumer.serialization.AvroSerializer;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.companyprofile.search.logging.DataMapHolder;
import uk.gov.companieshouse.environment.EnvironmentReader;
import uk.gov.companieshouse.environment.impl.EnvironmentReaderImpl;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;

@ExtendWith(MockitoExtension.class)
class ApplicationConfigTest {

    private static final String CHS_API_KEY = "test-api-key";
    private static final String CHS_API_URL = "http://localhost:8888";

    private final ApplicationConfig applicationConfig =
            new ApplicationConfig(CHS_API_KEY, CHS_API_URL);

    @Test
    void shouldCreateSerializerFactory() {
        SerializerFactory result = applicationConfig.serializerFactory();

        assertThat(result).isNotNull();
    }

    @Test
    void shouldCreateEnvironmentReader() {
        EnvironmentReader result = applicationConfig.environmentReader();

        assertThat(result).isInstanceOf(EnvironmentReaderImpl.class);
    }

    @Test
    void shouldCreateSerializer() {
        AvroSerializer result = applicationConfig.serializer();

        assertThat(result)
                .isNotNull()
                .isInstanceOf(AvroSerializer.class);
    }

    @Test
    void shouldCreateDeserializer() {
        AvroDeserializer<ResourceChangedData> result =
                applicationConfig.deserializer();

        assertThat(result).isNotNull();
    }

    @Test
    void shouldCreateObjectMapperWithExpectedConfiguration() {
        ObjectMapper result = applicationConfig.objectMapper();

        assertThat(result).isNotNull();

        assertThat(result.getPropertyNamingStrategy())
                .isEqualTo(PropertyNamingStrategies.SNAKE_CASE);

        assertThat(result.getSerializationConfig()
                .getDefaultPropertyInclusion()
                .getValueInclusion())
                .isEqualTo(JsonInclude.Include.NON_NULL);

        assertThat(result.getDeserializationConfig()
                .isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES))
                .isFalse();

        assertThat(result.getSerializationConfig()
                .isEnabled(SerializationFeature.FAIL_ON_EMPTY_BEANS))
                .isFalse();
    }

    @Test
    void shouldCreateInternalApiClientSupplier() {
        Supplier<InternalApiClient> supplier =
                applicationConfig.internalApiClientSupplier();

        assertThat(supplier).isNotNull();
    }

    @Test
    void shouldCreateInternalApiClientWithConfiguredValues() {
        try (MockedStatic<DataMapHolder> dataMapHolder =
                mockStatic(DataMapHolder.class)) {

            String requestId = "test-request-id";
            dataMapHolder.when(DataMapHolder::getRequestId)
                    .thenReturn(requestId);

            Supplier<InternalApiClient> supplier =
                    applicationConfig.internalApiClientSupplier();

            InternalApiClient result = supplier.get();

            assertThat(result).isNotNull();
            assertThat(result.getBasePath()).isEqualTo(CHS_API_URL);

            dataMapHolder.verify(DataMapHolder::getRequestId);
        }
    }

    @Test
    void shouldCreateNewInternalApiClientEachTimeSupplierIsCalled() {
        try (MockedStatic<DataMapHolder> dataMapHolder =
                mockStatic(DataMapHolder.class)) {

            dataMapHolder.when(DataMapHolder::getRequestId)
                    .thenReturn("request-id");

            Supplier<InternalApiClient> supplier =
                    applicationConfig.internalApiClientSupplier();

            InternalApiClient first = supplier.get();
            InternalApiClient second = supplier.get();

            assertThat(first)
                    .isNotSameAs(second);

            assertThat(first.getBasePath())
                    .isEqualTo(CHS_API_URL);

            assertThat(second.getBasePath())
                    .isEqualTo(CHS_API_URL);

            dataMapHolder.verify(DataMapHolder::getRequestId,
                    org.mockito.Mockito.times(2));
        }
    }
}