package com.expedia.content.media.processing.services.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.mvel2.MVEL;

public class MVELValidator implements MapMessageValidator {

    private Map<String, List<String>> ruleMaps;

    public Map<String, List<String>> getRuleMaps() {
        return ruleMaps;
    }

    public void setRuleMaps(Map<String, List<String>> ruleMaps) {
        this.ruleMaps = ruleMaps;
    }

    public List<Map<String, String>> validate(List<Map<String, Object>> messageList) throws Exception {

        List<Map<String, String>> list = new ArrayList<>();
        List<String> ruleList = ruleMaps.get("EPC");
        for (Map messageMap : messageList) {
            StringBuffer errorMsg = new StringBuffer();
            for (String rule : ruleList) {
                String error = "";
                try {
                    error = MVEL.eval(rule, messageMap).toString();
                } catch (Exception ex) {
                    System.out.println("exception:" + ex.getMessage());
                    if (ex.getMessage().contains("unresolvable property or identifier")) {
                        errorMsg.append(rule + " is missed").append("\r\n");
                    }
                }
                if (!"valid".equals(error) && !"".equals(error)) {
                    errorMsg.append(error).append("\r\n");
                }
            }
            if (errorMsg.toString().length() > 0) {
                Map<String, String> jsonMap = new TreeMap<>();
                jsonMap.put("fileName", (String) messageMap.get("fileName"));
                jsonMap.put("error", errorMsg.toString());
                list.add(jsonMap);
            }
        }
        return list;
    }
}


