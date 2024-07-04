package uk.gov.companieshouse.companyprofile.search.deserialiser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import consumer.exception.NonRetryableErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.logging.Logger;

@Component
public class CompanyProfileDeserialiser {

    private final Logger logger;


    private final ObjectMapper objectMapper;

    @Autowired
    public CompanyProfileDeserialiser(Logger logger, ObjectMapper objectMapper) {
        this.logger = logger;
        this.objectMapper = objectMapper;
    }

    /**
     * Deserialise Company Profile message.
     */
    public Data deserialiseCompanyProfile(String data) {
        try {
            return objectMapper.readValue(data, Data.class);
        } catch (JsonProcessingException exception) {
            logger.errorContext("Unable to parse message payload data", exception, null);
            throw new NonRetryableErrorException("Unable to parse message payload data", exception);
        }
    }

}
