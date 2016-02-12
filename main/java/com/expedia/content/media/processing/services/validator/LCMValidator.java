package com.expedia.content.media.processing.services.validator;


import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.MediaProviderDao;
import com.expedia.content.media.processing.services.dao.SKUGroupCatalogItemDao;
import com.expedia.content.media.processing.services.util.ValidatorUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LCMValidator implements MapMessageValidator {

    @Autowired
    private SKUGroupCatalogItemDao skuGroupCatalogItemDao;

    @Autowired
    private MediaProviderDao mediaProviderDao;

    public List<Map<String, String>> validateImages(List<ImageMessage> messageList) {
        final List<Map<String, String>> list = new ArrayList<>();
        final Map messageMap = new HashMap();
        for (final ImageMessage imageMessage : messageList) {
            final StringBuffer errorMsg = new StringBuffer();
            messageMap.put("imageMessage", imageMessage);

            if (!skuGroupCatalogItemDao.gteSKUGroup(Integer.parseInt(imageMessage.getOuterDomainData().getDomainId()))) {
                errorMsg.append("The domainId does not exist in LCM.");
            }

            if (!mediaProviderDao.getMediaProviderList(Integer.parseInt(imageMessage.getOuterDomainData().getDomainId()), imageMessage.getOuterDomainData().getProvider())) {
                errorMsg.append("The mediaProvider does not exist in LCM.");
            }

            if (errorMsg.length() > 0) {
                ValidatorUtil.putErrorMapToList(list, errorMsg, imageMessage);
            }
        }
        return list;
    }
}
