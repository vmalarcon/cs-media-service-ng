package com.expedia.content.media.processing.services.util;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Find file path from S3 or window share.
 */
@Component
public class FileSourceFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSourceFinder.class);

    public static final String S3_PREFIX = "s3://";
    public static final String HOTELS = "/hotels";
    public static final int MILLION_FOLDER_LIMIT = 6000000;
    public static final String SOURCE_DIR = "\\\\CHE-FILIDXIMG01\\GSO_media\\lodging";
    public static final String SOURCE_DIR_NEW = "\\\\CHE-FILIDXIMG01\\GSO_MediaNew\\lodging";



    /**
     * get the file source URL from S3 repo.
     *
     * @return true if found, false otherwise.
     */
    public  String getGUIDFilePath(String bucketName, String prefix, String millonFolder, String partialName) {
        try {
            final AmazonS3 s3Client = new AmazonS3Client();
            final ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                    .withBucketName(bucketName)
                    .withPrefix(prefix + millonFolder);
            final ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);
            for (final S3ObjectSummary summary : objectListing.getObjectSummaries()) {
                if (summary.getKey().contains(partialName.substring(0, 8))) {
                    return S3_PREFIX + summary.getBucketName() + "/" + summary.getKey();
                }
            }
        } catch (AmazonServiceException e) {
            LOGGER.error("s3 query exception", e);
        }
        return "";
    }

    /**
     * get source URL from S3 bucket or window file share
     *
     * @param bucketName
     * @param prefix
     * @param fileUrl
     * @param domainId
     * @return
     */
    public  String getSourcePath(String bucketName, String prefix, String fileUrl, int domainId) {
        final String fileName = getFileNameFromUrl(fileUrl);
        final String pattern = "_[\\w]{1}.jpg";
        final String millonFolder = getMillonFolderFromUrl(fileUrl);
        if (matchGuid(fileName)) {
            return getGUIDFilePath(bucketName, prefix, millonFolder, fileName);
        } else {
            if (domainId < MILLION_FOLDER_LIMIT) {
                return SOURCE_DIR + millonFolder.replace("/", "\\") + fileName.replaceFirst(pattern, ".jpg");
            } else {
                return SOURCE_DIR_NEW + millonFolder.replace("/", "\\") + fileName.replaceFirst(pattern, ".jpg");
            }
        }
    }

    /**
     * match the derivative file name
     *
     * @param fileName
     * @return
     */
    private  boolean matchGuid(String fileName) {
        final String subName = getFileNameFromUrl(fileName);
        if (subName.length() > 8 && subName.substring(0, 8).contains("_")) {
            return false;
        }
        final String pattern = "[\\w]{8}_[\\w]{1}.jpg";
        if (subName.matches(pattern)) {
            return true;
        }
        return false;
    }

    private  String getMillonFolderFromUrl(String fileUrl) {
        //http://images.trvl-media.com/hotels/1000000/10000/8400/8393/4a8a5b92_t.jpg
        if (fileUrl.contains(HOTELS) && fileUrl.contains("/")) {
            final int lastLoc = fileUrl.lastIndexOf('/');
            final int firstLoc = fileUrl.indexOf(HOTELS) + HOTELS.length();
            if (lastLoc > firstLoc) {
                return fileUrl.substring(firstLoc, lastLoc + 1);
            }
        }
        return fileUrl;
    }

    /**
     * get the derivative file name from http URL.
     *
     * @param fileUrl
     * @return
     */
    public  String getFileNameFromUrl(String fileUrl) {
        //http://images.trvl-media.com/hotels/1000000/10000/8400/8393/4a8a5b92_t.jpg
        if (fileUrl.contains(HOTELS)) {
            final int lastLoc = fileUrl.lastIndexOf('/');
            return fileUrl.substring(lastLoc + 1);
        }
        return fileUrl;
    }

}
