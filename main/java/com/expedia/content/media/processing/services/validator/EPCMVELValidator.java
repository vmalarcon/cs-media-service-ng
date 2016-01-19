package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * validation {@code ImageMessage} list based on MVEL rule that defined in xml configuration
 */
public class EPCMVELValidator implements MapMessageValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(EPCMVELValidator.class);
    private String ruleName = "";
    private static final String RULE_PREFIX = "domainData";
    private static final String ERROR_NOT_FOUND = "could not access:";

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    private Map<String, List<String>> ruleMaps;

    public Map<String, List<String>> getRuleMaps() {
        return ruleMaps;
    }

    public void setRuleMaps(Map<String, List<String>> ruleMaps) {
        this.ruleMaps = ruleMaps;
    }

    /**
     * validate imageMessage by MVEL rule evaluation, validation error message is defined in xml configuration.
     *
     * @param messageList ImageMessage list
     * @return JSON string contains fileName and error description
     */
    public List<Map<String, String>> validateImages(List<ImageMessage> messageList) {
        List<Map<String, String>> list = new ArrayList<>();
        List<String> ruleList = ruleMaps.get(ruleName);
        Map messageMap = new HashMap();
        Map domainMap = new HashMap();
        for (ImageMessage imageMessage : messageList) {
            StringBuffer errorMsg = new StringBuffer();
            messageMap.put("imageMessage", imageMessage);
            //compare ImageMessage (non outer domain) fields with rules
            compareRulesWithMessageMap(errorMsg, ruleList, messageMap);
            int index = 0;
            if (imageMessage.getOuterDomainData() != null) {
                domainMap.put(RULE_PREFIX, imageMessage.getOuterDomainData());
                //merge two map because rule
                //imageMessage.imageType.imageType.equals('Lodging')&amp;&amp;!domainData.domainDataName.equals('LCM') ?"domainDataName must be LCM.":"valid"
                messageMap.putAll(domainMap);
                compareRulesWithDomainMap(errorMsg, ruleList, messageMap, index);
                index++;

            }
            if (errorMsg.length() > 0) {
                putErrorMapToList(list, errorMsg, imageMessage);
            }
        }
        return list;
    }

    private void putErrorMapToList(List<Map<String, String>> list, StringBuffer errorMsg, ImageMessage imageMesage) {
        Map<String, String> jsonMap = new TreeMap<>();
        jsonMap.put("fileName", imageMesage.getFileName());
        jsonMap.put("error", errorMsg.toString());
        list.add(jsonMap);
    }

    private void compareRulesWithMessageMap(StringBuffer errorMsg, List<String> ruleList, Map<String, Object> objectMap) {
        for (String rule : ruleList) {
            String validationError = "";
            try {
                if (!rule.contains(RULE_PREFIX)) {
                    validationError = MVEL.eval(rule, objectMap).toString();
                }
            } catch (Exception ex) {
                LOGGER.error("rule compare exception:", ex);
            }
            if (!validationError.contains("valid") && !"".equals(validationError)) {
                errorMsg.append(validationError).append("\r\n");
            }
        }
    }

    private void compareRulesWithDomainMap(StringBuffer errorMsg, List<String> ruleList, Map<String, Object> objectMap, int index) {
        for (String rule : ruleList) {
            String validationError = "";
            try {
                if (rule.contains(RULE_PREFIX)) {
                    validationError = MVEL.eval(rule, objectMap).toString();
                    //validationError = "domainData[" + index + "].domainDataFields." + validationError;
                    validationError = "domainDataFields." + validationError;

                }
            } catch (Exception ex) {
                LOGGER.warn("rule compare exception:", ex);
                //not very good solution here, later we need to define a validation Object for domain field Map
                //now domainFields like domainData.domainDataFields.categoryId is not required any more
               // String exceptionMsg = ex.getMessage();
                //validationError = composeValidtionError(exceptionMsg, index);
            }
            if (!validationError.contains("valid") && !"".equals(validationError) && !errorMsg.toString().contains(validationError)) {
                errorMsg.append(validationError).append("\r\n");
            }
        }
    }

    private String composeValidtionError(String exceptionMsg, int index) {
        String validationError = "";
        if (exceptionMsg.contains(ERROR_NOT_FOUND)) {
            int parseLocation = exceptionMsg.indexOf(ERROR_NOT_FOUND);
            if (parseLocation > 0) {
                validationError =
                        exceptionMsg.substring(parseLocation + ERROR_NOT_FOUND.length() + 1, exceptionMsg.indexOf(";")) + " is required.";
                validationError = "domainData[" + index + "].domainDataFields." + validationError;
            }
        }
        return validationError;
    }
}


