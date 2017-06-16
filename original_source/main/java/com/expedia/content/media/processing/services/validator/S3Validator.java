package com.expedia.content.media.processing.services.validator;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import org.apache.commons.lang.StringUtils;

/**
 * Verifies if a file exists in the S3 repo.
 */
public class S3Validator {
    private static final FormattedLogger LOGGER = new FormattedLogger(S3Validator.class);

    public static final String S3_PREFIX = "s3://";

    private S3Validator() {
    }

    /**
     * Verifies if a file exists in the S3 repo.
     *
     * @param fileUrl The file to verify.
     * @return true if found, false otherwise.
     */
    public static ValidationStatus checkFileExists(String fileUrl) {
        ValidationStatus validationStatus = new ValidationStatus(false, "Provided fileUrl does not exist.", ValidationStatus.NOT_FOUND);
        String bucketName = StringUtils.EMPTY;
        String objectName = StringUtils.EMPTY;
        try {
            bucketName = getBucketName(fileUrl);
            objectName = getObjectName(fileUrl);
            final AmazonS3 s3Client = new AmazonS3Client();
            final S3Object object = s3Client.getObject(new GetObjectRequest(bucketName, objectName));
            if (object != null) {
                validationStatus = checkFileIsGreaterThanZero(object);
            }
        } catch (AmazonServiceException e) {
            LOGGER.error(e, "s3 key query exception fileUrl = {} bucketName = {} objectName = {}", fileUrl, bucketName, objectName);
        }
        return validationStatus;
    }

    private static String getBucketName(String fileUrl) {
        final String bucketName = fileUrl.substring(S3_PREFIX.length());
        return bucketName.substring(0, bucketName.indexOf('/'));
    }

    private static String getObjectName(String fileUrl) {
        final String bucketName = fileUrl.substring(S3_PREFIX.length());
        return bucketName.substring(bucketName.indexOf('/') + 1);
    }

    /**
     * Verifies if the file is not empty.
     * @param object S3 object associated to the file
     * @return false iff ObjectMetadata exists and the ContentLength is 0
     */
    private static ValidationStatus checkFileIsGreaterThanZero(S3Object object) {
        final ObjectMetadata objectMetadata = object.getObjectMetadata();
        return new ValidationStatus(objectMetadata == null || (objectMetadata.getContentLength() > 0), "Provided file is 0 Bytes", ValidationStatus.ZERO_BYTES);
    }
}
