package com.expedia.content.media.processing.services;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

import java.util.HashMap;
import java.util.Map;

import com.expedia.content.media.processing.services.validator.ValidationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.expedia.content.media.processing.services.reqres.TempDerivativeMessage;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.expedia.content.media.processing.services.util.MediaServiceUrl;
import com.expedia.content.media.processing.services.util.RequestMessageException;
import com.expedia.content.media.processing.services.validator.TempDerivativeMVELValidator;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Web service controller for temporary derivatives.
 */
@RestController
public class TempDerivativeController extends CommonServiceController {
    private static final Logger LOGGER = LoggerFactory.getLogger(TempDerivativeController.class);
    private static final String RESPONSE_FIELD_THUMBNAIL_URL = "thumbnailUrl";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private ThumbnailProcessor thumbnailProcessor;
    @Autowired
    private TempDerivativeMVELValidator tempDerivativeMVELValidator;

    /**
     * Web services interface to create a temporary derivative of a given image with given specifications.
     *
     * @param message JSON formated TempDerivativeMessage.
     * @param headers request Headers.
     * @return url of the generated temporary derivative.
     * @throws Exception
     */
    @RequestMapping(value = "/media/v1/tempderivative", method = RequestMethod.POST)
    public ResponseEntity<String> getTempDerivative(@RequestBody final String message,
                                                    @RequestHeader MultiValueMap<String, String> headers) throws Exception {
        final String requestID = this.getRequestId(headers);
        final String serviceUrl = MediaServiceUrl.MEDIA_TEMP_DERIVATIVE.getUrl();
        LOGGER.info("RECEIVED REQUEST - messageName={}, requestId=[{}], JSONMessage=[{}]", serviceUrl, requestID, message);
        try {
            final TempDerivativeMessage tempDerivativeMessage = buildTempDerivativeFromJSONMessage(message);
            final String errors = tempDerivativeMVELValidator.validateTempDerivativeMessage(tempDerivativeMessage);
            if (!errors.isEmpty()) {
                LOGGER.error("ERROR - messageName={}, error=[{}], requestId=[{}], JSONMessage=[{}].", serviceUrl, errors, requestID, message);
                return this.buildErrorResponse("JSON request format is invalid. " + errors + " Json message=" + message, serviceUrl, BAD_REQUEST);
            }
            final ValidationStatus fileValidation = verifyUrlExistence(tempDerivativeMessage.getFileUrl());
            if (!fileValidation.isValid()) {
                if (BAD_REQUEST.equals(fileValidation.getStatus())) {
                    LOGGER.info("Response not found. Provided 'fileUrl does not exist' for requestId=[{}], message=[{}]", requestID, message);
                } else {
                    LOGGER.info("Returning bad request. Provided 'file is 0 Bytes' for requestId=[{}], message=[{}]", requestID, message);
                }
                return this.buildErrorResponse(fileValidation.getMessage(), serviceUrl, fileValidation.getStatus());
            }
            final Map<String, String> response = new HashMap<>();
            response.put(RESPONSE_FIELD_THUMBNAIL_URL, thumbnailProcessor.createTempDerivativeThumbnail(tempDerivativeMessage));
            return new ResponseEntity<>(OBJECT_MAPPER.writeValueAsString(response), OK);
        } catch (Exception ex) {
            LOGGER.error("ERROR - messageName={}, error=[{}], requestId=[{}], JSONMessage=[{}].", serviceUrl, ex.getMessage(), requestID, message, ex);
            return this.buildErrorResponse("JSON request format is invalid. Json message=" + message, serviceUrl, BAD_REQUEST);
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