package com.expedia.content.media.processing.services.dao.dynamo;


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.expedia.content.media.processing.services.dao.MediaDBException;
import com.expedia.content.media.processing.services.dao.domain.Media;

/**
 * DynamoDB implementation of the MediaConfigRepository interface.
 */
@Repository
public class DynamoMediaRepository {

    private static final String DOMAIN_ID = "DomainId";
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoMediaRepository.class);

    private final DynamoDBMapper dynamoMapper;

    public DynamoMediaRepository(DynamoDBMapper dynamoMapper) {
        this.dynamoMapper = dynamoMapper;
    }

    /**
     * TODO
     * @param media
     * @return
     */
    public List<Media> loadMedia(String domainId) {
        List<Media> mediaList = null;
        try {
             final Condition hashKeyCondition = new Condition()
                    .withComparisonOperator(ComparisonOperator.EQ)
                    .withAttributeValueList(new AttributeValue().withS(domainId));
            final DynamoDBScanExpression dynamoDBScanExpression = new DynamoDBScanExpression();
            dynamoDBScanExpression.addFilterCondition(DOMAIN_ID, hashKeyCondition);
            mediaList = dynamoMapper.scan(Media.class, dynamoDBScanExpression);
        } catch (Exception e) {
            LOGGER.error("ERROR - message={}.", e.getMessage(), e);
            throw new MediaDBException(e.getMessage(), e);
        }
        return mediaList;
    }

}
