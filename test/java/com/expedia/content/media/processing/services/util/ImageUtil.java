package com.expedia.content.media.processing.services.util;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class ImageUtil {

    /**
     * Build a test file Image
     * source from http://stackoverflow.com/questions/12674064/how-to-save-a-bufferedimage-as-a-file
     * 
     * @return
     * @throws Exception
     */
    public static File buildTestImage(int widht, int height, File fileName) throws Exception {
        try {
            final BufferedImage img = new BufferedImage(widht, height, BufferedImage.TYPE_INT_RGB);
            final int red = 5;
            final int green = 25;
            final int blue = 255;
            final int color = (red << 16) | (green << 8) | blue;
            for (int x = 0; x < widht; x++) {
                for (int y = 20; y < height; y++) {
                    img.setRGB(x, y, color);
                }
            }
            ImageIO.write(img, "jpg", fileName);
        } catch (Exception e) {
        }
        return fileName;
    }
}
