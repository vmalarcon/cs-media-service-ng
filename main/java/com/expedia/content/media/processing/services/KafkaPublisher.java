package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.avro.ImageMessageAvro;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.util.ImageMessageAvroUtil;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * publish imageMessage to kafka topic
 */
@Component
public class KafkaPublisher {
    @Value("${kafka.broker.server}")
    private String kafkaServer;
    @Value("${kafka.schema.server}")
    private String kafkaSchemaServer;
    @Value("${kafka.imagemessage.topic}")
    private String imageMessageTopic;
    @Value("${kafka.message.send.enable}")
    private boolean sendMessage;

    /**
     * convert imageMessage to Avro object and publish to imageMessage topic.
     * @param imageMessage
     */
    public void publishToTopic(ImageMessage imageMessage) {
        if (sendMessage) {
            final ImageMessageAvro imageMessageAvro = ImageMessageAvroUtil.generateAvroMsg(imageMessage);
            final ProducerRecord<String, ImageMessageAvro> record = new ProducerRecord<String, ImageMessageAvro>(
                    imageMessageTopic, "imageMessage", imageMessageAvro);
            final Producer<String, ImageMessageAvro> producer = getProducer();
            producer.send(record);
            producer.close();
        }
    }



    private Producer<String, ImageMessageAvro> getProducer() {
        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroSerializer");
        props.put("schema.registry.url", kafkaSchemaServer);
        return new KafkaProducer<String, ImageMessageAvro>(props);
    }


}
