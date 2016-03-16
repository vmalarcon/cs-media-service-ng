package com.expedia.content.media.processing.services.util;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.amazonaws.util.IOUtils;
import com.expedia.content.media.processing.pipeline.domain.Metadata;
import com.expedia.content.media.processing.pipeline.util.TemporaryWorkFolder;
import com.expedia.content.media.processing.services.dao.domain.Thumbnail;

/**
 * Utility class for thumbnail manipulation.
 */
public   class ThumbnailUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThumbnailUtil.class);
    private static final String S3_PREFIX = "s3:/";
    private static final String DERIVATIVE_TYPE = "t";

    private ThumbnailUtil(){
        
    }
    /**
     * Retrieve basic metadata for a file.
     * 
     * @param sourcePath source path.
     * @return
     */
    public static Metadata getBasicMetadata(Path sourcePath) {
        BufferedImage bufferedImage;
        int height = 0;
        int width = 0;
        Long sourceSize = 0L;
        if (sourcePath != null) {
            try {
                bufferedImage = ImageIO.read(sourcePath.toFile());
                height = bufferedImage.getHeight();
                width = bufferedImage.getWidth();
                LOGGER.debug("Media width: " + width);
                sourceSize = sourcePath.toFile().length();
                LOGGER.debug("Media size: " + sourceSize);
                return Metadata.builder().fileSize((int) sourcePath.toFile().length()).width(width).height(height).build();
            } catch (Exception e) {
                LOGGER.debug("Unable to extract the metadas for the given url file: " + sourcePath.getFileName(),e);
            }
        }
        return null;
    }

    /**
     * Retrieve the source path base on the fileUrl.
     * 
     * @param fileUrl
     * @param guid
     * @param workFolder
     * @param resourceLoader
     * @return
     * @throws IOException
     */
    public static Path retrieveSourcePath(final String fileUrl, final String guid, TemporaryWorkFolder workFolder,
                                          ResourceLoader resourceLoader) throws IOException {
        if (fileUrl.toLowerCase(Locale.US).startsWith(S3_PREFIX)) {
            return fetchS3(fileUrl, guid, workFolder.getWorkPath(), resourceLoader);
        } else {
            return fetchUrl(fileUrl, guid, workFolder.getWorkPath());
        }
    }

    /**
     * Build a thumbnail from the given path
     * 
     * @param thumbnailPath path for the thumbnail.
     * @param url thumbnail location url;
     * @param sourcePath path for the source image.
     * @return
     */
    public static Thumbnail buildThumbnail(Path thumbnailPath, String url, Path sourcePath) {

        final Metadata sourceMetadata = getBasicMetadata(sourcePath);
        final Metadata thumbnailMetadata = getBasicMetadata(thumbnailPath);
        return Thumbnail.builder().thumbnailMetadata(thumbnailMetadata).sourceMetadata(sourceMetadata).location(url).type(DERIVATIVE_TYPE).build();
    }

    /**
     * Fetch HTTP URL. Starts with {@code http://} or {@code https://}.
     *
     * @param url Image URL to fetch.
     * @param guid GUID for the image.
     * @param workPath Temporary working folder to use for downloading the image.
     * @return Path where the image is downloaded.
     * @throws IOException
     *             When unable to fetch the HTTP URL.
     */
    private static Path fetchUrl(final String url, final String guid, final Path workPath) throws IOException {
        LOGGER.debug("Fetching HTTP URL -> " + url);
        final URL validUrl = new URL(url);
        final Path filePath = Paths.get(workPath.toString(), guid + ".jpg");
        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath.toString())) {
            IOUtils.copy(validUrl.openStream(), fileOutputStream);
        }
        LOGGER.debug("Fetched HTTP URL -> " + url);
        return filePath;
    }

    /**
     * Fetch S3 URL. Starts with {@code s3://}.
     *
     * @param url Image URL to fetch from S3.
     * @param guid GUID for the image.
     * @param workPath Temporary working folder to use for downloading the image.
     * @return Path where the image is downloaded.
     * @throws IOException When unable to fetch the S3 URL.
     */
    private static Path fetchS3(final String url, final String guid, final Path workPath, ResourceLoader resourceLoader) throws IOException {
        LOGGER.debug("Fetching S3 URL -> " + url);
        final Resource resource = resourceLoader.getResource(url);
        final Path filePath = Paths.get(workPath.toString(), guid + ".jpg");
        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath.toString())) {
            IOUtils.copy(resource.getInputStream(), fileOutputStream);
        }
        LOGGER.debug("Fetched S3 URL -> " + url);
        return filePath;
    }
}
