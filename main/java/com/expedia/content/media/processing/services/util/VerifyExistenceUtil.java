package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.services.validator.HTTPValidator;
import com.expedia.content.media.processing.services.validator.S3Validator;

/**
 * Interface to verify if a file exists.
 */
public interface VerifyExistenceUtil {
    /**
     * Verifies if the file exists in an S3 bucket or is available in HTTP.
     *
     * @param fileUrl Incoming imageMessage's fileUrl.
     * @return {@code true} if the file exists; {@code false} otherwise.
     */
    default boolean verifyURLExistence(final String fileUrl) {
        if (fileUrl.startsWith(S3Validator.S3_PREFIX)) {
            return S3Validator.checkFileExists(fileUrl);
        } else {
            return HTTPValidator.checkFileExists(fileUrl);
        }
    }
}
