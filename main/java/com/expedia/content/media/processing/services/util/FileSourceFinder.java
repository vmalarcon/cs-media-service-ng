package com.expedia.content.media.processing.services.util;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.S3Object;
import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.pipeline.util.ImageCopy;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;

/**
 * Find file path from S3 or window share.
 */
@Component
public class FileSourceFinder {
    private static final FormattedLogger LOGGER = new FormattedLogger(FileSourceFinder.class);

    public static final String S3_PREFIX = "s3://";
    public static final String HOTELS = "/hotels";
    public static final int MILLION_FOLDER_LIMIT = 6000000;
    public static final String SOURCE_DIR = "\\\\CHE-FILIDXIMG01\\GSO_media\\lodging";
    public static final String SOURCE_DIR_NEW = "\\\\CHE-FILIDXIMG01\\GSO_MediaNew\\lodging";

    @Value("${media.source.query.s3only}")
    private boolean queryS3BucketOnly;

    @Autowired
    private ImageCopy imageCopy;

    /**
     * get the file source URL from S3 repo.
     *
     * @return true if found, false otherwise.
     */
    public String getGUIDFilePath(String bucketName, String prefix, String millonFolder, String guid, String extension) {
        S3Object object;
        final String objectName = prefix + millonFolder + guid + extension;
        try {
            object = imageCopy.getImage(bucketName, objectName);
            if (object != null) {
                //bug fix CSPB-533800, if close the object, there is no connection timeout error.
                object.close();
                return S3_PREFIX + bucketName + "/" + objectName;
            }
        } catch (AmazonServiceException | IOException e) {
            LOGGER.warn(e, "s3 query exception MediaGuid={} BucketName={} ObjectName={}", guid, bucketName, objectName);
        }
        return "";
    }

    /**
     * get source URL from S3 bucket or window file share
     *
     * @param bucketName
     * @param prefix
     * @param fileUrl
     * @param guid
     * @param lcmMedia
     * @return
     */
    public String getSourcePath(String bucketName, String prefix, String fileUrl, String guid, LcmMedia lcmMedia) {
        final String fileName = getFileNameFromUrl(fileUrl);
        final String millionFolder = getMillionFolderFromUrl(fileUrl);
        if (matchGuid(fileName)) {
            return getGUIDFilePath(bucketName, prefix, millionFolder, guid, "." + FilenameUtils.getExtension(lcmMedia.getFileName()));
        }
        final String pattern = "_[\\w]{1}.jpg";
        if (queryS3BucketOnly) {
            final String sourceName = fileName.replaceFirst(pattern, ".jpg");
            return getGUIDFilePath(bucketName, prefix, millionFolder, FilenameUtils.getBaseName(sourceName), "." + FilenameUtils.getExtension(lcmMedia.getFileName()));
        }
        else {
            if (lcmMedia.getDomainId() < MILLION_FOLDER_LIMIT) {
                return SOURCE_DIR + millionFolder.replace("/", "\\") + fileName.replaceFirst(pattern, ".jpg");
            } else {
                return SOURCE_DIR_NEW + millionFolder.replace("/", "\\") + fileName.replaceFirst(pattern, ".jpg");
            }
        }
    }

    /**
     * match the derivative file name
     *
     * @param fileName
     * @return
     */
    public boolean matchGuid(String fileName) {
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

    private String getMillionFolderFromUrl(String fileUrl) {
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
    public String getFileNameFromUrl(String fileUrl) {
        //http://images.trvl-media.com/hotels/1000000/10000/8400/8393/4a8a5b92_t.jpg
        if (fileUrl.contains(HOTELS)) {
            final int lastLoc = fileUrl.lastIndexOf('/');
            return fileUrl.substring(lastLoc + 1);
        }
        return fileUrl;
    }

}
