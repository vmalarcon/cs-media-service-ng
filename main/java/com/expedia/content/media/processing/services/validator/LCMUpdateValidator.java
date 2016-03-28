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
import java.util.*;

public class LCMUpdateValidator implements MapMessageValidator {


    @Autowired
    private RoomTypeDao roomTypeDao;


    private final static String DEFAULT_LANG_ID = "1033";

    public List<Map<String, String>> validateImages(List<ImageMessage> messageList) {
        final List<Map<String, String>> list = new ArrayList<>();
        final Map messageMap = new HashMap();
        for (final ImageMessage imageMessage : messageList) {
            final StringBuffer errorMsg = new StringBuffer();
            messageMap.put("imageMessage", imageMessage);

            if (!roomTypeDao.roomTypeCatalogItemIdExists(imageMessage.getOuterDomainData())) {
                errorMsg.append("The room does not belong to the property in LCM.");
            }

            if (errorMsg.length() > 0) {
                ValidatorUtil.putErrorMapToList(list, errorMsg, imageMessage);
            }
        }
        return list;
    }
}
