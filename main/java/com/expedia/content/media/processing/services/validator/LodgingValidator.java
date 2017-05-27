package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.DomainCategoriesDao;
import com.expedia.content.media.processing.services.dao.mediadb.MediaDBLodgingReferenceRoomIdDao;
import com.expedia.content.media.processing.services.util.DomainDataUtil;
import com.expedia.content.media.processing.services.util.ValidatorUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Provided the validation logic for MediaAdd
 */
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class LodgingValidator implements MapMessageValidator {

    @Autowired
    private MediaDBLodgingReferenceRoomIdDao mediaDBLodgingReferenceRoomIdDao;

    @Autowired
    private DomainCategoriesDao mediaDBMediaDomainCategoriesDao;

    private final static String DEFAULT_LANG_ID = "1033";

    @Override
    public List<String> validateImages(List<ImageMessage> messageList) {
        final List<String> list = new ArrayList<>();
        final StringBuffer errorMsg = new StringBuffer();
        for (final ImageMessage imageMessage : messageList) {
            try {
                final List<Object> invalidRoomIds = mediaDBLodgingReferenceRoomIdDao.getInvalidRoomIds(imageMessage.getOuterDomainData());
                if (!invalidRoomIds.isEmpty()) {
                    errorMsg.append("The following roomIds ").append(invalidRoomIds).append(" do not belong to the property.");
                }
                if (DomainDataUtil.duplicateRoomExists(imageMessage.getOuterDomainData())) {
                    errorMsg.append("The request contains duplicate rooms.");
                }
                if (DomainDataUtil.roomsFieldIsInvalid(imageMessage.getOuterDomainData())) {
                    errorMsg.append("Some room-entries have no roomId key.");
                }
            } catch (ClassCastException e) {
                errorMsg.append("The rooms field must be a list.");
            }

            if (!mediaDBMediaDomainCategoriesDao.subCategoryIdExists(imageMessage.getOuterDomainData(), DEFAULT_LANG_ID)) {
                errorMsg.append("The provided category does not exist.");
            }
            if (errorMsg.length() > 0) {
                ValidatorUtil.putErrorMapToList(list, errorMsg);
            }
        }
        return list;
    }
}
