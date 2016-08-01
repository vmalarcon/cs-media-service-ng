package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.util.Poker;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.util.FileSourceFinder;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.expedia.content.media.processing.services.util.MediaServiceUrl;
import com.expedia.content.media.processing.services.util.RequestMessageException;
import com.fasterxml.jackson.databind.ObjectMapper;
import expedia.content.solutions.metrics.annotations.Meter;
import expedia.content.solutions.metrics.annotations.Timer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Web service controller to get Source URL by derivative file name..
 */
@Component
@RestController
public class SourceURLController extends CommonServiceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourceURLController.class);
    @Autowired
    private MediaDao mediaDao;
    @Autowired
    FileSourceFinder fileSourceFinder;
    @Value("${media.bucket.name}")
    private String bucketName;
    @Value("${media.bucket.prefix.name}")
    private String bucketPrefix;
    @Value("${cs.poke.hip-chat.room}")
    private String hipChatRoom;
    @Autowired
    private Poker poker;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Web service interface to source URL and contentProviderName from  derivative file name.
     *
     * @param message JSON formatted message, "mediaNames", contains an array of media file names.
     * @return ResponseEntity Standard spring response object.
     * @throws Exception Thrown if processing the message fails.
     */
    @SuppressWarnings({"rawtypes", "unchecked", "PMD.SignatureDeclareThrowsException"})
    @Meter(name = "mediaSourceURLCounter")
    @Timer(name = "mediaSourceURLTimer")
    @RequestMapping(value = "/media/v1/sourceurl", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity getSourceURL(@RequestBody final String message, @RequestHeader MultiValueMap<String, String> headers) throws Exception {
        final String requestID = getRequestId(headers);
        LOGGER.info("RECEIVED REQUEST - url=[{}], message=[{}], requestId=[{}]", MediaServiceUrl.MEDIA_SOURCEURL.getUrl(), message,
                requestID);
        String jsonResponse = null;
        try {
            final Map messageMap = JSONUtil.buildMapFromJson(message);
            final String fileUrl = (String) messageMap.get("mediaUrl");
            if (fileUrl == null) {
                return this.buildErrorResponse("mediaUrl is required in message.", MediaServiceUrl.MEDIA_SOURCEURL.getUrl(), BAD_REQUEST);
            }
            final String fileName = fileSourceFinder.getFileNameFromUrl(fileUrl);
            final LcmMedia lcmMedia = mediaDao.getContentProviderName(fileName);
            if (!checkDBResultValid(lcmMedia)) {
                return buildErrorResponse(fileUrl + " not found.", MediaServiceUrl.MEDIA_SOURCEURL.getUrl(), NOT_FOUND);
            }
            String guid = "";
            if (fileSourceFinder.matchGuid(fileName)) {
                guid = getGuidByMediaId(lcmMedia.getMediaId().toString());
                if (guid == null) {
                    return buildErrorResponse("can not found GUID.", MediaServiceUrl.MEDIA_SOURCEURL.getUrl(), NOT_FOUND);
                }
            }
            final String sourcePath = fileSourceFinder.getSourcePath(bucketName, bucketPrefix, fileUrl, lcmMedia.getDomainId(), guid);
            final Map<String, String> response = new HashMap<>();
            response.put("contentProviderMediaName", lcmMedia.getFileName());
            response.put("mediaSourceUrl", sourcePath);
            jsonResponse = OBJECT_MAPPER.writeValueAsString(response);
            LOGGER.info("RESPONSE - url=[{}], responseMsg=[{}], requestId=[{}]", MediaServiceUrl.MEDIA_SOURCEURL.getUrl(), jsonResponse,
                    requestID);
        } catch (RequestMessageException ex) {
            LOGGER.error("ERROR - url=[{}], imageMessage=[{}], error=[{}], requestId=[{}]", MediaServiceUrl.MEDIA_SOURCEURL.getUrl(), message,
                    ex.getMessage(), ex, requestID);
            return buildErrorResponse(ex.getMessage(), MediaServiceUrl.MEDIA_SOURCEURL.getUrl(), BAD_REQUEST);
        } catch (Exception ex) {
            LOGGER.error("ERROR - serviceUrl={}, error=[{}], requestId=[{}], JSONMessage=[{}].", MediaServiceUrl.MEDIA_SOURCEURL.getUrl(), ex.getMessage(), requestID, message, ex);
            poker.poke("Media Services failed to process a getSourceURL request - RequestId: " + requestID, hipChatRoom,
                    message, ex);
            throw ex;
        }
        return new ResponseEntity<>(jsonResponse, HttpStatus.OK);
    }

    private boolean checkDBResultValid(LcmMedia lcmMedia) {
        if (lcmMedia == null || StringUtils.isBlank(lcmMedia.getFileName()) || lcmMedia.getDomainId() == null || lcmMedia.getMediaId() == null) {
            return false;
        }
        return true;
    }

    private String getGuidByMediaId(String mediaId) {
        if (org.apache.commons.lang.StringUtils.isNumeric(mediaId)) {
            final List<Media> mediaList = mediaDao.getMediaByMediaId(mediaId);
            if (!mediaList.isEmpty()) {
                return mediaList.stream().findFirst().get().getMediaGuid();
            }
        }
        return null;
    }

}
