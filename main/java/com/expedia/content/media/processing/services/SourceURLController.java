package com.expedia.content.media.processing.services;

import com.amazonaws.services.s3.model.S3Object;
import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.pipeline.util.ImageCopy;
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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Web service controller to get Source URL by derivative file name..
 */
@Component
@RestController
public class SourceURLController extends CommonServiceController {

    private static final FormattedLogger LOGGER = new FormattedLogger(SourceURLController.class);
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

    @Autowired
    private ImageCopy imageCopy;
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
    @RequestMapping(value = "/media/v1/sourceurl", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @Transactional
    public ResponseEntity getSourceURL(@RequestBody final String message, @RequestHeader MultiValueMap<String, String> headers) throws Exception {
        final String requestID = getRequestId(headers);
        LOGGER.info("RECEIVED SOURCE URL REQUEST ServiceUrl={} RequestMessage={} RequestId={}", MediaServiceUrl.MEDIA_SOURCEURL.getUrl(), message, requestID);
        String jsonResponse;
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
            final String guid = "";
            String sourcePath = "";
            if (fileSourceFinder.matchGuid(fileName)) {
                final Media dynamo = getDynamoByMediaId(lcmMedia.getMediaId().toString());
                if (dynamo == null) {
                    return buildErrorResponse("can not found GUID.", MediaServiceUrl.MEDIA_SOURCEURL.getUrl(), NOT_FOUND);
                }
                 sourcePath = dynamo.getSourceUrl();
            } else if (StringUtils.isEmpty(sourcePath)) {
                sourcePath = fileSourceFinder.getSourcePath(bucketName, bucketPrefix, fileUrl, guid, lcmMedia);
            }
            final Map<String, String> response = new HashMap<>();
            response.put("contentProviderMediaName", lcmMedia.getFileName());
            response.put("mediaSourceUrl", sourcePath);
            jsonResponse = OBJECT_MAPPER.writeValueAsString(response);
            LOGGER.info("SOURCE URL RESPONSE ServiceUrl={} ResponseMessage={} RequestId={}", MediaServiceUrl.MEDIA_SOURCEURL.getUrl(), jsonResponse, requestID);
        } catch (RequestMessageException ex) {
            final ResponseEntity<String> responseEntity = buildErrorResponse(ex.getMessage(), MediaServiceUrl.MEDIA_SOURCEURL.getUrl(), BAD_REQUEST);
            LOGGER.error(ex, "ERROR ResponseStatus={} ResponseBody={} ServiceUrl={} RequestMessage={} RequestId={} ErrorMessage={}",
                    responseEntity.getStatusCode().toString(), responseEntity.getBody(), MediaServiceUrl.MEDIA_SOURCEURL.getUrl(), message, requestID,
                    ex.getMessage());
            return responseEntity;
        } catch (Exception ex) {
            LOGGER.error(ex, "ERROR ServiceUrl={} RequestMessage={} RequestId={} ErrorMessage={}", MediaServiceUrl.MEDIA_SOURCEURL.getUrl(), message, requestID,
                    ex.getMessage());
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

    @SuppressWarnings("PMD")
    private Media getDynamoByMediaId(String mediaId) {
        if (StringUtils.isNumeric(mediaId)) {
            final List<Media> mediaList = mediaDao.getMediaByMediaId(mediaId);
            if (!mediaList.isEmpty()) {
                final Optional<Media> existMedia = mediaList.stream().max(Comparator.comparing(Media::getLastUpdated));
                if (existMedia.isPresent()) {
                    return existMedia.get();
                }
            }
        }
        return null;
    }

    /**
     * web service method to download source image as file stream.
     * @param message JSON formatted message with property 'mediaUrl'
     * @return image content as stream
     * @throws Exception when the provided URL is not found.
     */
    @RequestMapping(value = "/media/v1/sourceimage", method = RequestMethod.POST, produces = MediaType.IMAGE_JPEG_VALUE)
    @Meter(name = "mediaSourceImageCounter")
    @Timer(name = "mediaSourceImageTimer")
    public ResponseEntity<byte[]> download(@RequestBody final String message, @RequestHeader MultiValueMap<String, String> headers) throws Exception {
        final String requestID = getRequestId(headers);
        LOGGER.info("RECEIVED SOURCE IMAGE REQUEST ServiceUrl={} ResponseMessage={} RequestId={}",
                MediaServiceUrl.MEDIA_SOURCEIMAGE.getUrl(), message, requestID);
        final Map messageMap = JSONUtil.buildMapFromJson(message);
        final String fromUrl = (String) messageMap.get("mediaUrl");
        final String objectName = fromUrl.substring(("s3://"+bucketName).length()+1);
        try {
            final S3Object object = imageCopy.getImage(bucketName, objectName);
            if (object == null) {
                final ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(NOT_FOUND);
                LOGGER.error("Resource not found FileUrl={} ResponseStatus={}", fromUrl, responseEntity.getStatusCode().toString());
                return responseEntity;
            }
            final HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.valueOf(MediaType.IMAGE_JPEG_VALUE));
            return new ResponseEntity<>(IOUtils.toByteArray(object.getObjectContent()), responseHeaders, HttpStatus.OK);
        } catch (Exception ex) {
            LOGGER.error(ex, "ERROR ServiceUrl={} RequestMessage={} RequestId={} ErrorMessage={}", MediaServiceUrl.MEDIA_SOURCEIMAGE.getUrl(), message, requestID,
                    ex.getMessage());
            poker.poke("Media Services failed to process a sourceImage download request - RequestId: " + requestID, hipChatRoom,
                    message, ex);
            throw ex;
        }
    }


}
