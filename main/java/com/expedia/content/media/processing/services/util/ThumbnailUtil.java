package com.expedia.content.media.processing.services.util;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
@SuppressWarnings({"PMD.UseUtilityClass"})
public final class ThumbnailUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThumbnailUtil.class);
    private static final String DERIVATIVE_TYPE = "t";

    /**
     * Retrieve basic metadata for a file.
     * 
     * @param sourcePath source path.
     * @return
     */
    public static Metadata getBasicMetadata(Path sourcePath) throws Exception {
        BufferedImage bufferedImage;
        int height = 0;
        int width = 0;
        Long sourceSize = 0L;
        if (sourcePath != null) {
            bufferedImage = ImageIO.read(sourcePath.toFile());
            height = bufferedImage.getHeight();
            width = bufferedImage.getWidth();
            LOGGER.debug("Media width: " + width);
            sourceSize = sourcePath.toFile().length();
            LOGGER.debug("Media size: " + sourceSize);
            return Metadata.builder().fileSize((int) sourcePath.toFile().length()).width(width).height(height).build();
        }
        return null;
    }

    /**
     * Retrieve the source path base on the fileUrl.
     * 
     * @param fileUrl Image URL to fetch.
     * @param guid GUID for the image.
     * @param workFolder Temporary working folder to use for downloading the image.
     * @param resourceLoader
     * @return Path where the image is downloaded.
     * @throws IOException When unable to fetch the URL
     */
    public static Path retrieveSourcePath(final String fileUrl, final String guid, TemporaryWorkFolder workFolder,
                                          ResourceLoader resourceLoader) throws IOException {
        final Resource resource = resourceLoader.getResource(fileUrl);
        LOGGER.debug("Fetching URL -> " + fileUrl);
        final Path filePath = Paths.get(workFolder.getWorkPath().toString(), guid + ".jpg");
        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath.toString())) {
            IOUtils.copy(resource.getInputStream(), fileOutputStream);
        }
        LOGGER.debug("Fetched  URL -> " + fileUrl);
        return filePath;
    }

    /**
     * Build a thumbnail from the given path
     * 
     * @param thumbnailPath path for the thumbnail.
     * @param url thumbnail location url;
     * @param sourcePath path for the source image.
     * @return
     */
    public static Thumbnail buildThumbnail(Path thumbnailPath, String url, Path sourcePath) throws Exception {

        final Metadata sourceMetadata = getBasicMetadata(sourcePath);
        final Metadata thumbnailMetadata = getBasicMetadata(thumbnailPath);
        return Thumbnail.builder().thumbnailMetadata(thumbnailMetadata).sourceMetadata(sourceMetadata).location(url).type(DERIVATIVE_TYPE).build();
    }
}
