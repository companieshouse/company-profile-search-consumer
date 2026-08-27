package uk.gov.companieshouse.companyprofile.search.config;

import static org.assertj.core.api.Assertions.assertThat;

import consumer.deserialization.AvroDeserializer;
import consumer.exception.TopicErrorInterceptor;
import consumer.serialization.AvroSerializer;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import uk.gov.companieshouse.stream.ResourceChangedData;

class KafkaConfigTest {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final int LISTENER_CONCURRENCY = 3;

    private KafkaConfig kafkaConfig;

    private AvroSerializer serializer;
    private AvroDeserializer<ResourceChangedData> deserializer;

    @BeforeEach
    void setUp() {
        serializer = new AvroSerializer();
        deserializer = new AvroDeserializer<>(ResourceChangedData.class);

        kafkaConfig = new KafkaConfig(
                deserializer,
                serializer,
                BOOTSTRAP_SERVERS,
                LISTENER_CONCURRENCY
        );
    }

    @Test
    void shouldCreateKafkaConsumerFactory() {
        ConsumerFactory<String, ResourceChangedData> result =
                kafkaConfig.kafkaConsumerFactory();

        assertThat(result)
                .isInstanceOf(DefaultKafkaConsumerFactory.class);

        DefaultKafkaConsumerFactory<String, ResourceChangedData> factory =
                (DefaultKafkaConsumerFactory<String, ResourceChangedData>) result;

        Map<String, Object> configs = factory.getConfigurationProperties();

        assertThat(configs)
                .containsEntry(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        BOOTSTRAP_SERVERS
                )
                .containsEntry(
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        ErrorHandlingDeserializer.class
                )
                .containsEntry(
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        ErrorHandlingDeserializer.class
                )
                .containsEntry(
                        ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS,
                        StringDeserializer.class
                )
                .containsEntry(
                        ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                        AvroDeserializer.class
                )
                .containsEntry(
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                        "earliest"
                )
                .containsEntry(
                        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                        "false"
                )
                .containsEntry(
                        ConsumerConfig.ISOLATION_LEVEL_CONFIG,
                        "read_committed"
                );
    }

    @Test
    void shouldCreateProducerFactory() {
        ProducerFactory<String, Object> result =
                kafkaConfig.producerFactory();

        assertThat(result)
                .isInstanceOf(DefaultKafkaProducerFactory.class);

        DefaultKafkaProducerFactory<String, Object> factory =
                (DefaultKafkaProducerFactory<String, Object>) result;

        Map<String, Object> configs =
                factory.getConfigurationProperties();

        assertThat(configs)
                .containsEntry(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        BOOTSTRAP_SERVERS
                )
                .containsEntry(
                        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                        StringSerializer.class
                )
                .containsEntry(
                        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                        AvroSerializer.class
                )
                .containsEntry(
                        ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
                        TopicErrorInterceptor.class.getName()
                );
    }

    @Test
    void shouldCreateKafkaTemplate() {
        KafkaTemplate<String, Object> result =
                kafkaConfig.kafkaTemplate();

        assertThat(result).isNotNull();
    }

    @Test
    void shouldCreateListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ResourceChangedData> result =
                kafkaConfig.listenerContainerFactory();

        assertThat(result).isNotNull();

        assertThat(result.getContainerProperties().getAckMode())
                .isEqualTo(ContainerProperties.AckMode.RECORD);
    }
}