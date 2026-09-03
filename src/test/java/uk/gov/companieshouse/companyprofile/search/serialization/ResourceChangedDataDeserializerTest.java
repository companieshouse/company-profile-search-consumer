package uk.gov.companieshouse.companyprofile.search.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import consumer.exception.NonRetryableErrorException;
import java.io.ByteArrayOutputStream;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.companyprofile.search.util.TestHelper;
import uk.gov.companieshouse.stream.ResourceChangedData;

@ExtendWith(MockitoExtension.class)
class ResourceChangedDataDeserializerTest {

    private final ResourceChangedDataDeserializer deserializer =
            new ResourceChangedDataDeserializer();

    @Test
    void shouldDeserializeValidAvroData() throws Exception {
        ResourceChangedData expected = new TestHelper().createBasicPayload();

        byte[] data = serialize(expected);

        ResourceChangedData result = deserializer.deserialize("test-topic", data);

        assertThat(result).isNotNull();
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldThrowNonRetryableErrorExceptionWhenDeserializationFails() {
        byte[] invalidData = new byte[]{1, 2, 3, 4};

        assertThatThrownBy(() -> deserializer.deserialize("test-topic", invalidData))
                .isInstanceOf(NonRetryableErrorException.class)
                .hasCauseInstanceOf(Exception.class);
    }

    private byte[] serialize(ResourceChangedData data) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);

        ReflectDatumWriter<ResourceChangedData> writer =
                new ReflectDatumWriter<>(ResourceChangedData.class);

        writer.write(data, encoder);
        encoder.flush();

        return outputStream.toByteArray();
    }
}