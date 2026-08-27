package uk.gov.companieshouse.companyprofile.search.serialization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import consumer.exception.NonRetryableErrorException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.companyprofile.search.logging.DataMapHolder;
import uk.gov.companieshouse.companyprofile.search.util.TestHelper;
import uk.gov.companieshouse.stream.ResourceChangedData;

@ExtendWith(MockitoExtension.class)
class ResourceChangedDataSerializerTest {

    private final ResourceChangedDataSerializer serializer =
            new ResourceChangedDataSerializer();

    @Test
    void shouldReturnNullWhenPayloadIsNull() {
        byte[] result = serializer.serialize("test-topic", null);

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnByteArrayUnchangedWhenPayloadIsByteArray() {
        byte[] payload = {1, 2, 3, 4};

        byte[] result = serializer.serialize("test-topic", payload);

        assertThat(result).isSameAs(payload);
    }

    @Test
    void shouldSerializeResourceChangedData() {
        ResourceChangedData payload = new TestHelper().createBasicPayload();

        byte[] result = serializer.serialize("test-topic", payload);

        assertThat(result)
                .isNotNull()
                .isNotEmpty();
    }

    @Test
    void shouldSerializeOtherPayloadUsingToString() {
        Object payload = "hello world";

        byte[] result = serializer.serialize("test-topic", payload);

        assertThat(result).isEqualTo("hello world".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldSerializeOtherPayloadUsingUtf8() {
        Object payload = "héllo wörld £";

        byte[] result = serializer.serialize("test-topic", payload);

        assertThat(result).isEqualTo(payload.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldThrowNonRetryableErrorExceptionWhenSerializationFails() {
        ResourceChangedData payload = mock(ResourceChangedData.class);

        try (MockedStatic<DataMapHolder> dataMapHolder = mockStatic(DataMapHolder.class)) {

            dataMapHolder.when(DataMapHolder::getLogMap).thenReturn(null);

            assertThatThrownBy(() -> serializer.serialize("test-topic", payload))
                    .isInstanceOf(NonRetryableErrorException.class)
                    .hasMessage("Serialization exception while writing to byte array");
        }
    }
}