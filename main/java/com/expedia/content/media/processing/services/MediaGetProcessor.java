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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.expedia.content.media.processing.services.util.JSONUtil.buildMapFromJson;

// TODO: JavaDoc ALL the things
@Component
public class MediaGetProcessor {

    private final MediaDao mediaDao;

    @Autowired
    public MediaGetProcessor(MediaDao mediaDao) {
        this.mediaDao = mediaDao;
    }

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

    public Optional<MediaGetResponse> processMediaGetRequest(String mediaGuid) {
        final Optional<Media> media = mediaDao.getMediaByGuid(mediaGuid);
        if (media.isPresent()) {
            return Optional.of(convertMediaToMediaGetResponse(media.get()));
        } else {
            return Optional.empty();
        }
    }

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
                // TODO check if timeStamp format is correct here.
                .lastUpdateDateTime(media.getLastUpdated().toString())
                .domainProvider(media.getProvider())
                .domainDerivativeCategory(media.getDomainDerivativeCategory())
                .domainFields(buildMapFromJson(media.getDomainFields()))
                .derivatives(media.getDerivativesList())
                .comments(media.getCommentList().stream()
                        .map(commentString -> Comment.builder()
                            .note(commentString)
                            .timestamp(media.getLastUpdated().toString())
                            .build())
                        .collect(Collectors.toList()))
                .domain(media.getDomain())
                .domainId(media.getDomainId())
                .build();
    }
}
