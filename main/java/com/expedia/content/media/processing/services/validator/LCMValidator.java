package com.expedia.content.media.processing.services.validator;


import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.CatalogItemMediaDao;
import com.expedia.content.media.processing.services.util.ValidatorUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LCMValidator implements MapMessageValidator {

    @Autowired
    private CatalogItemMediaDao catalogItemMediaDao;

    @Override
    public List<Map<String, String>> validateImages(List<ImageMessage> messageMapList) {
        final List<Map<String, String>> list = new ArrayList<>();
        final Map messageMap = new HashMap();
        for (final ImageMessage imageMessage : messageMapList) {
            final StringBuffer errorMsg = new StringBuffer();
            messageMap.put("imageMessage", imageMessage);
            //compare ImageMessage (non outer domain) fields with rules
            compareRulesWithMessageMap(errorMsg, ruleList, messageMap);
            if (errorMsg.length() > 0) {
                ValidatorUtil.putErrorMapToList(list, errorMsg, imageMessage);
            }
        }
        return list;
    }
}
