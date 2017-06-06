package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.services.dao.MediaDao;
import com.expedia.content.media.processing.services.dao.domain.Comment;
import com.expedia.content.media.processing.services.dao.domain.DomainIdMedia;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.reqres.MediaByDomainIdResponse;
import com.expedia.content.media.processing.services.reqres.MediaGetResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.expedia.content.media.processing.services.util.JSONUtil.buildMapFromJson;

/**
 * Helper class for processing Media Get requests.
 */
@Component
@SuppressWarnings({"PMD.UnsynchronizedStaticDateFormatter"})
public class MediaGetProcessor {
    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS XXX", Locale.US);

    private final MediaDao mediaDao;

    @Autowired
    public MediaGetProcessor(MediaDao mediaDao) {
        this.mediaDao = mediaDao;
    }

    /**
     * Handles a MediaByDomainId Request.
     *
     * @param domain The domain the domain id belongs to.
     * @param domainId Identification of the domain item the media is required.
     * @param activeFilter Filter determining what images to return. When true only active are returned. When false only inactive media is returned. When
     * all then all are returned. All is set a default.
     * @param derivativeTypeFilter Inclusive filter to use to only return certain types of derivatives. Returns all derivatives if not specified.
     * @param derivativeCategoryFilter Inclusive filter to use to only return certain types of medias. Returns all medias if not specified.
     * @param pageSize Positive integer to filter the number of media displayed per page. pageSize is inclusive with pageIndex.
     * @param pageIndex Positive integer to filter the page to display. pageIndex is inclusive with pageSize.
     * @return A MediaByDomainResponse to represent the values from the Media DB.
     */
    public MediaByDomainIdResponse processMediaByDomainIDRequest(Domain domain, String domainId, String activeFilter, String derivativeTypeFilter,
                                                                 String derivativeCategoryFilter, Integer pageSize, Integer pageIndex) {
        final List<DomainIdMedia> domainIdMedias = mediaDao.getMediaByDomainId(domain, domainId, activeFilter, derivativeTypeFilter, derivativeCategoryFilter, pageSize, pageIndex)
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        final Integer totalMediaCount = mediaDao.getTotalMediaCountByDomainId(domain, domainId, activeFilter, derivativeCategoryFilter).orElse(0);
        return MediaByDomainIdResponse.builder()
                .domain(domain.getDomain())
                .domainId(domainId)
                .totalMediaCount(totalMediaCount)
                .images(domainIdMedias)
                .build();
    }

    /**
     * Handles a MediaGet request by media guid
     *
     * @param mediaGuid The MediaGuid of the Media to return.
     * @return An Optional MediaGetResponse if the guid exists in the DB, otherwise an Optional.empty() is returned.
     */
    public Optional<MediaGetResponse> processMediaGetRequest(String mediaGuid) {
        final Optional<Media> media = mediaDao.getMediaByGuid(mediaGuid);
        if (media.isPresent()) {
            return Optional.of(convertMediaToMediaGetResponse(media.get()));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Converts a Media object returned by the MediaDB into a MediaGetResponse.
     *
     * @param media The Media object to return.
     * @return A MediaGetResponse object representative of the Media object passed.
     */
    private MediaGetResponse convertMediaToMediaGetResponse(Media media) {
        return MediaGetResponse.builder()
                .mediaGuid(media.getMediaGuid())
                .fileUrl(media.getFileUrl())
                .sourceUrl(media.getSourceUrl())
                .fileName(media.getFileName())
                .active(media.getActive())
                .width(media.getWidth())
                .height(media.getHeight())
                .fileSize(media.getFileSize())
                .status(media.getStatus())
                .lastUpdatedBy(media.getClientId())
                .lastUpdateDateTime(DATE_FORMAT.format(media.getLastUpdated()))
                .domainProvider(media.getProvider())
                .domainDerivativeCategory(media.getDomainDerivativeCategory())
                .domainFields(media.getDomainFields() == null ? new HashMap<>() : buildMapFromJson(media.getDomainFields()))
                .derivatives(media.getDerivativesList())
                .comments(media.getCommentList().stream()
                        .map(commentString -> Comment.builder()
                            .note(commentString)
                            .timestamp(DATE_FORMAT.format(media.getLastUpdated()))
                            .build())
                        .collect(Collectors.toList()))
                .domain(media.getDomain())
                .domainId(media.getDomainId())
                .build();
    }
}
