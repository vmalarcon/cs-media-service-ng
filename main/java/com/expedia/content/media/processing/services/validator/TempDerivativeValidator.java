package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.services.derivative.TempDerivativeMessage;

import java.util.ArrayList;
import java.util.List;


/**
 * validation {@code TempDerivative} list based on the Temporary Derivative API
 */
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.NPathComplexity", "PMD.LogicInversion"})
public class TempDerivativeValidator {

    private TempDerivativeValidator() {}

    /**
     * Validates a TempDerivative Message
     *
     * @param tempDerivative
     * @return
     */
    public static List<String> validateMessage(TempDerivativeMessage tempDerivative) {
        final List<String> list = new ArrayList<>();

        if(tempDerivative.getFileUrl() == null) {
            list.add("fileUrl is required.");
        }
        if((tempDerivative.getFileUrl()) != null && !tempDerivative.getFileUrl().matches("(.*)http://(.*)|(.*)https://(.*)|(.*)s3://(.*)|(.*)file:///(.*)"))  {
            list.add("fileUrl is malformed.");
        }
        if((tempDerivative.getFileUrl()) != null
                &&(!tempDerivative.getFileUrl().toLowerCase().endsWith(".jpg")
                    &&!tempDerivative.getFileUrl().toLowerCase().endsWith(".jpeg")
                    &&!tempDerivative.getFileUrl().toLowerCase().endsWith(".png")
                    &&!tempDerivative.getFileUrl().toLowerCase().endsWith(".bmp")
                    &&!tempDerivative.getFileUrl().toLowerCase().endsWith(".gif"))) {
            list.add("fileUrl extension is malformed.");
        }
        if((tempDerivative.getRotation() != null && (!(Integer.valueOf(tempDerivative.getRotation())%90 == 0) && !(Integer.valueOf(tempDerivative.getRotation()) > 270)))) {
            list.add("getRotation() value is not an accepted value.");
        }
        if(Integer.valueOf(tempDerivative.getWidth()) == null) {
            list.add("width is required.");
        }
        if(Integer.valueOf(tempDerivative.getHeight()) == null) {
            list.add("height is required.");
        }

        return list;
    }

}
