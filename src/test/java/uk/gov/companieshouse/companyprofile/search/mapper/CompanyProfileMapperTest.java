package uk.gov.companieshouse.companyprofile.search.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import consumer.exception.NonRetryableErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.logging.Logger;

class CompanyProfileMapperTest {

    private ObjectMapper objectMapper;
    private Logger logger;
    private CompanyProfileMapper mapper;

    @BeforeEach
    void setUp() {
        objectMapper = mock(ObjectMapper.class);
        logger = mock(Logger.class);

        mapper = new CompanyProfileMapper(logger, objectMapper);
    }

    @Test
    void shouldDeserialiseCompanyProfile() throws Exception {
        String json = """
                    {
                    "company_number": "12345678"
                }
        """;

        Data expected = mock(Data.class);

        when(objectMapper.readValue(json, Data.class)).thenReturn(expected);

        Data result = mapper.deserialiseCompanyProfile(json);

        assertThat(result).isSameAs(expected);

        verify(objectMapper).readValue(json, Data.class);
    }

    @Test
    void shouldThrowNonRetryableErrorExceptionWhenDeserialisationFails() throws Exception {
        String invalidJson = "invalid json";

        JsonProcessingException exception = mock(JsonProcessingException.class);

        when(objectMapper.readValue(invalidJson, Data.class)).thenThrow(exception);

        assertThatThrownBy(() -> mapper.deserialiseCompanyProfile(invalidJson))
                .isInstanceOf(NonRetryableErrorException.class)
                .hasMessage("Unable to parse message payload data")
                .hasCause(exception);

        verify(logger).errorContext("Unable to parse message payload data", exception,null);
    }
}