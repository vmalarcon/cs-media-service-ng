package com.expedia.content.media.processing.services.validator;

import java.util.*;

import com.expedia.content.media.processing.services.util.json.DomainData;
import com.expedia.content.media.processing.services.util.json.Image;
import org.mvel2.MVEL;

public class MVELValidator implements MapMessageValidator {

    private Map<String, List<String>> ruleMaps;

    public Map<String, List<String>> getRuleMaps() {
        return ruleMaps;
    }

    public void setRuleMaps(Map<String, List<String>> ruleMaps) {
        this.ruleMaps = ruleMaps;
    }

    public List<Map<String, String>> validateImages(List<Image> messageList) throws Exception {

        List<Map<String, String>> list = new ArrayList<>();
        List<String> ruleList = ruleMaps.get("EPC");
        Map messageMap = new HashMap();
        Map subMap = new HashMap();
        for (Image imageMesage : messageList) {
            messageMap.put("imageMesage", imageMesage);
            List<DomainData> domainDataList = imageMesage.getDomainData();
            StringBuffer errorMsg = new StringBuffer();
            for (String rule : ruleList) {
                int num = 0;
                for (DomainData domainData : domainDataList) {
                    subMap.put("domainData", domainData);
                    String error = "";
                    try {
                        if (rule.contains("domainData")) {
                            error = MVEL.eval(rule, subMap).toString();
                            if (error.length() > 0) {
                                error = "domainData[" + num + "].domainDataFields." + error;
                            }
                        } else {
                            error = MVEL.eval(rule, messageMap).toString();
                        }
                    } catch (Exception ex) {
                        System.out.println("exception:" + ex.getMessage());
                    }
                    if (!error.contains("valid") && !"".equals(error)) {
                        errorMsg.append(error).append("\r\n");
                    }
                    num++;
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


