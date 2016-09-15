package com.expedia.content.media.processing.services.validator;


import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.annotation.Resource;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.dao.MediaDomainCategoriesDao;
import com.expedia.content.media.processing.services.dao.RoomTypeDao;
import com.expedia.content.media.processing.services.dao.SKUGroupCatalogItemDao;
import com.expedia.content.media.processing.services.util.DomainDataUtil;
import com.expedia.content.media.processing.services.util.ValidatorUtil;

import org.springframework.beans.factory.annotation.Autowired;

import lombok.Getter;

@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.ModifiedCyclomaticComplexity", "PMD.StdCyclomaticComplexity", "PMD.UnusedPrivateField", "PMD.NPathComplexity"})
public class LCMValidator implements MapMessageValidator {

    @Autowired
    @Getter private SKUGroupCatalogItemDao skuGroupCatalogItemDao;

    @Autowired
    @Getter private MediaDomainCategoriesDao mediaDomainCategoriesDao;

    @Autowired
    @Getter private RoomTypeDao roomTypeDao;

    @Resource(name = "providerProperties")
    @Getter private Properties providerProperties;

    private final static String DEFAULT_LANG_ID = "1033";
    
    @Override
    public List<String> validateImages(List<ImageMessage> messageList) {
        final List<String> list = new ArrayList<>();
        for (final ImageMessage imageMessage : messageList) {
            final StringBuffer errorMsg = new StringBuffer();
            try {
                final List<Integer> invalidRoomIds = roomTypeDao.getInvalidRoomIds(imageMessage.getOuterDomainData());
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
            
            final OuterDomain outerDomain = imageMessage.getOuterDomainData();
            final Object domainFields = outerDomain == null ? null : outerDomain.getDomainFields();
            if(!DomainDataUtil.domainFieldIsAMap(domainFields)){
                errorMsg.append("The provided domainFields must be a Map.");
            }
            if (errorMsg.length() > 0) {
                ValidatorUtil.putErrorMapToList(list, errorMsg);
            }          
        }
        return list;
    }  
 }
