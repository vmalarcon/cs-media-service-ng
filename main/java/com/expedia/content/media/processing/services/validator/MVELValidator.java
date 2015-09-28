package com.expedia.content.media.processing.services.validator;

import java.util.*;

import com.expedia.content.media.processing.services.util.json.Image;
import org.apache.commons.lang.StringUtils;
import org.mvel2.MVEL;

public class MVELValidator implements MapMessageValidator {

    private final static String ERROR = "unresolvable property or identifier:";
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
                    if (ex.getMessage().contains(ERROR) && rule.contains("!=null")) {
                        String field = StringUtils
                                .substring(ex.getLocalizedMessage(), ex.getMessage().indexOf("unresolvable property or identifier:") + ERROR.length() + 1,
                                        ex.getMessage().indexOf(
                                                "]"));
                        errorMsg.append(field + " is missed").append("\r\n");
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

    public List<Map<String, String>> validateImages(List<Image> messageList) throws Exception {

        List<Map<String, String>> list = new ArrayList<>();
        List<String> ruleList = ruleMaps.get("EPC");
        Map messageMap = new HashMap();
        for (Image imageMesage : messageList) {
            messageMap.put("imageMesage", imageMesage);
            StringBuffer errorMsg = new StringBuffer();
            for (String rule : ruleList) {
                String error = "";
                try {
                    error = MVEL.eval(rule, messageMap).toString();
                } catch (Exception ex) {
                    System.out.println("exception:" + ex.getMessage());
                }
                if (!"valid".equals(error) && !"".equals(error)) {
                    errorMsg.append(error).append("\r\n");
                }
            }
            if (errorMsg.length() > 0) {
                Map<String, String> jsonMap = new TreeMap<>();
                jsonMap.put("fileName", imageMesage.getFileName());
                jsonMap.put("error", errorMsg.toString());
                list.add(jsonMap);
            }
        }
        return list;
    }
}


