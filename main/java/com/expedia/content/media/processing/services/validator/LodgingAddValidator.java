package com.expedia.content.media.processing.services.validator;

import java.util.List;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.util.DomainDataUtil;
import com.expedia.content.media.processing.services.util.ValidatorUtil;
import org.apache.commons.lang3.StringUtils;

public class LodgingAddValidator extends LodgingValidator {

    public List<String> validateImages(List<ImageMessage> messageList) {
        final List<String> errorList = super.validateImages(messageList);
        for (final ImageMessage imageMessage : messageList) {
            final StringBuffer errorMsg = validateRequest(imageMessage);
            if (errorMsg.length() > 0) {
                ValidatorUtil.putErrorMapToList(errorList, errorMsg);
            }
        }
        return errorList;

    }

    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.ModifiedCyclomaticComplexity", "PMD.StdCyclomaticComplexity", "PMD.NPathComplexity"})
    private StringBuffer validateRequest(ImageMessage imageMessage) {
        final StringBuffer errorMsg = new StringBuffer();
        if (!getSkuGroupCatalogItemDao().skuGroupExists(Integer.parseInt(imageMessage.getOuterDomainData().getDomainId()))) {
            errorMsg.append("The provided domainId does not exist.");
        }

        if (StringUtils.isEmpty(DomainDataUtil.getDomainProvider(imageMessage.getOuterDomainData().getProvider(), getProviderProperties()))) {
            errorMsg.append("The provided mediaProvider does not exist.");
        }

        if (!imageMessage.getOuterDomainData().getDomain().equals(Domain.LODGING)) {
            errorMsg.append("The provided domain does not exist.");
        }
        return errorMsg;
    }

}
