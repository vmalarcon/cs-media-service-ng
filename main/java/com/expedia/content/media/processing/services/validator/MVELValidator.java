package com.expedia.content.media.processing.services.validator;

import java.util.List;
import java.util.Map;
import org.mvel2.MVEL;

public class MVELValidator {

    private Map<String, List<String>> ruleMaps;

    public Map<String, List<String>> getRuleMaps() {
        return ruleMaps;
    }

    public void setRuleMaps(Map<String, List<String>> ruleMaps) {
        this.ruleMaps = ruleMaps;
    }

    public void validate(Map messageMap) {
        List<String> ruleList = ruleMaps.get("EPC");
        StringBuffer errorMsg = new StringBuffer();
        for (String rule : ruleList) {
            String error = MVEL.eval(rule, messageMap).toString();
            if (!"".equals(error)) {
                errorMsg.append(error);
            }
        }
    }
}


