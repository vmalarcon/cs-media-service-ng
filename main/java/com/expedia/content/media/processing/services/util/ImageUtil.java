package com.expedia.content.media.processing.services.util;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.expedia.content.media.processing.pipeline.domain.Metadata;

/**
 * Utility class for thumbnail manipulation.
 */

public final class ImageUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageUtil.class);
    

    private ImageUtil(){
        
    }
    /**
     * Retrieve basic metadata for a file.
     * 
     * @param sourcePath source path.
     * @return
     */
    public static Metadata getBasicImageMetadata(Path sourcePath) throws Exception {
        BufferedImage bufferedImage;
        Metadata imageMetadata = null;
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
                imageMetadata = Metadata.builder().fileSize((int) sourcePath.toFile().length()).width(width).height(height).build();

            } catch (Exception e) {
                LOGGER.debug("The source path is not associate to an image file " + sourcePath.getFileName());
            }
        }
        return imageMetadata;
    }
}
