package com.expedia.content.media.processing.services.dao.dynamo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.expedia.content.media.processing.services.dao.MediaDBException;
import com.expedia.content.media.processing.services.dao.domain.Media;

/**
 * DynamoDB implementation of the MediaConfigRepository interface.
 */
@Repository
public class DynamoMediaRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoMediaRepository.class);

    private final DynamoDBMapper dynamoMapper;

    @Autowired
    public DynamoMediaRepository(DynamoDBMapper dynamoMapper) {
        this.dynamoMapper = dynamoMapper;
    }

    /**
     * Loads a list of media items based on a domain id.
     * 
     * @param domainId Id of the domain item the media is required.
     * @return The list of media attached to the domain id.
     */
    public List<Media> loadMedia(String domainId) {
        List<Media> mediaList = null;
        try {
            final Map<String, AttributeValue> params = new HashMap<>();
            params.put(":pDomainId", new AttributeValue().withS(domainId));
            //params.put(":pDomainName", new AttributeValue().withS(domainName));

            final DynamoDBQueryExpression<Media> query = new DynamoDBQueryExpression<Media>()
                    .withIndexName("cs-mediadb-index-Media-DomainID")
                    .withConsistentRead(false)
                    .withKeyConditionExpression("DomainID = :pDomainId")// and DomainName = :pDomainName")
                    .withExpressionAttributeValues(params);

            mediaList = dynamoMapper.query(Media.class, query);
        } catch (Exception e) {
            LOGGER.error("ERROR - message={}.", e.getMessage(), e);
            throw new MediaDBException(e.getMessage(), e);
        }
        return mediaList;
    }

}
