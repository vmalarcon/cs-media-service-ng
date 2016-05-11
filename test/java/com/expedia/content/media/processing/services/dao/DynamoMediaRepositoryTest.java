package com.expedia.content.media.processing.services.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.dynamo.DynamoMediaRepository;

@RunWith(MockitoJUnitRunner.class)
public class DynamoMediaRepositoryTest {

    private List<Media> mediaList;
    private String environment = "test";

    
    @Mock
    private DynamoDBMapper dynamoMapper;
    
    @Mock
    private PaginatedQueryList<Media> paginatedQueryList;
    
    DynamoMediaRepository dynamoMediaRepository;
    
    @Before
    public void setup() {
        mediaList = defaultMediaList();
        dynamoMediaRepository = new DynamoMediaRepository(dynamoMapper, environment);
        when(paginatedQueryList.stream()).thenReturn(mediaList.stream()).thenReturn(mediaList.stream());
        when(dynamoMapper.query(eq(Media.class), Matchers.anyObject())).thenReturn(paginatedQueryList);
        when(dynamoMapper.load(eq(Media.class),eq("g01"))).thenReturn(mediaList.get(0));
        when(dynamoMapper.load(eq(Media.class),eq("g02"))).thenReturn(mediaList.get(1));
        when(dynamoMapper.load(eq(Media.class),eq("g03"))).thenReturn(mediaList.get(2));
    }

    @Test
    public void testGeMediaByFileName() {
        final List<Media> media = dynamoMediaRepository.getMediaByFilename("media2");
        assertTrue(media.size()==1);
        final Media actual = media.get(0);
        final Media expected = mediaList.get(1);
        assertEquals(expected.getActive(), actual.getActive());
        assertEquals(expected.getMediaGuid(), actual.getMediaGuid());
        assertEquals(expected.getFileName(), actual.getFileName());
        assertEquals(expected.getDomain(), actual.getDomain());
        assertEquals(expected.getDomainId(), actual.getDomainId());
        assertEquals(expected.isHidden(), actual.isHidden());
    }

    @Test
    public void testGetMedia() {
        assertTrue(dynamoMediaRepository.getMedia("g01") == null);
        assertTrue(dynamoMediaRepository.getMedia("g02") != null);
        assertTrue(dynamoMediaRepository.getMedia("g03") == null);
    }

    private List<Media> defaultMediaList() {
        final Media media1 = Media.builder().mediaGuid("g01").fileName("media1").active("true").domain("Lodging").domainId("123").environment(environment).hidden(true).build();
        final Media media2 = Media.builder().mediaGuid("g02").fileName("media2").active("true").domain("Lodging").domainId("5").environment(environment).hidden(false).build();
        final Media media3 = Media.builder().mediaGuid("g03").fileName("media3").active("true").domain("Lodging").domainId("5").environment(environment).hidden(true).build();
        return Arrays.asList(media1, media2, media3);
    }
}
