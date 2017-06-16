package com.expedia.content.media.processing.services.util;

import java.awt.image.BufferedImage;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import com.expedia.content.media.processing.pipeline.util.FormattedLogger;

import com.expedia.content.media.processing.pipeline.domain.Metadata;

/**
 * Utility class for thumbnail manipulation.
 */

public final class ImageUtil {

    private static final FormattedLogger LOGGER = new FormattedLogger(ImageUtil.class);
    

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
                LOGGER.debug("Media Metadata File={} Width={}", sourcePath.getFileName(), width);
                sourceSize = sourcePath.toFile().length();
                LOGGER.debug("Media Metadata File={} Size={}", sourcePath.getFileName(), sourceSize);
                imageMetadata = Metadata.builder().fileSize((int) sourcePath.toFile().length()).width(width).height(height).build();

            } catch (Exception e) {
                LOGGER.debug(e, "The source path is not associate to an image File={}", sourcePath.getFileName());
            }
        }
        return imageMetadata;
    }



    /**
     * get the percentage configuration value
     * and compare the value with java random value to decide route to Kafka Component or not.
     *
     * @return boolean indicate whether
     */
    public static boolean routeKafkaLcmConsByPercentage(int routePercentage) {
        final int ranNum = (int) (Math.random() * 100);
        if (ranNum < routePercentage) {
            return true;
        }
        return false;
    }
}
