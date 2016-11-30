package com.expedia.content.media.processing.services.dao.dynamo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.Metadata;
import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
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

    private static final FormattedLogger LOGGER = new FormattedLogger(DynamoMediaRepository.class);
    private final static ObjectWriter WRITER = new ObjectMapper().writer();
    private final DynamoDBMapper dynamoMapper;

    private final String environment;

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
        return dynamoMapper.query(Media.class, expression).stream()
                .filter(item -> !(Boolean.TRUE.equals(item.isHidden())))
                .filter(item -> environment.equals(item.getEnvironment()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a media by its GUID.
     *
     * @param mediaGUID GUID of the Media.
     * @return Media with the requested GUID.
     */
    public Media getMedia(String mediaGUID) {
        final Media media = dynamoMapper.load(Media.class, mediaGUID);
        return media == null ? null : media.isHidden() ? null : media;
    }

    /**
     * Deletes a media.
     *
     * @param media the Media to be deleted.
     */
    public void deleteMedia(Media media) {
        dynamoMapper.delete(media);
    }

    /**
     * get the Media information from dynamo Media table.
     * @param mediaId media Id from JSON
     * @return list of Media
     */
    public List<Media> getMediaByMediaId(String mediaId) {
        final HashMap<String, AttributeValue> params = new HashMap<>();
        params.put(":mfn", new AttributeValue().withS(mediaId));
        final DynamoDBQueryExpression<Media> expression = new DynamoDBQueryExpression<Media>()
                .withIndexName("cs-mediadb-index-Media-lcmMediaId")
                .withConsistentRead(false)
                .withKeyConditionExpression("lcmMediaId = :mfn")
                .withExpressionAttributeValues(params);
        final List<Media> results = dynamoMapper.query(Media.class, expression);
        return results.stream()
                .filter(item -> !(Boolean.TRUE.equals(item.isHidden())))
                .filter(item -> environment.equals(item.getEnvironment()))
                .collect(Collectors.toList());
    }

    /**
     * save the JSON message to Dynamo media table.
     * @param media
     */
    public void saveMedia(Media media) {
        try {
            dynamoMapper.save(media);
        } catch (Exception e) {
            LOGGER.error(e, "ERROR when trying to save in dynamodb MediaGuid={} ClientID={} ErrorMessage={}.", media.getMediaGuid(), media.getClientId(), e.getMessage());
            throw new MediaDBException(e.getMessage(), e);
        }
    }

    /**
     * Returns all property hero media for the domain id and domain name passed in the arguments.
     *
     * @param domainId   The domain id for a media.
     * @param domainName The domain name for a media.
     */
    public List<Media> retrieveHeroPropertyMedia(String domainId, String domainName) throws MediaDBException {
        final HashMap<String, String> names = new HashMap<>();
        names.put("#domain", "Domain");

        final HashMap<String, AttributeValue> params = new HashMap<>();
        params.put(":pDomainId", new AttributeValue().withS(domainId));
        params.put(":pDomain", new AttributeValue().withS(domainName));

        try {
            final DynamoDBQueryExpression<Media> query = new DynamoDBQueryExpression<Media>()
                    .withIndexName("cs-mediadb-index-Media-DomainID-Domain")
                    .withConsistentRead(false)
                    .withKeyConditionExpression("DomainID = :pDomainId and #domain = :pDomain")
                    .withExpressionAttributeNames(names)
                    .withExpressionAttributeValues(params);

            final List<Media> results = dynamoMapper.query(Media.class, query);
            return results.stream()
                    .filter(item -> !(Boolean.TRUE.equals(item.isHidden())))
                    .filter(item -> environment.equals(item.getEnvironment()))
                    .filter(item -> item.getPropertyHero() != null && item.getPropertyHero())
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            final String message = String.format("ERROR retrieving hero media for domainId=[%s], domainName=[%s]", domainId, domainName);
            throw new MediaDBException(message, ex);
        }
    }

    /**
     * Loads a list of media items based on a domain id.
     *
     * @param domainId Id of the domain item the media is required.
     * @return The list of media attached to the domain id.
     */
    public List<Media> loadMedia(Domain domain, String domainId) {
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
            return new ArrayList<>(dynamoMapper.query(Media.class, query))
                    .stream()
                    .filter(item -> !(Boolean.TRUE.equals(item.isHidden())))
                    .filter(item -> environment.equals(item.getEnvironment()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.error(e, "ERROR Loading Media Index={} Domain={} DomainId={} ErrorMessage={}",
                    "cs-mediadb-index-Media-DomainID-Domain", domain, domainId, e.getMessage());
            throw new MediaDBException(e.getMessage(), e);
        }
    }

    /**
     * Store the media Add message in dynamoDB
     *
     * @param imageMessage message to store.
     * @param thumbnail    Associated thumbnail.
     */
    public void storeMediaAddMessage(ImageMessage imageMessage, Thumbnail thumbnail) {
        try {
            dynamoMapper.save(buildMedia(imageMessage, thumbnail));
            LOGGER.info("Media successfully added in dynamodb", imageMessage);
            if (imageMessage.isGenerateThumbnail()) {
                dynamoMapper.save(buildDerivative(imageMessage, thumbnail));
                LOGGER.info("Thumbnail derivative successfully added in dynamodb ThumbnailDerivative={}",
                        Arrays.asList(String.valueOf(thumbnail)), imageMessage);
            }
        } catch (Exception e) {
            LOGGER.error(e, "ERROR when trying to save in dynamodb ErrorMessage={}", Arrays.asList(e.getMessage()), imageMessage);
            throw new MediaDBException(e.getMessage(), e);
        }
    }

    /**
     * Build a media object from the imageMessage.
     *
     * @param imageMessage imageMessage to use.
     * @param thumbnail    generated thumbnail.
     * @return returns the media
     */
    private Media buildMedia(ImageMessage imageMessage, Thumbnail thumbnail) throws Exception {
        MediaDerivative mediaDerivative = null;
        Metadata basicMetadata = null;
        if (thumbnail != null) {
            mediaDerivative = buildDerivative(imageMessage, thumbnail);
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
                .mediaGuid(imageMessage.getMediaGuid())
                .userId(imageMessage.getUserId())
                .provider(imageMessage.getOuterDomainData().getProvider()).build();
    }

    /**
     * Build a MediaDerivative object from the imageMessage and thumbnail object.
     *
     * @param imageMessage imageMessage to use.
     * @param thumbnail    thumbnail to use.
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
