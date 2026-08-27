package uk.gov.companieshouse.companyprofile.search.config;

import static java.time.temporal.ChronoUnit.SECONDS;

import consumer.exception.TopicErrorInterceptor;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;
import uk.gov.companieshouse.companyprofile.search.serialization.ResourceChangedDataDeserializer;
import uk.gov.companieshouse.companyprofile.search.serialization.ResourceChangedDataSerializer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.stream.ResourceChangedData;

@TestConfiguration
public class KafkaTestContainerConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("kafka-test-container-config");

    private final ResourceChangedDataDeserializer deserializer;
    private final ResourceChangedDataSerializer serializer;

    @Value("${company-profile.search.topic}")
    private String topic;

    @Value("${company-profile.search.group-id}")
    private String groupId;

    public KafkaTestContainerConfig(ResourceChangedDataDeserializer deserializer, ResourceChangedDataSerializer serializer) {
        this.deserializer = deserializer;
        this.serializer = serializer;
    }

    @Bean
    ConfluentKafkaContainer kafkaContainer() {
        var kafkaContainer = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:latest"));
        WaitStrategy waitStrategy = Wait.defaultWaitStrategy()
                .withStartupTimeout(Duration.of(300, SECONDS));
        kafkaContainer.setWaitStrategy(waitStrategy);
        kafkaContainer.start();
        return kafkaContainer;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<@NonNull String, @NonNull ResourceChangedData> listenerContainerFactory(ConfluentKafkaContainer kafkaContainer) {
        ConcurrentKafkaListenerContainerFactory<@NonNull String, @NonNull ResourceChangedData> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(kafkaConsumerFactory(kafkaContainer));
        factory.getContainerProperties().setIdleBetweenPolls(0);
        factory.getContainerProperties().setPollTimeout(10L);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    @Bean
    ConsumerFactory<@NonNull String, ResourceChangedData> kafkaConsumerFactory(ConfluentKafkaContainer kafkaContainer) {
        Map<String, Object> configs = consumerConfigs(kafkaContainer);
        return new DefaultKafkaConsumerFactory<>(configs, new StringDeserializer(), new ErrorHandlingDeserializer<>());
    }

    @Bean
    Map<String, Object> consumerConfigs(ConfluentKafkaContainer kafkaContainer) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, ResourceChangedDataDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        return props;
    }

    @Bean
    ProducerFactory<@NonNull String, Object> producerFactory(final ConfluentKafkaContainer kafkaContainer) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, TopicErrorInterceptor.class.getName());

        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), serializer);
    }

    @Bean
    KafkaTemplate<@NonNull String, @NonNull Object> kafkaTemplate(ConfluentKafkaContainer kafkaContainer) {
        return new KafkaTemplate<>(producerFactory(kafkaContainer));
    }

    @Bean
    KafkaConsumer<String, Object> testObserverConsumer(final ConfluentKafkaContainer kafkaContainer) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        KafkaConsumer<String, Object> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of(
                "%s-%s-invalid".formatted(topic, groupId), // stream-company-profile-company-profile-search-consumer-itest-invalid
                "%s-%s-error".formatted(topic, groupId), // stream-company-profile-company-profile-search-consumer-itest-invalid
                "%s-%s-retry".formatted(topic, groupId) // stream-company-profile-company-profile-search-consumer-itest-invalid
        ));
        return consumer;
    }

}
