package com.expedia.content.media.processing.services.validator;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3Validator {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3Validator.class);

    private static final String S3_PREFIX = "s3://";

    public static boolean checkFileExists(String fileUrl) {
        boolean exist = false;
        try {
            String bucketName = getBucketName(fileUrl);
            String objectName = getObjectName(fileUrl);
            AmazonS3 s3Client = new AmazonS3Client(new ProfileCredentialsProvider());
            S3Object object = s3Client.getObject(
                    new GetObjectRequest(bucketName, objectName));
            if (object != null) {
                exist = true;
            }
        } catch (AmazonServiceException e) {
            LOGGER.error("s3 key query exception", e);
        }
        return exist;
    }

    private static String getBucketName(String fileUrl) {
        String bucketName = fileUrl.substring(S3_PREFIX.length());
        return bucketName.substring(0, bucketName.indexOf("/"));
    }

    private static String getObjectName(String fileUrl) {
        String bucketName = fileUrl.substring(S3_PREFIX.length());
        return bucketName.substring(bucketName.indexOf("/") + 1);
    }

}
