package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.pipeline.util.Poker;
import com.expedia.content.media.processing.services.reqres.TempDerivativeMessage;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.expedia.content.media.processing.services.util.MediaServiceUrl;
import com.expedia.content.media.processing.services.util.RequestMessageException;
import com.expedia.content.media.processing.services.validator.TempDerivativeMVELValidator;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

/**
 * Web service controller for temporary derivatives.
 */
@RestController
public class TempDerivativeController extends CommonServiceController {
    private static final FormattedLogger LOGGER = new FormattedLogger(TempDerivativeController.class);
    private static final String RESPONSE_FIELD_THUMBNAIL_URL = "thumbnailUrl";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Map<String, HttpStatus> STATUS_MAP = new HashMap<>();
    static {
        STATUS_MAP.put(ValidationStatus.NOT_FOUND, NOT_FOUND);
        STATUS_MAP.put(ValidationStatus.ZERO_BYTES, BAD_REQUEST);
        STATUS_MAP.put(ValidationStatus.VALID, OK);
    }

    @Autowired
    private ThumbnailProcessor thumbnailProcessor;
    @Autowired
    private TempDerivativeMVELValidator tempDerivativeMVELValidator;
    @Value("${cs.poke.hip-chat.room}")
    private String hipChatRoom;
    @Autowired
    private Poker poker;

    /**
     * Web services interface to create a temporary derivative of a given image with given specifications.
     *
     * @param message JSON formated TempDerivativeMessage.
     * @param headers request Headers.
     * @return url of the generated temporary derivative.
     * @throws Exception
     */
    @RequestMapping(value = "/media/v1/tempderivative", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> getTempDerivative(@RequestBody final String message, @RequestHeader MultiValueMap<String,String> headers) throws Exception {
        final String requestID = this.getRequestId(headers);
        final String serviceUrl = MediaServiceUrl.MEDIA_TEMP_DERIVATIVE.getUrl();
        LOGGER.info("RECEIVED TEMP DERIVATIVE REQUEST ServiceUrl={} RequestId={} RequestMessage={}", serviceUrl, requestID, message);
        try {
            final TempDerivativeMessage tempDerivativeMessage = buildTempDerivativeFromJSONMessage(message);
            final String errors = tempDerivativeMVELValidator.validateTempDerivativeMessage(tempDerivativeMessage);
            if (!errors.isEmpty()) {
                LOGGER.error("ERROR ServiceUrl={} ErrorMessage={} RequestId={} RequestMessage={}", serviceUrl, errors, requestID, message);
                return this.buildErrorResponse("JSON request format is invalid. " + errors + " Json message=" + message, serviceUrl, BAD_REQUEST);
            }
            @SuppressWarnings("CPD-START")
            final ValidationStatus fileValidation = verifyUrl(tempDerivativeMessage.getFileUrl());
            if (!fileValidation.isValid()) {
                final ResponseEntity<String> responseEntity = this.buildErrorResponse(fileValidation.getMessage(), serviceUrl, STATUS_MAP.get(fileValidation.getStatus()) == null ?
                        BAD_REQUEST : STATUS_MAP.get(fileValidation.getStatus()));
                switch (fileValidation.getStatus()) {
                    case ValidationStatus.NOT_FOUND:
                        LOGGER.info("NOT FOUND Reason=\"Provided 'fileUrl does not exist'\" ServiceUrl={} RequestId={}", Arrays.asList(serviceUrl, requestID), message);
                        break;
                    case ValidationStatus.ZERO_BYTES:
                        LOGGER.info("BAD REQUEST Reason=\"Provided 'file is 0 Bytes'\" ServiceUrl={} RequestId={}", Arrays.asList(serviceUrl, requestID), message);
                        break;
                    default:
                        LOGGER.info("BAD REQUEST ServiceUrl={} RequestId={}", Arrays.asList(serviceUrl, requestID), message);
                        break;
                }
                return responseEntity;
            }
            @SuppressWarnings("CPD-END")
            final Map<String, String> response = new HashMap<>();
            response.put(RESPONSE_FIELD_THUMBNAIL_URL, thumbnailProcessor.createTempDerivativeThumbnail(tempDerivativeMessage));
            return new ResponseEntity<>(OBJECT_MAPPER.writeValueAsString(response), OK);
        } catch (Exception ex) {
            LOGGER.error(ex, "ERROR ServiceUrl={} ErrorMessage={} RequestId={} RequestMessage={}", serviceUrl, ex.getMessage(), requestID, message);
            poker.poke("Media Services failed to process a getTempDerivative request - RequestId: " + requestID, hipChatRoom,
                    message, ex);
            throw ex;
        }
    }

    private static TempDerivativeMessage buildTempDerivativeFromJSONMessage(String jsonMessage) throws RequestMessageException {
        final Map jsonMap = JSONUtil.buildMapFromJson(jsonMessage);
        final String fileUrl = (String) jsonMap.get("fileUrl");
        final String rotation = (String) jsonMap.get("rotation");
        final Integer width = (Integer) jsonMap.get("width");
        final Integer height = (Integer) jsonMap.get("height");
        return TempDerivativeMessage.builder()
                .fileUrl(fileUrl)
                .rotation(rotation)
                .width(width)
                .height(height)
                .build();
    }
}
