package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.services.util.JSONUtil;
import com.expedia.content.media.processing.services.validator.HTTPValidator;
import com.expedia.content.media.processing.services.validator.S3Validator;
import com.expedia.content.media.processing.services.validator.ValidationStatus;
import expedia.content.solutions.metrics.annotations.Counter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

import static com.expedia.content.media.processing.services.util.URLUtil.patchURL;

/**
 * Common functionality shared by the service controllers.
 */
@SuppressWarnings({"PMD.AbstractClassWithoutAbstractMethod"})
public abstract class CommonServiceController {

    protected static final String REQUEST_ID = "request-id";
    protected static final String VALID_MESSAGE = "OK";
    protected static final String INVALID_MESSAGE = "Invalid Message.";

    /**
     * Builds an error response.
     *
     * @param errorMessage Failed message from validate.
     * @param resourcePath Path of the accessed resource.
     * @param httpStatus HTTP status of the error.
     * @return A response with an error code.
     */
    @Counter(name = "badRequestCounter")
    public ResponseEntity<String> buildErrorResponse(final String errorMessage, final String resourcePath,
            final HttpStatus httpStatus) {
        final String responseMessage = JSONUtil.generateJsonForErrorResponse(errorMessage, resourcePath, httpStatus.value(), httpStatus.getReasonPhrase());
        return new ResponseEntity<>(responseMessage, httpStatus);
    }

    /**
     * Pulls the request id from the header values.
     *
     * @param headers Header value map.
     * @return The request id
     */
    protected static String getRequestId(MultiValueMap<String,String> headers) {
        return headers.getFirst(REQUEST_ID);
    }

    /**
     * Verifies if the file exists in an S3 bucket or is available in HTTP.
     *
     * @param fileUrl Incoming imageMessage's fileUrl.
     * @return {@code true} if the file exists; {@code false} otherwise.
     */
    public ValidationStatus verifyUrl(final String fileUrl) {
        if (StringUtils.isEmpty(fileUrl)) {
            return new ValidationStatus(false, "No fileUrl provided.", "");
        }
        if (fileUrl.startsWith(S3Validator.S3_PREFIX)) {
            return S3Validator.checkFileExists(patchURL(fileUrl));
        } else {
            return HTTPValidator.checkFileExists(patchURL(fileUrl));
        }
    }

}
