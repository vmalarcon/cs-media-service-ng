package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.pipeline.kafka.KafkaCommonPublisher;
import com.expedia.content.media.processing.pipeline.reporting.Activity;
import com.expedia.content.media.processing.pipeline.reporting.App;
import com.expedia.content.media.processing.pipeline.reporting.LogActivityProcess;
import com.expedia.content.media.processing.pipeline.reporting.LogEntry;
import com.expedia.content.media.processing.pipeline.reporting.Reporting;
import com.expedia.content.media.processing.pipeline.retry.RetryableMethod;
import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.domain.Thumbnail;
import com.expedia.content.media.processing.services.util.DomainDataUtil;
import com.expedia.content.media.processing.services.util.FileNameUtil;
import com.expedia.content.media.processing.services.util.MediaReplacement;
import com.fasterxml.jackson.databind.ObjectMapper;
import expedia.content.solutions.metrics.annotations.Meter;
import expedia.content.solutions.metrics.annotations.Timer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static com.expedia.content.media.processing.pipeline.util.SQSUtil.sendMessageToQueue;

/**
 * A Class to handle the Processing of MediaAdd Requests.
 * Functions involve dealing with reprocessing media vs new added media, logging processing activities, and sending messages to SQS and Kafka.
 *
 */
@Component
public class MediaAddProcessor {
    private static final FormattedLogger LOGGER = new FormattedLogger(MediaAddProcessor.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String MEDIA_CLOUD_ROUTER_CLIENT_ID = "Media Cloud Router";
    private static final String RESPONSE_FIELD_LCM_MEDIA_ID = "lcmMediaId";
    private static final String RESPONSE_FIELD_MEDIA_GUID = "mediaGuid";
    private static final String RESPONSE_FIELD_STATUS = "status";
    private static final String RESPONSE_FIELD_THUMBNAIL_URL = "thumbnailUrl";
    private static final String ERROR_MESSAGE = "error message";
    private static final String REJECTED_STATUS = "REJECTED";
    private static final String REPROCESS_OPERATION = "reprocess";


    @Resource(name = "providerProperties")
    private Properties providerProperties;
    @Value("${kafka.imagemessage.topic}")
    private String imageMessageTopic;
    @Value("${kafka.imagemessage.topic.retry}")
    private String imageMessageRetryTopic;
    @Value("${media.aws.collector.queue.name}")
    private String publishQueue;
    private final QueueMessagingTemplate messagingTemplate;
    private final LogActivityProcess logActivityProcess;
    private final Reporting reporting;
    private final ThumbnailProcessor thumbnailProcessor;
    private final MediaDao mediaDao;
    private final KafkaCommonPublisher kafkaCommonPublisher;

    @Autowired
    public MediaAddProcessor(MediaDao mediaDao, KafkaCommonPublisher kafkaCommonPublisher, ThumbnailProcessor thumbnailProcessor, LogActivityProcess logActivityProcess,
                             Reporting reporting, QueueMessagingTemplate messagingTemplate) {
        this.mediaDao = mediaDao;
        this.kafkaCommonPublisher = kafkaCommonPublisher;
        this.thumbnailProcessor = thumbnailProcessor;
        this.logActivityProcess = logActivityProcess;
        this.reporting = reporting;
        this.messagingTemplate = messagingTemplate;

    }

    /**
     * Processes a MediaAdd request building a filled out ImageMessage, saving/updating the ImageMessage in the MediaDB, publishing the ImageMessage to Kafka and SQS,
     * and returns a ResponseEntity.
     * Note: If any errors occur in processing, due to bad data, an Error Response entity is return,
     *       otherwise a Successful Response is returned with the media guid attached.
     *
     * @param message JSON formatted ImageMessage.
     * @param requestID The id of the request. Used for tracking purposes.
     * @param serviceUrl URL of the message called.
     * @param clientId Web service client id.
     * @param successStatus Status to return when successful.
     * @param timeReceived The time at which MediaService received the request
     * @return The response for the service call.
     * @throws Exception Thrown if the message can't be validated or the response can't be serialized.
     */
    @SuppressWarnings({"PMD.PrematureDeclaration", "PMD.CyclomaticComplexity", "PMD.NPathComplexity"})
    public ResponseEntity<String> processRequest(final String message, final String requestID,
                                                  final String serviceUrl, final String clientId, HttpStatus successStatus, Date timeReceived) throws Exception {
        final ImageMessage imageMessage = buildImageMessageFromJSONMessage(message, requestID, clientId);
        imageMessage.addLogEntry(new LogEntry(App.MEDIA_SERVICE, Activity.RECEPTION, timeReceived));
        logActivity(imageMessage, Activity.RECEPTION, timeReceived);
        final Map<String, String> response = new HashMap<>();
        response.put(RESPONSE_FIELD_MEDIA_GUID, imageMessage.getMediaGuid());
        response.put(RESPONSE_FIELD_STATUS, "RECEIVED");
        // Verifies if the Json Message received requests a Thumbnail to be generated.
        // Note: If generating a thumbnail fails, downstream Processes such as the Derivative Creator and Collector will also fail
        //       (due to doing the exact same calls), so an Error Response is returned to stop further processing downstream.
        if (imageMessage.isGenerateThumbnail()) {
            try {
                final Thumbnail thumbnail = thumbnailProcessor.createThumbnail(imageMessage);
                response.put(RESPONSE_FIELD_THUMBNAIL_URL, thumbnail.getLocation());
            } catch (Exception e) {
                response.put(RESPONSE_FIELD_STATUS, REJECTED_STATUS);
                response.put(ERROR_MESSAGE, e.getLocalizedMessage());
                return new ResponseEntity<>(OBJECT_MAPPER.writeValueAsString(response), HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
        // checks if the media is a reprocess, updating the record if it is, and inserting a record if it is not.
        if (StringUtils.isNotEmpty(imageMessage.getOperation()) && imageMessage.getOperation().contains(REPROCESS_OPERATION)) {
            LOGGER.info("Started updating media in MediaDB MediaGuid={} RequestId={} ClientId={}", imageMessage.getMediaGuid(), requestID, clientId);
            mediaDao.updateMedia(imageMessage);
            LOGGER.info("Finished updating media in MediaDB MediaGuid={} RequestId={} ClientId={}", imageMessage.getMediaGuid(), requestID, clientId);
        } else {
            LOGGER.info("Started inserting media in MediaDB MediaGuid={} RequestId={} ClientId={}", imageMessage.getMediaGuid(), requestID, clientId);
            mediaDao.addMedia(imageMessage);
            LOGGER.info("Finished inserting media in MediaDB MediaGuid={} RequestId={} ClientId={}", imageMessage.getMediaGuid(), requestID, clientId);

        }
        publishMsg(imageMessage);
        final ResponseEntity<String> responseEntity = new ResponseEntity<>(OBJECT_MAPPER.writeValueAsString(response), successStatus);
        LOGGER.info("SUCCESS ResponseStatus={} ResponseBody={} ServiceUrl={}",
                Arrays.asList(responseEntity.getStatusCode().toString(), responseEntity.getBody(), serviceUrl), imageMessage);
        return responseEntity;
    }

    /**
     * Builds an ImageMessage by parsing a Json String message. This method adds appropriate fields based on whether the message is a
     * reprocess method or not; generating a MediaGuid and resolving a filename in the latter case.
     *
     * @param message The Json String from which to parse, to build and add the appropriate fields to an ImageMessage
     * @param requestId The id of the request. Used for tracking purposes.
     * @param clientId Web service client id.
     * @return A Map contains the updated message with request and other data added
     * and if the file is checked for reprocessing .
     */
    private ImageMessage buildImageMessageFromJSONMessage(final String message, final String requestId, final String clientId) {
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(message);
        final ImageMessage.ImageMessageBuilder imageMessageBuilder = imageMessage.createBuilderFromMessage();
        OuterDomain outerDomain = imageMessage.getOuterDomainData();
        final boolean isReprocessMedia = verifyReprocessMediaAndUpdateImageMessage(imageMessage.getFileName(), imageMessageBuilder, outerDomain, clientId, requestId);
        if (!isReprocessMedia) {
            imageMessageBuilder.mediaGuid(UUID.randomUUID().toString());
            final String domainProvider = getDomainProviderFromMapping(outerDomain);
            outerDomain = OuterDomain.builder().from(outerDomain).mediaProvider(domainProvider).build();
            imageMessageBuilder.outerDomainData(outerDomain);
            if (imageMessage.getProvidedName() == null) {
                imageMessageBuilder.providedName(resolveProvidedName(imageMessage));
            }
            imageMessageBuilder.fileName(FileNameUtil.resolveFileNameByProvider(imageMessageBuilder.build()));
        }
        return imageMessageBuilder.clientId(clientId).requestId(String.valueOf(requestId)).build();
    }

    /**
     * Verifies if a Media is a reprocess message, base on file name and clientId, and merges the originalMedia data into the ImageMessage
     * being process if it is a reprocess message.
     * Note: Only media sent from the Media Cloud Router with clientId "Media Cloud Router" are eligible media to be consider reprocess media.
     *
     *
     * @param filename The filename of the media to verify.
     * @param imageMessageBuilder The Builder of the ImageMessage being processed.
     * @param outerDomain The OuterDomain data of the ImageMessage being processed.
     * @param clientId The clientId of the ImageMessage being processed.
     * @param requestId The requestId of the ImageMessage being processed.
     * @return If the media being processed is a reprocess message true is returned, otherwise false.
     */
    private boolean verifyReprocessMediaAndUpdateImageMessage(String filename, ImageMessage.ImageMessageBuilder imageMessageBuilder, OuterDomain outerDomain,
                                                              String clientId, String requestId) {
        if (MEDIA_CLOUD_ROUTER_CLIENT_ID.equals(clientId)) {
            final Optional<Media> optionalMedia = findMediaToReprocess(filename, outerDomain.getDomainId(), outerDomain.getProvider());
            if (optionalMedia.isPresent()) {
                final Media originalMedia = optionalMedia.get();
                addOriginalMediaDataToImageMessage(originalMedia, imageMessageBuilder, outerDomain);
                LOGGER.info("REPLACEMENT MEDIA RequestId={} MediaGuid={} lcmMediaId={}", requestId, originalMedia.getMediaGuid(), originalMedia.getDomainId());
                return true;
            }
        }
        return false;
    }

    /**
     * Looks for the original Media to reprocess base on FileName, DomainId, and Provider.
     *
     * @param fileName The file name of the ImageMessage being processed.
     * @param domainId The domainId of the ImageMessage being processed.
     * @param provider The provider of the ImageMessage being processed.
     * @return A Media Object (if it exists) of the Original Media to reprocess in the MediaId.
     */
    private Optional<Media> findMediaToReprocess(String fileName, String domainId, String provider) {
        final List<Optional<Media>> mediaList = mediaDao.getMediaByFilename(fileName);
        return MediaReplacement.selectBestMedia(mediaList, domainId, provider);
    }


    /**
     * Merges the data from the originalMedia into the ImageMessageBuilder of the message being processed.
     * Note: This is only used for reprocess media messages so that the media get the same MediaGuid, and LcmMediaId.
     * Note: This method will add the Reprocess Operation to the operation field for down stream processes to
     * be aware that this message is a reprocess media message.
     *
     * @param originalMedia The original Media to extract MediaGuid and LcmMediaId from.
     * @param imageMessageBuilder The Builder object of the ImageMessage being processed to add orignal Media Data to.
     * @param outerDomain The OuterDomain data of the ImageMessage being processed.
     */
    private void addOriginalMediaDataToImageMessage(Media originalMedia, ImageMessage.ImageMessageBuilder imageMessageBuilder, OuterDomain outerDomain) {
        final OuterDomain.OuterDomainBuilder domainBuilder = OuterDomain.builder().from(outerDomain);
        domainBuilder.addField(RESPONSE_FIELD_LCM_MEDIA_ID, originalMedia.getLcmMediaId());
        imageMessageBuilder.outerDomainData(domainBuilder.build());
        imageMessageBuilder.mediaGuid(originalMedia.getMediaGuid());
        imageMessageBuilder.providedName(originalMedia.getProvidedName());
        imageMessageBuilder.operation(REPROCESS_OPERATION);
    }

    /**
     * Resolves which fileName should be used as the ProvidedName. If the fileName field does not exist a name
     * is extracted from the FileURL.
     * Note: this method will only be called on new media sent through MediaAdd.
     * message is parsed and Reprocessed Media will never end up in the branch in @buildImageMessageFromJSONMessage()
     * that calls this method
     *
     * @param imageMessage an imageMessage that does has null for the providedName field
     * @return the fileName to use for the providedName field
     */
    private String resolveProvidedName(final ImageMessage imageMessage) {
        return (imageMessage.getFileName() == null) ?
                FileNameUtil.getFileNameFromUrl(imageMessage.getFileUrl()) :
                imageMessage.getFileName();
    }

    /**
     * get the domainProvider text from the mapping regardless of case-sensitivity
     * if the exact text is not passed, the default is set to 1.
     *
     * @param outerDomain The OuterDomain data of the ImageMessage being processed.
     * @return outerDomain with domainProvider replaced by the exact domainProvider from the mapping
     */
    private String getDomainProviderFromMapping(OuterDomain outerDomain) {
        return DomainDataUtil.getDomainProvider(outerDomain.getProvider(), providerProperties);
    }

    /**
     * Handles publishing a filled out ImageMessage to SQS and Kafka for downstream processes.
     * Note: that the {@code @Meter} {@code @Timer} {@code @RetryableMethod} annotations introduce aspects from metrics-support and spring-retry
     * modules. The aspects should be applied in order, Metrics being outside (outer) and retry being inside (inner).
     *
     * @param message is the received json format message from main application
     */
    @Meter(name = "publishMessageCounter")
    @Timer(name = "publishMessageTimer")
    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    @RetryableMethod
    private void publishMsg(final ImageMessage message) {
        message.addLogEntry(new LogEntry(App.MEDIA_SERVICE, Activity.MEDIA_MESSAGE_RECEIVED, new Date()));
        try {
            LOGGER.info("Publishing to SQS", message);
            sendMessageToQueue(messagingTemplate, publishQueue, message);
            logActivity(message, Activity.MEDIA_MESSAGE_RECEIVED, null);
            LOGGER.info("Publishing to Kafka", message);
            kafkaCommonPublisher.publishImageMessage(message, imageMessageTopic, imageMessageRetryTopic);
        } catch (Exception ex) {
            LOGGER.error(ex, "Error publishing ErrorMessage={}", Arrays.asList(ex.getMessage()), message);
            throw new RuntimeException("Error publishing message=[" + message.toJSONMessage() + "]", ex);
        }
    }

    /**
     * Logs a completed activity, its time, and ExepdiaId are appended before the file name
     *
     * @param imageMessage The imageMessage of the file being processed.
     * @param activity The activity to log.
     * @param date The timestamp at which the activity happened, if null the latest timestamp will be generated .
     */
    private void logActivity(final ImageMessage imageMessage, final Activity activity, final Date date) throws URISyntaxException {
        final Date logDate = (date == null) ? new Date() : date;
        final LogEntry logEntry =
                new LogEntry(imageMessage.getFileName(), imageMessage.getMediaGuid(), activity, logDate, imageMessage.getOuterDomainData().getDomain(),
                        imageMessage.getOuterDomainData().getDomainId(), imageMessage.getOuterDomainData().getDerivativeCategory());
        logActivityProcess.log(logEntry, reporting);

    }
}
