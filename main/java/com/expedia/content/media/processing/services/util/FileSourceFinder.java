package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.services.dao.DerivativesDao;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.domain.MediaDerivative;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Find file path from S3 or window share.
 */
// TODO: JavaDoc all the things
@Component
public class FileSourceFinder {
    public static final String S3_PREFIX = "s3://";
    public static final String HOTELS = "/hotels";
    @Value("${media.bucket.name}")
    private String bucketName;
    @Value("${media.bucket.prefix.name}")
    private String bucketPrefix;
    @Value("${media.bucket.prefix.derivative.name}")
    private String derivativeBucketPrefix;

    @Autowired
    private DerivativesDao mediaDBDerivativesDao;
    @Autowired
    private MediaDao mediaDBMediaDao;

    /**
     * Returns a source Media given the derivativeUrl.
     * @param derivativeUrl the derivative derivativeUrl.
     * @return A Media Object of the source media record.
     * @throws Exception
     */
    public Optional<Media> getMediaByDerivativeUrl(String derivativeUrl) throws Exception {
        final String derivativeLocation = mediaUrlToS3Path(derivativeUrl, false);
        final Optional<MediaDerivative> derivative = mediaDBDerivativesDao.getDerivativeByLocation(derivativeLocation);
        return derivative.map(der -> mediaDBMediaDao.getMediaByGuid(der.getMediaGuid())).orElse(Optional.empty());
    }

    /**
     * Returns the s3 Path from a media url.
     *
     * @param mediaUrl The MediaUrl to parse, to construct the s3 path.
     * @param findSourcePath Boolean value to determine whether to return the source or derivative path.
     * @return the S3 path to a image.
     */
    public String mediaUrlToS3Path(String mediaUrl, boolean findSourcePath) {
        final String fileName = getFileNameFromUrl(mediaUrl);
        final String millionFolder = getMillionFolderFromUrl(mediaUrl);
        if (findSourcePath) {
            return S3_PREFIX + bucketName + "/" + bucketPrefix + millionFolder + fileName;
        } else {
            return S3_PREFIX + bucketName + "/" + derivativeBucketPrefix + millionFolder + fileName;
        }
    }

    public String getMillionFolderFromUrl(String fileUrl) {
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
     * @param fileUrl The file url to parse, to find the filename.
     * @return The file name from the fileUrl.
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
