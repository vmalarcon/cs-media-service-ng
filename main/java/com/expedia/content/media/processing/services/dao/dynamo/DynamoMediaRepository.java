package com.expedia.content.media.processing.services.dao.dynamo;

import java.util.Date;
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
import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.Metadata;
import com.expedia.content.media.processing.services.dao.MediaDBException;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.domain.MediaDerivative;
import com.expedia.content.media.processing.services.dao.domain.Thumbnail;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * DynamoDB implementation of the MediaConfigRepository interface.
 */
@Repository
public class DynamoMediaRepository {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamoMediaRepository.class);
    private final static ObjectWriter WRITER = new ObjectMapper().writer();
    private final DynamoDBMapper dynamoMapper;
    private final String environment;
    
    @Autowired
    public DynamoMediaRepository(DynamoDBMapper dynamoMapper, String environment) {
        this.dynamoMapper = dynamoMapper;
        this.environment = environment;
    }
    
    /**
     * Given a fileName returns all the media that were saved with that name.
     *
     * @param fileName File name of the Media.
     * @return List of Media with the requested Filename.
     */
    public List<Media> getMediaByFilename(String fileName) {
        final HashMap<String, AttributeValue> params = new HashMap<>();
        params.put(":mfn", new AttributeValue().withS(fileName));
        
        final DynamoDBQueryExpression<Media> expression = new DynamoDBQueryExpression<Media>()
                .withIndexName("cs-mediadb-index-Media-MediaFileName")
                .withConsistentRead(false)
                .withKeyConditionExpression("MediaFileName = :mfn")
                .withExpressionAttributeValues(params);
                
        return dynamoMapper.query(Media.class, expression);
    }
    
    /**
     * Loads a list of media items based on a domain id.
     * 
     * @param domainId Id of the domain item the media is required.
     * @return The list of media attached to the domain id.
     */
    public List<Media> loadMedia(Domain domain, String domainId) {
        List<Media> mediaList = null;
        try {
            final HashMap<String, String> names = new HashMap<>();
            names.put("#domain", "Domain");
            
            final Map<String, AttributeValue> params = new HashMap<>();
            params.put(":pDomainId", new AttributeValue().withS(domainId));
            params.put(":pDomain", new AttributeValue().withS(domain.getDomain()));
            
            final DynamoDBQueryExpression<Media> query = new DynamoDBQueryExpression<Media>()
                    .withIndexName("cs-mediadb-index-Media-DomainID-Domain")
                    .withConsistentRead(false)
                    .withKeyConditionExpression("DomainID = :pDomainId and #domain = :pDomain")
                    .withExpressionAttributeNames(names)
                    .withExpressionAttributeValues(params);
                    
            mediaList = dynamoMapper.query(Media.class, query);
        } catch (Exception e) {
            LOGGER.error("ERROR - message={}.", e.getMessage(), e);
            throw new MediaDBException(e.getMessage(), e);
        }
        return mediaList;
    }
    
    /**
     * Store the media Add message in dynamoDB
     * 
     * @param imageMessage message to store.
     * @param thumbnail Associated thumbnail.
     */
    public void storeMediaAddMessage(ImageMessage imageMessage, Thumbnail thumbnail) {
        try {
            dynamoMapper.save(buildMedia(imageMessage, thumbnail));
            LOGGER.info("Media successfully added in dynamodb : GUID=[{}], file url =[{}], RequestId=[{}] ", imageMessage.getMediaGuid(),
                    imageMessage.getFileUrl(),
                    imageMessage.getRequestId());
            if (imageMessage.isGenerateThumbnail()) {
                dynamoMapper.save(buildDerivative(imageMessage, thumbnail));
                LOGGER.info("Thumbnail derivative successfully added in dynamodb : Derivatives=[{}], RequestId=[{}] ", thumbnail,
                        imageMessage.getRequestId());
            }
        } catch (Exception e) {
            LOGGER.error("ERROR when trying to save in dynamodb - error message={}.", e.getMessage(), e);
            throw new MediaDBException(e.getMessage(), e);
        }
    }
    
    /**
     * Build a media object from the imageMessage.
     * 
     * @param imageMessage imageMessage to use.
     * @param thumbnail generated thumbnail.
     * @return returns the media
     */
    private Media buildMedia(ImageMessage imageMessage, Thumbnail thumbnail) throws Exception {
        MediaDerivative mediaDerivative = null;
        Metadata basicMetadata = null;
        if (thumbnail != null) {
            mediaDerivative = imageMessage.isGenerateThumbnail() ? buildDerivative(imageMessage, thumbnail) : null;
            basicMetadata = thumbnail.getSourceMetadata();
        }
        return Media.builder().active(imageMessage.isActive().toString())
                .clientId(imageMessage.getClientId())
                .derivatives((mediaDerivative == null) ? "" : WRITER.writeValueAsString(mediaDerivative))
                .domain(imageMessage.getOuterDomainData().getDomain().getDomain())
                .domainDerivativeCategory(imageMessage.getOuterDomainData().getDerivativeCategory())
                .domainFields(WRITER.writeValueAsString(imageMessage.getOuterDomainData().getDomainFields()))
                .domainId(imageMessage.getOuterDomainData().getDomainId())
                .environment(environment)
                .fileName(imageMessage.getFileName())
                .fileUrl(imageMessage.getFileUrl())
                .lastUpdated(new Date())
                .metadata(basicMetadata == null ? "" : WRITER.writeValueAsString(basicMetadata))
                .mediaGuid(imageMessage.getMediaGuid()).build();
    }
    
    /**
     * Build a MediaDerivative object from the imageMessage and thumbnail object.
     * 
     * @param imageMessage imageMessage to use.
     * @param thumbnail thumbnail to use.
     * @return returns the MediaDerivative.
     */
    private MediaDerivative buildDerivative(ImageMessage imageMessage, Thumbnail thumbnail) {
        return MediaDerivative.builder()
                .height(Integer.toString(thumbnail.getThumbnailMetadata().getHeight()))
                .width(Integer.toString(thumbnail.getThumbnailMetadata().getWidth()))
                .fileSize(Integer.toString(thumbnail.getThumbnailMetadata().getFileSize()))
                .location(thumbnail.getLocation())
                .mediaGuid(imageMessage.getMediaGuid())
                .type(thumbnail.getType()).build();
    }
    
}
