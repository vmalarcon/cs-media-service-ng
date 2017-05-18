package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.kafka.KafkaCommonPublisher;
import com.expedia.content.media.processing.pipeline.reporting.CompositeReporting;
import com.expedia.content.media.processing.pipeline.reporting.KafkaReporting;
import com.expedia.content.media.processing.pipeline.reporting.Reporting;
import com.expedia.content.media.processing.services.dao.mediadb.MediaDBDerivativesDao;
import com.expedia.content.media.processing.services.dao.mediadb.MediaDBLodgingReferenceHotelIdDao;
import com.expedia.content.media.processing.services.dao.mediadb.MediaDBLodgingReferenceRoomIdDao;
import com.expedia.content.media.processing.services.dao.mediadb.MediaDBMediaDao;
import com.expedia.content.media.processing.services.dao.mediadb.MediaDBDomainCategoriesDao;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Configuration
public class ApplicationConfiguration {

    @Value("${kafka.broker.server}")
    private String brokerServer;

    @Value("${kafka.schema.server}")
    private String schemaServer;

    @Value("${kafka.message.send.enable}")
    private String enableSend;

    @Value("${mdb.datasource.username}")
    private String username;

    @Value("${mdb.datasource.password}")
    private String password;

    @Value("${mdb.datasource.url}")
    private String dataSourceURL;

    @Bean
    public DriverManagerDataSource mediaDBDataSource() {
        final DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(dataSourceURL);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    @Bean
    public MediaDBMediaDao mediaDBMediaDao() {
        return new MediaDBMediaDao(mediaDBDataSource());
    }

    @Bean
    public MediaDBLodgingReferenceHotelIdDao mediaDBLodgingReferenceHotelIdDao() {
        return new MediaDBLodgingReferenceHotelIdDao(mediaDBDataSource());
    }

    @Bean
    public MediaDBLodgingReferenceRoomIdDao mediaDBLodgingReferenceRoomIdDao() {
        return new MediaDBLodgingReferenceRoomIdDao(mediaDBDataSource());
    }

    @Bean
    public MediaDBDomainCategoriesDao mediaDBMediaDomainCategoriesDao() {
        return new MediaDBDomainCategoriesDao(mediaDBDataSource());
    }

    @Bean
    public MediaDBDerivativesDao mediaDBDerivativesDao() {
        return new MediaDBDerivativesDao(mediaDBDataSource());
    }

    @Bean
    public KafkaCommonPublisher kafkaCommonPublisher() throws IOException {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerServer);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroSerializer");
        props.put("schema.registry.url", schemaServer);
        props.put("enableSend", enableSend);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        return new KafkaCommonPublisher(props);
    }

    @Bean
    @Primary
    public CompositeReporting compositeReporting(@Value("${appname}") final String appname,
                                                 @Value("${kafka.activity.topic}") String activityTopic)
            throws IOException {
        final List<Reporting> reportings = new ArrayList<>();
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerServer);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroSerializer");
        props.put("schema.registry.url", schemaServer);
        props.put("enableSend", enableSend);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        final KafkaCommonPublisher kafkaCommonPublisher = new KafkaCommonPublisher(props);
        final KafkaReporting kafkaReporting = new KafkaReporting (kafkaCommonPublisher, appname, activityTopic);
        reportings.add(kafkaReporting);
        return new CompositeReporting(reportings);
    }

}
