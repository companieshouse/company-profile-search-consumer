package uk.gov.companieshouse.companyprofile.search.serialization;

import static uk.gov.companieshouse.companyprofile.search.Application.NAMESPACE;

import consumer.exception.NonRetryableErrorException;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.companyprofile.search.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;

@Component
public class ResourceChangedDataDeserializer implements Deserializer<ResourceChangedData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    @Override
    public ResourceChangedData deserialize(String topic, byte[] data) {
        try {
            Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            DatumReader<ResourceChangedData> reader = new ReflectDatumReader<>(ResourceChangedData.class);

            return reader.read(null, decoder);

        } catch (Exception ex) {
            LOGGER.error("De-Serialization exception while converting to Avro schema object", ex, DataMapHolder.getLogMap());
            throw new NonRetryableErrorException(ex);
        }
    }

}
