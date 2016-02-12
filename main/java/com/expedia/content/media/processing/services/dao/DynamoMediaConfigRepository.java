package com.expedia.content.media.processing.services.dao;


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.expedia.content.media.processing.pipeline.data.manager.repository.dynamo.domain.MediaConfig;

/**
 * DynamoDB implementation of the MediaConfigRepository interface.
 */
@Repository
public class DynamoMediaConfigRepository {

    private final static Logger LOGGER = LoggerFactory.getLogger(DynamoMediaConfigRepository.class);

    private final DynamoDBMapper dynamoMapper;

    public DynamoMediaConfigRepository(DynamoDBMapper dynamoMapper) {
        this.dynamoMapper = dynamoMapper;
    }

    public void saveMediaConfig(MediaConfig mediaConfig) {
        try {
            dynamoMapper.save(mediaConfig);
        } catch (Exception e) {
            LOGGER.error("ERROR - message={}.", e.getMessage(), e);
            throw new MediaDBException(e.getMessage(), e);
        }
    }

    public void deleteMediaConfig(MediaConfig mediaConfig) {
        try {
            dynamoMapper.delete(mediaConfig);
        } catch (Exception e) {
            LOGGER.error("ERROR - message={}.", e.getMessage(), e);
            throw new MediaDBException(e.getMessage(), e);
        }
    }

    /**
     * use DynamoDBScanExpression because it can query by either environment or propertyName,
     * if use DynamoDBQueryExpression hashKey environment must be provided.
     *
     * @param mediaConfig
     * @return
     */
    public List<MediaConfig> loadMediaConfig(MediaConfig mediaConfig) {
        List<MediaConfig> mediaConfigList = null;
        try {
            final Condition rangeCondition = new Condition()
                    .withComparisonOperator(ComparisonOperator.EQ)
                    .withAttributeValueList(new AttributeValue().withS(mediaConfig.getPropertyName()));
            final Condition hashKeyCondition = new Condition()
                    .withComparisonOperator(ComparisonOperator.EQ)
                    .withAttributeValueList(new AttributeValue().withS(mediaConfig.getEnvironment()));
            final DynamoDBScanExpression dynamoDBScanExpression = new DynamoDBScanExpression();
            if (mediaConfig.getPropertyName() != null) {
                dynamoDBScanExpression.addFilterCondition(PROPERTYNAME, rangeCondition);
            }
            if (mediaConfig.getEnvironment() != null) {
                dynamoDBScanExpression.addFilterCondition(ENVIRONMENT, hashKeyCondition);
            }
            mediaConfigList = dynamoMapper.scan(MediaConfig.class, dynamoDBScanExpression);
        } catch (Exception e) {
            LOGGER.error("ERROR - message={}.", e.getMessage(), e);
            throw new MediaDBException(e.getMessage(), e);
        }
        return mediaConfigList;
    }

}
