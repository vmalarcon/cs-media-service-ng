package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KafkaPublisherTest {


    @Test
    public void testPublishAvroMsg(){
        KafkaPublisher kafkaPublisher = new KafkaPublisher();
        String jsonMessage =
                "{ " + "\"fileUrl\": \"http://i.imgur.com/3PRGFii.jpg\", " + "\"fileName\": \"NASA_ISS-4.jpg\", " + "\"userId\": \"bobthegreat\", "
                        + "\"domain\": \"Lodging\", " + "\"domainId\": \"1238\", " + "\"domainProvider\": \"EPC Internal User\" " + "}";
        ImageMessage imageMessage  = ImageMessage.parseJsonMessage(jsonMessage);
        kafkaPublisher.publishToTopic(imageMessage);
    }
}
