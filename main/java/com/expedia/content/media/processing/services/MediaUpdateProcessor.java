package com.expedia.content.media.processing.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.expedia.content.media.processing.pipeline.kafka.KafkaCommonPublisher;
import com.expedia.content.media.processing.services.dao.mediadb.MediaDBMediaDao;
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

@Component
public class MediaUpdateProcessor {
    private static final String MESSAGE_PROPERTY_HERO = "propertyHero";
    private static final String MESSAGE_SUB_CATEGORY_ID = "subcategoryId";
    private static final String MESSAGE_ROOMS = "rooms";
    private static final String UPDATE_OPERATION = "mediaUpdate";

    @Value("${kafka.imagemessage.topic}")
    private String imageMessageTopic;
    @Autowired
    private KafkaCommonPublisher kafkaCommonPublisher;
    @Autowired
    private MediaDBMediaDao mediaDBMediaDao;
    @Value("${kafka.mediadb.update.enable}")
    private boolean enableMediaDBUpdate;

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
        mediaDBMediaDao.updateMediaOnImageMessage(updatedImageMessage);
        kafkaCommonPublisher.publishImageMessage(addUpdateOperationTag(updatedImageMessage), imageMessageTopic);
        final Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        final String jsonResponse = new ObjectMapper().writeValueAsString(response);
        return new ResponseEntity<>(jsonResponse, HttpStatus.OK);
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
     * Replaces 'room','subcategoryId', and 'propertyHero' in the domainFields value with the input JSON message.
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
            if (domainFieldsNew.get(MESSAGE_ROOMS) != null) {
                domainFieldsInDB.put(MESSAGE_ROOMS, domainFieldsNew.get(MESSAGE_ROOMS));
            }
            if (domainFieldsNew.get(MESSAGE_SUB_CATEGORY_ID) != null) {
                domainFieldsInDB.put(MESSAGE_SUB_CATEGORY_ID, domainFieldsNew.get(MESSAGE_SUB_CATEGORY_ID));
            }
            if (domainFieldsNew.get(MESSAGE_PROPERTY_HERO) != null) {
                domainFieldsInDB.put(MESSAGE_PROPERTY_HERO, domainFieldsNew.get(MESSAGE_PROPERTY_HERO));
            }
            return domainFieldsInDB;
        }
    }

    /**
     * Adds 'mediaUpdate' operation to the ImageMessage's 'operation' property to indicate to the lcm-consumer that the updateImageMessage is a update.
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
