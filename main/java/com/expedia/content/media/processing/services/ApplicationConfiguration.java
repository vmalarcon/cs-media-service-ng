package com.expedia.content.media.processing.services;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.expedia.content.media.processing.pipeline.kafka.KafkaCommonPublisher;
import com.expedia.content.media.processing.pipeline.reporting.CompositeReporting;
import com.expedia.content.media.processing.pipeline.reporting.KafkaReporting;
import com.expedia.content.media.processing.pipeline.reporting.Reporting;
import com.expedia.content.media.processing.pipeline.reporting.dynamo.DynamoReporting;
import com.expedia.content.media.processing.pipeline.reporting.sql.LcmReporting;
import com.expedia.content.media.processing.pipeline.reporting.sql.SQLLogEntryInsertSproc;
import com.expedia.content.media.processing.services.dao.LcmProcessLogDao;
import com.expedia.content.media.processing.services.dao.MediaDomainCategoriesDao;
import com.expedia.content.media.processing.services.dao.PropertyRoomTypeGetIDSproc;
import com.expedia.content.media.processing.services.dao.RoomTypeDao;
import com.expedia.content.media.processing.services.dao.SKUGroupCatalogItemDao;
import com.expedia.content.media.processing.services.dao.SKUGroupGetSproc;
import com.expedia.content.media.processing.services.dao.mediadb.MediaDBMediaDao;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaDomainCategoriesSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaLogSproc;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Configuration
public class ApplicationConfiguration {

    private static final int MIN_POOL = 5;
    private static final int MAX_POOL = 25;
    private static final int INIT_POOL = 15;

    @Value("${kafka.broker.server}")
    private String brokerServer;

    @Value("${kafka.schema.server}")
    private String schemaServer;

    @Value("${kafka.message.send.enable}")
    private String enableSend;

    @Value("${${EXPEDIA_ENVIRONMENT}.mdb.datasource.username}")
    private String username;

    @Value("${${EXPEDIA_ENVIRONMENT}.mdb.datasource.password}")
    private String password;

    @Value("${${EXPEDIA_ENVIRONMENT}.mdb.datasource.url}")
    private String dataSourceURL;

    @Value("${${EXPEDIA_ENVIRONMENT}.datasource.url}")
    private String lcmDataSourceURL;

    @Value("${${EXPEDIA_ENVIRONMENT}.datasource.username}")
    private String lcmUserName;

    @Value("${${EXPEDIA_ENVIRONMENT}.datasource.password}")
    private String lcmPassword;

    @Bean
    public DataSource mediaDBDataSource() {
        final BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(dataSourceURL);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        final BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("net.sourceforge.jtds.jdbc.Driver");
        dataSource.setUrl(lcmDataSourceURL);
        dataSource.setUsername(lcmUserName);
        dataSource.setPassword(lcmPassword);
        dataSource.setValidationQuery("select 1");
        dataSource.setInitialSize(INIT_POOL);
        dataSource.setMinIdle(MIN_POOL);
        dataSource.setMaxIdle(MAX_POOL);
        return dataSource;
    }

    @Bean
    @Qualifier("lcm")
    public PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }

    @Bean
    @Qualifier("mysql")
    public PlatformTransactionManager transactionManager2() {
        return new DataSourceTransactionManager(mediaDBDataSource());
    }

    @Bean
    public MediaDBMediaDao mediaDBMediaDao() {
        return new MediaDBMediaDao(mediaDBDataSource());
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
    public CompositeReporting compositeReporting(@Value("${appname}") final String appname, final DynamoDBMapper dynamoMapper,
            final LcmReporting lcmReporting, @Value("${kafka.activity.topic}") String activityTopic)
            throws IOException {
        final List<Reporting> reportings = new ArrayList<>();
        final DynamoReporting dynamoReporting = new DynamoReporting(dynamoMapper, appname);
        reportings.add(dynamoReporting);
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerServer);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroSerializer");
        props.put("schema.registry.url", schemaServer);
        props.put("enableSend", enableSend);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        final KafkaCommonPublisher kafkaCommonPublisher = new KafkaCommonPublisher(props);
        final KafkaReporting kafkaReporting = new KafkaReporting(kafkaCommonPublisher, appname, activityTopic);
        reportings.add(kafkaReporting);
        reportings.add(lcmReporting);
        return new CompositeReporting(reportings);
    }

    @Bean
    public RoomTypeDao roomTypeDao() {
        return new RoomTypeDao(new PropertyRoomTypeGetIDSproc(dataSource()));
    }

    @Bean
    public SKUGroupCatalogItemDao skuGroupCatalogItemDao() {
        return new SKUGroupCatalogItemDao(new SKUGroupGetSproc(dataSource()));
    }

    @Bean
    public MediaDomainCategoriesDao mediaDomainCategoriesDao() {
        return new MediaDomainCategoriesDao(new SQLMediaDomainCategoriesSproc(dataSource()));
    }

    @Bean
    public LcmProcessLogDao processLogDao() {
        return new LcmProcessLogDao(new SQLMediaLogSproc(dataSource()));
    }

    @Bean
    @Primary
    public SQLLogEntryInsertSproc lcmInsertSproc() {
        return new SQLLogEntryInsertSproc(dataSource());
    }

}
