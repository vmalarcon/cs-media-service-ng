package com.expedia.content.media.processing.services.validator;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


import java.io.InputStream;

@Component
public class S3Validator {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3Validator.class);

    @Autowired
    private  ResourceLoader resourceLoader;

    public boolean checkFileExists(String fileUrl) throws Exception {
        boolean exist = true;
        final Resource sourceFile = resourceLoader.getResource(fileUrl);
        try (InputStream sourceInputStream = sourceFile.getInputStream();) {
        } catch (Exception ex) {
            LOGGER.error("s3 key query exception", ex);
            exist = false;
        }
        return exist;
    }

}
