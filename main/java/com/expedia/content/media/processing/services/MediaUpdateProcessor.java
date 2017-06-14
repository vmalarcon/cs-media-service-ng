package com.expedia.content.media.processing.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.expedia.content.media.processing.pipeline.kafka.KafkaCommonPublisher;
import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.services.dao.MediaDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.StringUtils;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Helper class for processing Media Update requests.
 */
@Component
public class MediaUpdateProcessor {
    private static final FormattedLogger LOGGER = new FormattedLogger(MediaUpdateProcessor.class);
    private static final String UPDATE_OPERATION = "mediaUpdate";

    @Value("${kafka.imagemessage.topic}")
    private String imageMessageTopic;
    @Value("${kafka.imagemessage.topic.retry}")
    private String imageMessageRetryTopic;
    private final KafkaCommonPublisher kafkaCommonPublisher;
    private final MediaDao mediaDao;

    @Autowired
    public MediaUpdateProcessor(KafkaCommonPublisher kafkaCommonPublisher, MediaDao mediaDao) {
        this.kafkaCommonPublisher = kafkaCommonPublisher;
        this.mediaDao = mediaDao;
    }

    /**
     * Processed an Update ImageMessage request. Merges the data from the originalMedia (media from the MediaDB) with the updateImageMessage,
     * updates the MediaDB directly, and then pushes the newly updated message to the kafka imageMessageTopic.
     *
     * @param updateImageMessage The ImageMessage from the Update request.
     * @param originalMedia The Media record currently in the MediaDB.
     * @return A success Response if no exceptions occur during processing.
     * @throws Exception
     */
    @Transactional
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.NPathComplexity", "PMD.AvoidDeeplyNestedIfStmts"})
    public ResponseEntity<String> processRequest(final ImageMessage updateImageMessage,
                                                 Media originalMedia) throws Exception {
        final ImageMessage updatedImageMessage = buildUpdatedImageMessage(updateImageMessage, originalMedia);
        LOGGER.info("Started updating media in MediaDB MediaGuid={}", originalMedia.getMediaGuid());
        mediaDao.updateMedia(updatedImageMessage);
        unHeroMedia(updatedImageMessage, originalMedia.getDomainId());
        LOGGER.info("Finished updating media in MediaDB MediaGuid={}", originalMedia.getMediaGuid());
        // TODO: Update all the media that needs to be unhero'd in mediaDB and send them to kafka as well.
        kafkaCommonPublisher.publishImageMessage(addUpdateOperationTag(updatedImageMessage), imageMessageTopic, imageMessageRetryTopic);
        final Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.OK.value());
        final String jsonResponse = new ObjectMapper().writeValueAsString(response);
        return new ResponseEntity<>(jsonResponse, HttpStatus.OK);
    }

    /**
     * unhero current hero image in mediaDB
     * @param imageMessage
     * @param domainId
     */
    @SuppressWarnings("PMD")
    private void unHeroMedia(ImageMessage imageMessage, String domainId) {
        if (imageMessage.getOuterDomainData() != null
                && imageMessage.getOuterDomainData().getDomainFields() != null
                && ("true").equals(imageMessage.getOuterDomainData().getDomainFields().get("propertyHero"))
                && imageMessage.getOuterDomainData().getDomain() != null
                && ("Lodging").equals(imageMessage.getOuterDomainData().getDomain().getDomain())) {
            LOGGER.info("Started query media by domainId={}", domainId);
            final List<Optional<Media>> currentHeroList = mediaDao.getHeroMediaByDomainId(domainId);
            LOGGER.info("end query media by domainId={} heroListSize={}", imageMessage.getOuterDomainData().getDomainId(), currentHeroList.size());
            currentHeroList
                    .stream()
                    .filter(Optional::isPresent)
                    .filter(media -> !media.get().getMediaGuid().equals(imageMessage.getMediaGuid()))
                    .map(Optional::get)
                    .forEach(media -> {
                        mediaDao.unheroMedia(media.getMediaGuid(),
                                media.getDomainFields().replace("\"propertyHero\":\"true\"", "\"propertyHero\":\"false\""));
                        try {
                            kafkaCommonPublisher
                                    .publishImageMessage(media.toImageMessage(), imageMessageTopic, imageMessageRetryTopic);
                        } catch (Exception ex) {
                            LOGGER.error(ex, "send kafka message failed imageMessage={}", imageMessage.toJSONMessage());
                        }
                    });
        }
    }

    /**
     * Merges the data from the updateImageMessage with the originalMedia and then produces an ImageMessage with all of its fields up to date.
     *
     * @param updateImageMessage The ImageMessage from the Update request.
     * @param originalMedia The Media record currently in the MediaDB.
     * @return an up to date ImageMessage containing data from both the updateImageMessage and the originalMedia.
     * @throws Exception
     */
    @SuppressWarnings("PMD.NPathComplexity")
    private ImageMessage buildUpdatedImageMessage(ImageMessage updateImageMessage, Media originalMedia) throws Exception {
        if (updateImageMessage.getComment() != null) {
            final List<String> commentList = new ArrayList<>();
            commentList.add(updateImageMessage.getComment());
            originalMedia.setCommentList(commentList);
        }
        if (updateImageMessage.isActive() != null) {
            originalMedia.setActive(updateImageMessage.isActive() ? "true" : "false");
        }
        if (updateImageMessage.getHidden() != null) {
            originalMedia.setHidden(updateImageMessage.getHidden());
        }
        originalMedia.setUserId(StringUtils.isEmpty(updateImageMessage.getUserId()) ? updateImageMessage.getClientId() : updateImageMessage.getUserId());
        originalMedia.setLastUpdated(new Date());
        final String domainFields = originalMedia.getDomainFields();
        final Map<String, Object> domainFieldsInDB = domainFields == null ? new HashMap<>() : JSONUtil.buildMapFromJson(domainFields);
        final Map<String, Object> domainFieldsNew = updateImageMessage.getOuterDomainData().getDomainFields();
        final Map<String, Object> domainFieldsCombine = combineDomainFields(domainFieldsInDB, domainFieldsNew);
        originalMedia.setDomainFields(new ObjectMapper().writeValueAsString(domainFieldsCombine));
        return originalMedia.toImageMessage();
    }

    /**
     * Replaces fields in the current domainFields with the domainFields in the input JSON message.
     *
     * @param domainFieldsInDB The domain fields from the database.
     * @param domainFieldsNew The domain fields from the update message.
     * @return combined DomainFields
     */
    private Map<String, Object> combineDomainFields(Map<String, Object> domainFieldsInDB, Map<String, Object> domainFieldsNew) {
        if (domainFieldsNew == null || domainFieldsNew.isEmpty()) {
            return domainFieldsInDB;
        } else if (domainFieldsInDB == null || domainFieldsInDB.isEmpty()) {
            return domainFieldsNew;
        } else {
            domainFieldsInDB.putAll(domainFieldsNew);
            return domainFieldsInDB;
        }
    }

    /**
     * Adds 'mediaUpdate' operation to the ImageMessage's 'operation' property to indicate to the lcm-consumer that the updateImageMessage is a update.
     *
     * @param imageMessage The ImageMessage to add the operation tag to.
     * @return ImageMessage with operation tag added.
     */
    private ImageMessage addUpdateOperationTag(final ImageMessage imageMessage) {
        ImageMessage.ImageMessageBuilder imageMessageBuilder = new ImageMessage.ImageMessageBuilder();
        imageMessageBuilder = imageMessageBuilder.transferAll(imageMessage);
        imageMessageBuilder.operation(UPDATE_OPERATION);
        return imageMessageBuilder.build();
    }
}
