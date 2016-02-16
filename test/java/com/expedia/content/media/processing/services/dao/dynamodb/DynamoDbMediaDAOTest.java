package com.expedia.content.media.processing.services.dao.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.expedia.content.media.processing.services.dao.Media;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DynamoDbMediaDAOTest {

    @Mock
    private DynamoDBMapper mockMapper;

    @Mock
    private PaginatedQueryList<Media> mockQueryResult;

    @Captor
    private ArgumentCaptor<DynamoDBQueryExpression<Media>> expressionCaptor;

    @Test
    public void testCallDynamo() {
        final DynamoDbMediaDAO mediaDAO = new DynamoDbMediaDAO(mockMapper);

        when(mockMapper.query(eq(Media.class), any())).thenReturn(mockQueryResult);

        List<Media> mediaList = mediaDAO.getMediaByFilename("testFilename.jpg");
        assertNotNull(mediaList);

        verify(mockMapper).query(eq(Media.class), expressionCaptor.capture());

        final DynamoDBQueryExpression<Media> expression = expressionCaptor.getValue();

        assertEquals("cs-mediadb-index-Media-MediaFileName", expression.getIndexName());
        assertFalse(expression.isConsistentRead());
        assertEquals("MediaFileName = :mfn", expression.getKeyConditionExpression());
        assertTrue(expression.getExpressionAttributeValues().containsKey(":mfn"));
        assertEquals("testFilename.jpg", expression.getExpressionAttributeValues().get(":mfn").getS());
    }
}
