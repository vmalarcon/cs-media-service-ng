package com.expedia.content.media.processing.services.validator;


import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.MediaDomainCategoriesDao;
import com.expedia.content.media.processing.services.dao.MediaProviderDao;
import com.expedia.content.media.processing.services.dao.RoomTypeDao;
import com.expedia.content.media.processing.services.dao.SKUGroupCatalogItemDao;
import com.expedia.content.media.processing.services.util.ValidatorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("PMD")
public class LCMValidator implements MapMessageValidator {

    @Autowired
    private SKUGroupCatalogItemDao skuGroupCatalogItemDao;

    @Autowired
    private MediaProviderDao mediaProviderDao;

    @Autowired
    private MediaDomainCategoriesDao mediaDomainCategoriesDao;

    @Autowired
    private RoomTypeDao roomTypeDao;

    private final static String DEFAULT_LANG_ID = "1033";
    private final static String CATEGORY = "category";
    private final static String ROOMS = "rooms";


    public List<Map<String, String>> validateImages(List<ImageMessage> messageList) {
        final List<Map<String, String>> list = new ArrayList<>();
        final Map messageMap = new HashMap();
        for (final ImageMessage imageMessage : messageList) {
            final StringBuffer errorMsg = new StringBuffer();
            messageMap.put("imageMessage", imageMessage);

            if (!skuGroupCatalogItemDao.gteSKUGroup(Integer.parseInt(imageMessage.getOuterDomainData().getDomainId()))) {
                errorMsg.append("The domainId does not exist in LCM.");
            }

            if (!mediaProviderDao.getMediaProviderList(imageMessage.getOuterDomainData().getProvider())) {
                errorMsg.append("The mediaProvider does not exist in LCM.");
            }

            final String category = imageMessage.getOuterDomainData().getDomainFields() == null ||
                    imageMessage.getOuterDomainData().getDomainFields().get(CATEGORY) == null ? "" :
                    imageMessage.getOuterDomainData().getDomainFields().get(CATEGORY).toString();
            if (!("").equals(category) && !mediaDomainCategoriesDao.getCategoryId(imageMessage.getOuterDomainData().getDomain().getDomain(), DEFAULT_LANG_ID, category)) {
                errorMsg.append("The category does not exist in LCM.");
            }

            final List<Integer> roomIds = imageMessage.getOuterDomainData().getDomainFields() == null ||
                    imageMessage.getOuterDomainData().getDomainFields().get(ROOMS) == null ? Collections.EMPTY_LIST :
                    ValidatorUtil.getRoomIds(imageMessage.getOuterDomainData().getDomainFields().get(ROOMS));
            if (!CollectionUtils.isEmpty(roomIds) && !roomTypeDao.getRoomTypeCatalogItemId(Integer.parseInt(imageMessage.getOuterDomainData().getDomainId()), roomIds)) {
                errorMsg.append("The room does not belong to the property in LCM.");
            }

            if (errorMsg.length() > 0) {
                ValidatorUtil.putErrorMapToList(list, errorMsg, imageMessage);
            }
        }
        return list;
    }
}
