package com.expedia.content.media.processing.services.validator;

import java.util.ArrayList;
import java.util.List;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.util.DomainDataUtil;
import com.expedia.content.media.processing.services.util.ValidatorUtil;

public class RoomValidator implements MapMessageValidator {

    public List<String> validateImages(List<ImageMessage> messageList) {
        final List<String> list = new ArrayList<>();
        for (final ImageMessage imageMessage : messageList) {
            final StringBuffer errorMsg = new StringBuffer();
            if(DomainDataUtil.duplicateRoomExists(imageMessage.getOuterDomainData())){
                errorMsg.append("There are duplicate room ids exist in request.");
            }
            if (errorMsg.length() > 0) {
                ValidatorUtil.putErrorMapToList(list, errorMsg);
            }
        }
        return list;
    }
}
