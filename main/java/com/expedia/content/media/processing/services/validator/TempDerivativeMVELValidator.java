package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.services.derivative.TempDerivativeMessage;
import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * validation {@code TempDerivative} list based on the Temporary Derivative API
 */
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.NPathComplexity", "PMD.LogicInversion"})
public class TempDerivativeMVELValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TempDerivativeMVELValidator.class);
    private List<String> ruleList;


    public void setRuleList(List<String> ruleMaps) {
        this.ruleList = ruleMaps;
    }

    private TempDerivativeMVELValidator() {}

    /**
     * Validates a TempDerivativeMessage
     *
     * @param tempDerivativeMessage
     * @return
     */
    public String validateTempDerivativeMessage(TempDerivativeMessage tempDerivativeMessage) {
        final StringBuffer errorMsg = new StringBuffer();
        final Map messageMap = new HashMap();
        messageMap.put("tempDerivativeMessage", tempDerivativeMessage);
        return compareRulesWithMessageMap(errorMsg, ruleList, messageMap);
    }

    private String compareRulesWithMessageMap(StringBuffer errorMsg, List<String> ruleList, Map<String, Object> objectMap) {
        for (final String rule : ruleList) {
            String validationError = "";
            try {
                validationError = MVEL.eval(rule, objectMap).toString();

            } catch (Exception ex) {
                LOGGER.error("rule compare exception:", ex);
            }
            if (!validationError.contains("valid") && !"".equals(validationError)) {
                errorMsg.append(validationError).append("\r\n");
            }
        }
        return errorMsg.toString();
    }

}
