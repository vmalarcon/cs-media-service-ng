package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.util.DomainDataUtil;
import com.expedia.content.media.processing.services.util.ValidatorUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class LodgingAddValidator extends LodgingValidator {
    private final static String SUBCATEGORY_ID = "subcategoryId";


    public List<String> validateImages(List<ImageMessage> messageList) {
        final List<String> errorList = new ArrayList<>();
        for (final ImageMessage imageMessage : messageList) {
            final StringBuffer errorMsg = validateRequest(imageMessage);
            if (errorMsg.length() > 0) {
                ValidatorUtil.putErrorMapToList(errorList, errorMsg);
            }
        }
        if (errorList.isEmpty()) {
            errorList.addAll(super.validateImages(messageList));
        }
        return errorList;
    }
    
    private StringBuffer validateRequest (ImageMessage imageMessage) {
        final StringBuffer errorMsg = new StringBuffer();
        if (!getMediaDBLodgingReferenceHotelIdDao().domainIdExists(imageMessage.getOuterDomainData().getDomainId())) {
            errorMsg.append("The provided domainId does not exist.");
        }
        
        if (StringUtils.isEmpty(DomainDataUtil.getDomainProvider(imageMessage.getOuterDomainData().getProvider(), getProviderProperties()))) {
            errorMsg.append("The provided mediaProvider does not exist.");
        }
        
        if (!imageMessage.getOuterDomainData().getDomain().equals(Domain.LODGING)) {
            errorMsg.append("The provided domain does not exist.");
        }

        if (isFeatureImage(imageMessage.getOuterDomainData())) {
            errorMsg.append("The provided category does not exist.");
        }
        return errorMsg;
    }

    /**
     * verifies the subCategory in the message is not 3
     * if an image is feature, propertyHero should be set to true, not subcategory being set to 3
     *
     * @param outerDomain
     * @return
     */
    private static Boolean isFeatureImage(OuterDomain outerDomain) {
        final String category = getCategory(outerDomain);
        Boolean isFeature = Boolean.FALSE;
        if (org.apache.commons.lang.StringUtils.isNotBlank(category) && org.apache.commons.lang.StringUtils.isNumeric(category)) {
            isFeature = String.valueOf(3).equals(category) ? Boolean.TRUE : Boolean.FALSE;
        }
        return isFeature;
    }

    /**
     * extracts category
     *
     * @param outerDomain
     * @return
     */
    private static String getCategory(OuterDomain outerDomain) {
        final String category = outerDomain.getDomainFields() == null ||
                outerDomain.getDomainFields().get(SUBCATEGORY_ID) == null ? "" :
                outerDomain.getDomainFields().get(SUBCATEGORY_ID).toString();
        return category;
    }

}
