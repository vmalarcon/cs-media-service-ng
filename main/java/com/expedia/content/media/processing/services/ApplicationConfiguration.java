package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.kafka.KafkaCommonPublisher;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Properties;

@Configuration
public class ApplicationConfiguration {

    @Value("${kafka.broker.server}")
    private String bokerServer;

    @Value("${kafka.schema.server}")
    private String schemaServer;

    @Value("${kafka.message.send.enable}")
    private String enableSend;

    @Bean
    public KafkaCommonPublisher kafkaCommonPublisher() throws IOException {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bokerServer);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroSerializer");
        props.put("schema.registry.url", schemaServer);
        props.put("enableSend", enableSend);
        return new KafkaCommonPublisher(props);
    }

}
