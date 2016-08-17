package com.expedia.content.media.processing.services.validator;


import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.MediaDomainCategoriesDao;
import com.expedia.content.media.processing.services.dao.RoomTypeDao;
import com.expedia.content.media.processing.services.dao.SKUGroupCatalogItemDao;
import com.expedia.content.media.processing.services.util.DomainDataUtil;
import com.expedia.content.media.processing.services.util.ValidatorUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class LCMValidator implements MapMessageValidator {

    @Autowired
    private SKUGroupCatalogItemDao skuGroupCatalogItemDao;

    @Autowired
    private MediaDomainCategoriesDao mediaDomainCategoriesDao;

    @Autowired
    private RoomTypeDao roomTypeDao;

    @Resource(name = "providerProperties")
    private Properties providerProperties;

    private final static String DEFAULT_LANG_ID = "1033";

    public List<String> validateImages(List<ImageMessage> messageList) {
        final List<String> list = new ArrayList<>();
        final Map messageMap = new HashMap();
        for (final ImageMessage imageMessage : messageList) {
            final StringBuffer errorMsg = validateRequest(imageMessage);
            messageMap.put("imageMessage", imageMessage);
            if (errorMsg.length() > 0) {
                ValidatorUtil.putErrorMapToList(list, errorMsg, imageMessage);
            }
        }
        return list;
    }

    private StringBuffer validateRequest (ImageMessage imageMessage) {
        final StringBuffer errorMsg = new StringBuffer();
        if (!skuGroupCatalogItemDao.skuGroupExists(Integer.parseInt(imageMessage.getOuterDomainData().getDomainId()))) {
            errorMsg.append("The provided domainId does not exist.");
        }

        if (StringUtils.isEmpty(DomainDataUtil.getDomainProvider(imageMessage.getOuterDomainData().getProvider(), providerProperties))) {
            errorMsg.append("The provided mediaProvider does not exist.");
        }

        if (imageMessage.getOuterDomainData().getDomain().equals(Domain.LODGING) && !mediaDomainCategoriesDao.subCategoryIdExists(imageMessage.getOuterDomainData(), DEFAULT_LANG_ID)) {
            errorMsg.append("The provided category does not exist.");
        }
        
        final List<Integer> invalidRoomIds = roomTypeDao.getInvalidRoomIds(imageMessage.getOuterDomainData());
        if (!invalidRoomIds.isEmpty()) {
            errorMsg.append("rooms " + invalidRoomIds + " are not belong to the property.");
        }


        if (DomainDataUtil.duplicateRoomExists(imageMessage.getOuterDomainData())) {
            errorMsg.append("The request contains duplicate rooms.");
        }
        
        if (DomainDataUtil.roomsFieldIsInvalid(imageMessage.getOuterDomainData())) {
            errorMsg.append("Some of rooms entries have not roomId key");
        }

        return errorMsg;
    }
}
