package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.services.util.ValidatorUtil;
import org.mvel2.MVEL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * validation {@code ImageMessage} list based on MVEL rule that defined in xml configuration
 */
public class MVELValidator implements MapMessageValidator {
    private static final FormattedLogger LOGGER = new FormattedLogger(MVELValidator.class);
    private String clientRule = "";
    private static final String RULE_PREFIX = "domainData";

    public String getClientRule() {
        return clientRule;
    }

    public void setClientRule(String clientRule) {
        this.clientRule = clientRule;
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
    @SuppressWarnings({"rawtypes", "unchecked"})
    public List<String> validateImages(List<ImageMessage> messageList) {
        final List<String> list = new ArrayList<>();
        final List<String> ruleList = ruleMaps.get(clientRule);
        final Map messageMap = new HashMap();
        final Map domainMap = new HashMap();
        for (final ImageMessage imageMessage : messageList) {
            final StringBuffer errorMsg = new StringBuffer();
            messageMap.put("imageMessage", imageMessage);
            //compare ImageMessage (non outer domain) fields with rules
            compareRulesWithMessageMap(errorMsg, ruleList, messageMap);
            if (imageMessage.getOuterDomainData() != null) {
                domainMap.put(RULE_PREFIX, imageMessage.getOuterDomainData());
                //merge two map because rule
                //imageMessage.imageType.imageType.equals('Lodging')&amp;&amp;!domainData.domainDataName.equals('LCM') ?"domainDataName must be LCM.":"valid"
                messageMap.putAll(domainMap);
                compareRulesWithDomainMap(errorMsg, ruleList, messageMap);
            }
            if (errorMsg.length() > 0) {
                ValidatorUtil.putErrorMapToList(list, errorMsg);
            }
        }
        return list;
    }

    private void compareRulesWithMessageMap(StringBuffer errorMsg, List<String> ruleList, Map<String, Object> objectMap) {
        for (final String rule : ruleList) {
            String validationError = "";
            try {
                if (!rule.contains(RULE_PREFIX)) {
                    validationError = MVEL.eval(rule, objectMap).toString();
                }
            } catch (Exception ex) {
                LOGGER.error(ex, "Rule compare exception", (ImageMessage) objectMap.get("imageMessage"));
            }
            if (!validationError.contains("valid") && !"".equals(validationError)) {
                errorMsg.append(validationError).append("\r\n");
            }
        }
    }

    private void compareRulesWithDomainMap(StringBuffer errorMsg, List<String> ruleList, Map<String, Object> objectMap) {
        for (final String rule : ruleList) {
            String validationError = "";
            try {
                if (rule.contains(RULE_PREFIX)) {
                    validationError = "domainDataFields." + MVEL.eval(rule, objectMap).toString();

                }
            } catch (Exception ex) {
                LOGGER.warn(ex, "Rule compare exception", (ImageMessage) objectMap.get("imageMessage"));
                //TODO: not very good solution here, later we need to define a validation Object for domain field Map
                //now domainFields like domainData.domainDataFields.categoryId is not required any more
                // String exceptionMsg = ex.getMessage();
                //validationError = composeValidtionError(exceptionMsg, index);
            }
            if (!validationError.contains("valid") && !"".equals(validationError) && !errorMsg.toString().contains(validationError)) {
                errorMsg.append(validationError).append("\r\n");
            }
        }
    }

}


