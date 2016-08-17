package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.util.DomainDataUtil;
import com.expedia.content.media.processing.services.util.ValidatorUtil;

import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class RoomValidator implements MapMessageValidator {

    public List<String> validateImages(List<ImageMessage> messageList) {
        final List<String> list = new ArrayList<>();
        final Map messageMap = new HashMap();
        for (final ImageMessage imageMessage : messageList) {
            final StringBuffer errorMsg = new StringBuffer();
            messageMap.put("imageMessage", imageMessage);
            final List<Integer> roomIds = DomainDataUtil.getRoomIds(imageMessage.getOuterDomainData());
            final Set inputSet = new HashSet(roomIds);
            if (inputSet.size() < roomIds.size()) {
                errorMsg.append("There are duplicate room ids exist in request.");
            }
            if (errorMsg.length() > 0) {
                ValidatorUtil.putErrorMapToList(list, errorMsg);
            }
        }
        return list;
    }
}
