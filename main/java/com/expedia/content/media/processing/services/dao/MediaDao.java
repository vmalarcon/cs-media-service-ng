package com.expedia.content.media.processing.services.dao;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * TODO
 */
@Component
public class MediaDao {

    private static final int DEFAULT_LODGING_LOCALE = 1033;
    private static final String ACTIVE_FILTER_ALL = "all";
    private static final String ACTIVE_FILTER_TRUE = "true";
    private static final String ACTIVE_FILTER_FALSE = "false";

    private final SQLMediaIdListSproc lcmMediaIdSproc;
    private final SQLMediaGetSproc lcmMediaSproc;

    @Autowired
    public MediaDao(SQLMediaIdListSproc lcmMediaIdSproc, SQLMediaGetSproc lcmMediaSproc) {
        this.lcmMediaIdSproc = lcmMediaIdSproc;
        this.lcmMediaSproc = lcmMediaSproc;
    }

    @SuppressWarnings("unchecked")
    public List<Media> getMediaByDomainId(String domain, String domainId, String activeFilter, String derivativeFilter) {
        if ("lodging".equals(domain.toLowerCase(Locale.US))) {
            final Map<String, Object> idResult = lcmMediaIdSproc.execute(Integer.parseInt(domainId), DEFAULT_LODGING_LOCALE);
            final List<Integer> mediaIds = (List<Integer>) idResult.get(SQLMediaIdListSproc.MEDIA_ID_SET);
            /* @formatter:off */
            return mediaIds.stream()
                .map(mediaId -> {
                    final Map<String, Object> mediaResult = lcmMediaSproc.execute(Integer.parseInt(domainId), mediaId);
                    final Media media = ((List<Media>) mediaResult.get(SQLMediaGetSproc.MEDIA_SET)).get(0);
                    media.setDerivatives(((List<MediaDerivative>) mediaResult.get(SQLMediaGetSproc.MEDIA_DERIVATIVES_SET)).stream()
                            .filter(derivative -> (derivativeFilter == null || derivativeFilter.isEmpty() ||
                                                   derivativeFilter.contains(derivative.getMediSizeType())) ? true : false)
                            .collect(Collectors.toList()));
                    return media;
                })
                .filter(media -> (activeFilter == null || activeFilter.isEmpty() || activeFilter.equals(ACTIVE_FILTER_ALL) ||
                                 (activeFilter.equals(ACTIVE_FILTER_TRUE) && media.isActive()) ||
                                 (activeFilter.equals(ACTIVE_FILTER_FALSE) && !media.isActive())) ? true : false)
                .collect(Collectors.toList());
            /* @formatter:on */
        }
        return null;
    }

}
