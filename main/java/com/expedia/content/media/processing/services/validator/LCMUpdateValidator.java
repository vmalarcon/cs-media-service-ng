package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.MediaDomainCategoriesDao;
import com.expedia.content.media.processing.services.dao.RoomTypeDao;
import com.expedia.content.media.processing.services.util.ValidatorUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

public class LCMUpdateValidator implements MapMessageValidator {
    @Autowired
    private RoomTypeDao roomTypeDao;
    @Autowired
    private MediaDomainCategoriesDao mediaDomainCategoriesDao;
    private final static String DEFAULT_LANG_ID = "1033";

    public List<String> validateImages(List<ImageMessage> messageList) {
        final List<String> list = new ArrayList<>();
        final Map messageMap = new HashMap();
        for (final ImageMessage imageMessage : messageList) {
            final StringBuffer errorMsg = new StringBuffer();
            messageMap.put("imageMessage", imageMessage);
            final List<Integer> invalidRoomIds = roomTypeDao.getInvalidRoomIds(imageMessage.getOuterDomainData());
            if (!invalidRoomIds.isEmpty()) {
                errorMsg.append("rooms " + invalidRoomIds + " are not belong to the property.");
            }
            if (!mediaDomainCategoriesDao.subCategoryIdExists(imageMessage.getOuterDomainData(), DEFAULT_LANG_ID)) {
                errorMsg.append("The provided category does not exist.");
            }
            if (errorMsg.length() > 0) {
                ValidatorUtil.putErrorMapToList(list, errorMsg, imageMessage);
            }
        }
        return list;
    }
}
