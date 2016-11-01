package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.dao.MediaDomainCategoriesDao;
import com.expedia.content.media.processing.services.dao.RoomTypeDao;
import com.expedia.content.media.processing.services.dao.SKUGroupCatalogItemDao;
import com.expedia.content.media.processing.services.util.DomainDataUtil;
import com.expedia.content.media.processing.services.util.ValidatorUtil;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Provided the validation logic for MediaAdd
 */
@SuppressWarnings({"PMD.UnusedPrivateField"})
public class LodgingValidator implements MapMessageValidator {

    @Autowired
    @Getter protected SKUGroupCatalogItemDao skuGroupCatalogItemDao;

    @Autowired
    @Getter protected MediaDomainCategoriesDao mediaDomainCategoriesDao;

    @Autowired
    @Getter protected RoomTypeDao roomTypeDao;

    @Resource(name = "providerProperties") 
    @Getter protected Properties providerProperties;

    public final static String DEFAULT_LANG_ID = "1033";

    @Override
    public List<String> validateImages(List<ImageMessage> messageList) {
        final List<String> list = new ArrayList<>();
        final StringBuffer errorMsg = new StringBuffer();
        for (final ImageMessage imageMessage : messageList) {
            try {
                final List<Object> invalidRoomIds = roomTypeDao.getInvalidRoomIds(imageMessage.getOuterDomainData());
                if (!invalidRoomIds.isEmpty()) {
                    errorMsg.append("The following roomIds " + invalidRoomIds + " do not belong to the property.");
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

            if (!mediaDomainCategoriesDao.subCategoryIdExists(imageMessage.getOuterDomainData(), DEFAULT_LANG_ID)) {
                errorMsg.append("The provided category does not exist.");
            }
            domainFieldsValidation(errorMsg, imageMessage);
            if (errorMsg.length() > 0) {
                ValidatorUtil.putErrorMapToList(list, errorMsg);
            }
        }
        return list;
    }

    private void domainFieldsValidation(final StringBuffer errorMsg, ImageMessage imageMessage) {
        final OuterDomain outerDomain = imageMessage.getOuterDomainData();
        final Object domainFields = outerDomain == null ? null : outerDomain.getDomainFields();
        if (!DomainDataUtil.domainFieldIsValid(domainFields)) {
            errorMsg.append("The provided domainFields must be a valid Map.");
        }
    }
}
