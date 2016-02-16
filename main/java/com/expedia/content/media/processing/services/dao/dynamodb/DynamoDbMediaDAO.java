package com.expedia.content.media.processing.services.dao.dynamodb;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.expedia.content.media.processing.services.dao.Media;
import com.expedia.content.media.processing.services.dao.MediaDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

/**
 * Retrieves Media information stored in DynamoDB
 *
 * @see Media
 */
@Component
public class DynamoDbMediaDAO implements MediaDAO {

    private final DynamoDBMapper mapper;

    @Autowired
    public DynamoDbMediaDAO(DynamoDBMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Media> getMediaByFilename(String fileName) {
        final HashMap<String, AttributeValue> params = new HashMap<>();
        params.put(":mfn", new AttributeValue().withS(fileName));

        final DynamoDBQueryExpression<Media> expression = new DynamoDBQueryExpression<Media>()
                .withIndexName("cs-mediadb-index-Media-MediaFileName")
                .withConsistentRead(false)
                .withKeyConditionExpression("MediaFileName = :mfn")
                .withExpressionAttributeValues(params);

        return mapper.query(Media.class, expression);
    }
}
