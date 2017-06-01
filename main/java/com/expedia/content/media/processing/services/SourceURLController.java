package com.expedia.content.media.processing.services;

import com.amazonaws.services.s3.model.S3Object;
import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.pipeline.util.ImageCopy;
import com.expedia.content.media.processing.pipeline.util.Poker;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.exception.MediaNotFoundException;
import com.expedia.content.media.processing.services.util.FileSourceFinder;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.expedia.content.media.processing.services.util.MediaServiceUrl;
import com.expedia.content.media.processing.services.exception.RequestMessageException;
import com.fasterxml.jackson.databind.ObjectMapper;
import expedia.content.solutions.metrics.annotations.Meter;
import expedia.content.solutions.metrics.annotations.Timer;
import org.apache.commons.io.IOUtils;
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

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Web service controller to get Source URL by derivative file name..
 */
@Component
@RestController
public class SourceURLController extends CommonServiceController {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final FormattedLogger LOGGER = new FormattedLogger(SourceURLController.class);

    @Value("${media.bucket.name}")
    private String bucketName;
    @Value("${cs.poke.hip-chat.room}")
    private String hipChatRoom;
    private final ImageCopy imageCopy;
    private final FileSourceFinder fileSourceFinder;
    private final Poker poker;

    @Autowired
    public SourceURLController(ImageCopy imageCopy, FileSourceFinder fileSourceFinder, Poker poker) {
        this.imageCopy = imageCopy;
        this.fileSourceFinder = fileSourceFinder;
        this.poker = poker;
    }

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
        try {
            final Map messageMap = JSONUtil.buildMapFromJson(message);
            final String mediaUrl = (String) messageMap.get("mediaUrl");
            if (mediaUrl == null) {
                return buildErrorResponse("mediaUrl is required in message.", MediaServiceUrl.MEDIA_SOURCEURL.getUrl(), BAD_REQUEST);
            }
            final Media media = fileSourceFinder.getMediaByDerivativeUrl(mediaUrl).orElseThrow(() -> new MediaNotFoundException("Requested resource with mediaUrl " + mediaUrl + " was not found."));
            final Map<String, String> responseMap = new HashMap<>();
            responseMap.put("contentProviderMediaName", media.getFileName());
            responseMap.put("mediaSourceUrl", media.getSourceUrl());
            final String jsonResponse = OBJECT_MAPPER.writeValueAsString(responseMap);
            final ResponseEntity response = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
            LOGGER.info("SOURCE URL RESPONSE ServiceUrl={} ResponseMessage={} RequestId={}", MediaServiceUrl.MEDIA_SOURCEURL.getUrl(), response.getBody(), requestID);
            return response;
        } catch (MediaNotFoundException ex) {
            return buildErrorResponse(ex.getMessage(), MediaServiceUrl.MEDIA_SOURCEURL.getUrl(), NOT_FOUND);
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
        final String objectName = fromUrl.substring(("s3://" + bucketName).length() + 1);
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
