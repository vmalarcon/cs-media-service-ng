package com.expedia.content.media.processing.services.dao;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.Media;

import java.util.List;

public interface MediaUpdateDao {


    List<Media> getMediaByDomainId(Domain domain, String domainId, String activeFilter, String derivativeFilter);




    LcmMedia getContentProviderName(String fileName);
}
