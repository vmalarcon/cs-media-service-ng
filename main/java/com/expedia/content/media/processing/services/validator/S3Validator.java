package com.expedia.content.media.processing.services.validator;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3Validator {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3Validator.class);

    private static final String S3_CMD = "aws s3 ls ";

    public static boolean checkFileExists(String fileUrs) {
        if (fileUrs != null && fileUrs.contains("s3")) {
            String s = executeCommand(S3_CMD + fileUrs);
            LOGGER.info("checkFileExists s3 output:"+s);
            if (s != null && s.contains(FilenameUtils.getBaseName(fileUrs))) {
                return true;
            }
            return false;
        }
        return true;

    }

    private static String executeCommand(String command) {
        String result = "";
        Process process;
        try {
            process = Runtime.getRuntime().exec(command);
            process.waitFor();
            result = IOUtils.toString(process.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
