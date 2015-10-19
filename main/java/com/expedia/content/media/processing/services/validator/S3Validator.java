package com.expedia.content.media.processing.services.validator;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class S3Validator {

    private static final String S3_CMD = "aws s3 ls ";

    public static boolean checkFileExists(String fileUrs) {
        String s = executeCommand(S3_CMD + fileUrs);
        if (s != null && s.contains("testImage2.jpg")) {
            return true;
        }
        return false;
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
