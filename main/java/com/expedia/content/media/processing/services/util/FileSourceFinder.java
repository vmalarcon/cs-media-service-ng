package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.domain.MediaDerivative;
import com.expedia.content.media.processing.services.dao.mediadb.MediaDBDerivativesDao;
import com.expedia.content.media.processing.services.dao.mediadb.MediaDBMediaDao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * Find file path from S3 or window share.
 */
@Component
public class FileSourceFinder {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String S3_PREFIX = "s3://";
    public static final String HOTELS = "/hotels";
    @Value("${media.source.query.s3only}")
    private boolean queryS3BucketOnly;
    @Value("${media.bucket.name}")
    private String bucketName;
    @Value("${media.bucket.prefix.name}")
    private String bucketPrefix;

    @Autowired
    private MediaDBDerivativesDao mediaDBDerivativesDao;
    @Autowired
    private MediaDBMediaDao mediaDBMediaDao;

    public ResponseEntity getSourceUrl(String mediaUrl) throws Exception {
        final String derivativeLocation = mediaUrlToS3Path(mediaUrl);
        final MediaDerivative derivative = mediaDBDerivativesDao.getDerivativeByLocation(derivativeLocation);
        final Media media = mediaDBMediaDao.getMediaByGuid(derivative.getMediaGuid());
        final Map<String, String> response = new HashMap<>();
        response.put("contentProviderMediaName", media.getFileName());
        response.put("mediaSourceUrl", media.getSourceUrl());
        final String jsonResponse = OBJECT_MAPPER.writeValueAsString(response);
        return new ResponseEntity<>(jsonResponse, HttpStatus.OK);
    }

    /**
     * Returns the s3 Path from a media url.
     *
     * @param mediaUrl
     * @return
     */
    private String mediaUrlToS3Path(String mediaUrl) {
        final String fileName = getFileNameFromUrl(mediaUrl);
        final String millionFolder = getMillionFolderFromUrl(mediaUrl);
        return S3_PREFIX + bucketName + "/" + bucketPrefix + "/" + millionFolder + fileName;
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
