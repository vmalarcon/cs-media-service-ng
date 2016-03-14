package com.expedia.content.media.processing.services.dao;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.Metadata;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.dao.domain.Thumbnail;
import com.expedia.content.media.processing.services.dao.dynamo.DynamoMediaRepository;

public class DynamoMediaRepositoryTest {
    
    
    @Test
    public void testStoreMediaAddMessageWithThumbnail() throws Exception {
        
        final DynamoDBMapper dynamoMapper = mock(DynamoDBMapper.class);
        
        final Map<String, Object> domainDataFields = new LinkedHashMap<>();
        domainDataFields.put("categoryId", "71013");
        final OuterDomain domainData = new OuterDomain(Domain.LODGING, "123", "Comics", "VirtualTour", domainDataFields);
        final ImageMessage message =
                ImageMessage.builder()
                        .mediaGuid("aaaaaaa-1010-bbbb-292929229")
                        .requestId("bbbbbb-1010-bbbb-292929229")
                        .clientId("EPC")
                        .userId("you")
                        .rotation("90")
                        .active(true)
                        .fileUrl("file:/my/files/are/awesome.jpg")
                        .fileName("original_file_name.png")
                        .sourceUrl("s3://bucket/source/aaaaaaa-1010-bbbb-292929229.jpg")
                        .rejectedFolder("rejected")
                        .callback(new URL("http://multi.source.callback/callback"))
                        .comment("test comment!")
                        .outerDomainData(domainData)
                        .generateThumbnail(true)
                        .build();
                        
        final Metadata sourceMetadata = Metadata.builder().fileSize(9444).height(1024).width(800).build();
        final Metadata thumbnailMetadata = Metadata.builder().fileSize(1020).height(180).width(180).build();
        final String thumbnailUrl = "s3://bucket/folder/aaaaaaa-1010-bbbb-292929229_t.jpg";
        
        final Thumbnail thumbnail = Thumbnail.builder()
                .thumbnailMetadata(thumbnailMetadata)
                .sourceMetadata(sourceMetadata)
                .location(thumbnailUrl)
                .type("t")
                .build();
                
        final DynamoMediaRepository dynamoMediaRepository = new DynamoMediaRepository(dynamoMapper, "Dev");
        dynamoMediaRepository.storeMediaAddMessage(message, thumbnail);
        verify(dynamoMapper, times(2)).save(any());
    }
    
    @Test
    public void testStoreMediaAddMessageWithoutThumbnail() throws Exception {
        
        final DynamoDBMapper dynamoMapper = mock(DynamoDBMapper.class);
        
        final Map<String, Object> domainDataFields = new LinkedHashMap<>();
        domainDataFields.put("categoryId", "71013");
        final OuterDomain domainData = new OuterDomain(Domain.LODGING, "123", "Comics", "VirtualTour", domainDataFields);
        final ImageMessage message =
                ImageMessage.builder()
                        .mediaGuid("aaaaaaa-1010-bbbb-292929229")
                        .requestId("bbbbbb-1010-bbbb-292929229")
                        .clientId("EPC")
                        .userId("you")
                        .rotation("90")
                        .active(true)
                        .fileUrl("file:/my/files/are/awesome.jpg")
                        .fileName("original_file_name.png")
                        .sourceUrl("s3://bucket/source/aaaaaaa-1010-bbbb-292929229.jpg")
                        .rejectedFolder("rejected")
                        .callback(new URL("http://multi.source.callback/callback"))
                        .comment("test comment!")
                        .outerDomainData(domainData)
                        .generateThumbnail(false)
                        .build();
                        
        final Metadata sourceMetadata = Metadata.builder().fileSize(9444).height(1024).width(800).build();
        final Metadata thumbnailMetadata = Metadata.builder().fileSize(1020).height(180).width(180).build();
        final String thumbnailUrl = "s3://bucket/folder/aaaaaaa-1010-bbbb-292929229_t.jpg";
        
        final Thumbnail thumbnail = Thumbnail.builder()
                .thumbnailMetadata(thumbnailMetadata)
                .sourceMetadata(sourceMetadata)
                .location(thumbnailUrl)
                .type("t")
                .build();
                
        final DynamoMediaRepository dynamoMediaRepository = new DynamoMediaRepository(dynamoMapper, "Dev");
        dynamoMediaRepository.storeMediaAddMessage(message, thumbnail);
        verify(dynamoMapper, times(1)).save(any());
    }
    
}
