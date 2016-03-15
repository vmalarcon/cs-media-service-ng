package com.expedia.content.media.processing.services.dao;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaDerivative;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.domain.MediaProcessLog;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaRoom;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaContentProviderNameGetSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLRoomGetSproc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.services.dao.dynamo.DynamoMediaRepository;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaGetSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaIdListSproc;
import com.expedia.content.media.processing.services.util.ActivityMapping;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Media data access operations through LCM and the Dynamo MediaDB.
 */
@Component
public class LcmDynamoMediaDao implements MediaDao {

    private static final int DEFAULT_LODGING_LOCALE = 1033;
    private static final int LCM_HERO_CATEGORY = 3;
    private static final long KB_TO_BYTES_CONVERTER = 1024L;
    private static final String LODGING_VIRTUAL_TOUR_DERIVATIVE_TYPE = "VirtualTour";
    private static final String ACTIVE_FILTER_ALL = "all";
    private static final String ACTIVE_FILTER_TRUE = "true";
    private static final String ACTIVE_FILTER_FALSE = "false";
    private static final String FIELD_CATEGORY_ID = "categoryId";
    private static final String FIELD_PROPERTY_HERO = "propertyHero";
    private static final String FIELD_ROOM_ID = "roomId";
    private static final String FIELD_ROOM_HERO = "roomHero";
    private static final String FIELD_ROOMS = "rooms";
    private static final String FIELD_DERIVATIVE_LOCATION = "location";
    private static final String FIELD_DERIVATIVE_TYPE = "type";
    private static final String FIELD_DERIVATIVE_WIDTH = "width";
    private static final String FIELD_DERIVATIVE_HEIGHT = "height";
    private static final String FIELD_DERIVATIVE_FILE_SIZE = "fileSize";

    private static final Logger LOGGER = LoggerFactory.getLogger(LcmDynamoMediaDao.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private SQLMediaIdListSproc lcmMediaIdSproc;
    @Autowired
    private SQLMediaGetSproc lcmMediaSproc;
    @Autowired
    private SQLRoomGetSproc roomGetSproc;
    @Autowired
    private SQLMediaContentProviderNameGetSproc mediaContentProviderNameGetSproc;
    @Autowired
    private DynamoMediaRepository mediaRepo;
    @Autowired
    private LcmProcessLogDao processLogDao;
    @Autowired
    private List<ActivityMapping> activityWhiteList;
    @Resource(name = "providerProperties")
    private Properties providerProperties;
    @Value("${image.root.path}")
    private String imageRootPath;
    @Value("${media.status.sproc.param.limit}")
    private int paramLimit;

    @Override
    public List<Media> getMediaByFilename(String fileName) {
        return mediaRepo.getMediaByFilename(fileName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Media> getMediaByDomainId(Domain domain, String domainId, String activeFilter, String derivativeFilter) {
        List<Media> domainIdMedia = mediaRepo.loadMedia(domain, domainId).stream().collect(Collectors.toList());
        if (Domain.LODGING.equals(domain)) {
            final Map<String, Media> mediaLcmMediaIdMap =
                    domainIdMedia.stream().filter(media -> media.getLcmMediaId() != null && !"null".equals(media.getLcmMediaId()))
                            .collect(Collectors.toMap(Media::getLcmMediaId, media -> media));
            final Map<String, Object> idResult = lcmMediaIdSproc.execute(Integer.parseInt(domainId), DEFAULT_LODGING_LOCALE);
            final List<Integer> mediaIds = (List<Integer>) idResult.get(SQLMediaIdListSproc.MEDIA_ID_SET);
            /* @formatter:off */
            final List<Media> lcmMediaList = mediaIds.stream()
                    .map(buildLcmMedia(domainId, derivativeFilter))
                    .map(convertMedia(mediaLcmMediaIdMap))
                    .collect(Collectors.toList());
            /* @formatter:on */
            domainIdMedia.removeAll(mediaLcmMediaIdMap.values());
            domainIdMedia.addAll(0, lcmMediaList);

        }
        domainIdMedia =
                domainIdMedia.stream()
                        .filter(media -> (activeFilter == null || activeFilter.isEmpty() || activeFilter.equals(ACTIVE_FILTER_ALL)
                                || (activeFilter.equals(ACTIVE_FILTER_TRUE) && media.getActive() != null && media.getActive().equals(ACTIVE_FILTER_TRUE))
                                || (activeFilter.equals(ACTIVE_FILTER_FALSE)
                                && (media.getActive() == null || media.getActive().equals(ACTIVE_FILTER_FALSE)))) ? true : false)
                        .sorted((media1, media2) -> compareMedia(media1, media2, domain))
                        .collect(Collectors.toList());
        final List<String> fileNames =
                domainIdMedia.stream().filter(media -> media.getFileName() != null).map(media -> media.getFileName()).distinct()
                        .collect(Collectors.toList());

        final Map<String, String> fileStatus = getStatusByLoop(paramLimit, fileNames);
        domainIdMedia.stream().forEach(media -> media.setStatus(fileStatus.get(media.getFileName())));
        return domainIdMedia;
    }

    /**
     * Compares two media objects for sorting.
     * 
     * @param media1 First media to compare.
     * @param media2 Second media to compare.
     * @param domain Different domains will have different sorting requirements.
     * @return 0 if the media objects weigh the same, greater than 1 if the first item weighs more, less than 1 if the first item weighs less.
     */
    private int compareMedia(Media media1, Media media2, Domain domain) {
        if (Domain.LODGING.equals(domain)) {
            final Boolean media1Hero = Boolean.valueOf(media1.getDomainData() == null ? null : ((String) media1.getDomainData().get("propertyHero")));
            final Boolean media2Hero = Boolean.valueOf(media2.getDomainData() == null ? null : ((String) media2.getDomainData().get("propertyHero")));
            return media2Hero.compareTo(media1Hero);
        }
        return 0;
    }

    /**
     * because of the parameter length limitation in Sproc 'MediaProcessLogGetByFilename', if the fileNames lengh is bigger than the limitation
     * we call the sproc multiple times to get the result.
     *
     * @param limit Sproc parameter limitation.
     * @param fileNames File names to fetch the status for.
     * @return List of all status mapped to their file name.
     */
    private Map<String, String> getStatusByLoop(int limit, List<String> fileNames) {
        final int total = fileNames.size();
        Map<String, String> fileStatusAll = new HashMap<>();
        if (total > limit) {
            final int sprocCallCount = total / limit;
            final int rest = total % limit;
            int start = 0;
            for (int i = 0; i < sprocCallCount; i++) {
                final List<String> subList = fileNames.subList(start, start + limit);
                final Map<String, String> fileStatus = getLatestStatus(subList);
                fileStatusAll.putAll(fileStatus);
                start = start + limit;
            }
            final List<String> leftNames = fileNames.subList(total - rest, total);
            if (!leftNames.isEmpty()) {
                final Map<String, String> fileStatus = getLatestStatus(leftNames);
                fileStatusAll.putAll(fileStatus);
            }

        } else {
            fileStatusAll = getLatestStatus(fileNames);
        }
        return fileStatusAll;
    }

    /**
     * From a domain id builds a media item from LCM data. Allows an inclusive filter for derivatives.
     *
     * @param domainId         Id of the domain object for which the media is required.
     * @param derivativeFilter Inclusive filter of derivatives. A null or empty string will not exclude any derivatives.
     * @return
     */
    @SuppressWarnings("unchecked")
    private Function<Integer, LcmMedia> buildLcmMedia(String domainId, String derivativeFilter) {
        /* @formatter:off */
        return mediaId -> {
            final Map<String, Object> mediaResult = lcmMediaSproc.execute(Integer.parseInt(domainId), mediaId);
            final LcmMedia media = ((List<LcmMedia>) mediaResult.get(SQLMediaGetSproc.MEDIA_SET)).get(0);
            media.setDerivatives(((List<LcmMediaDerivative>) mediaResult.get(SQLMediaGetSproc.MEDIA_DERIVATIVES_SET)).stream()
                    .filter(derivative -> (derivativeFilter == null || derivativeFilter.isEmpty() ||
                                           derivativeFilter.contains(derivative.getMediSizeType())) ? true : false)
                    .collect(Collectors.toList()));
            return media;
        };
        /* @formatter:on */
    }

    /**
     * Function that converts LCM media item to a media item to return.
     *
     * @param mediaEidMap A map of media DB items that have an EID.
     * @return The converted LCM media.
     */
    @SuppressWarnings("PMD.NPathComplexity")
    private Function<LcmMedia, Media> convertMedia(final Map<String, Media> mediaEidMap) {
        return lcmMedia -> {
            final Media dynamoMedia = mediaEidMap.get(lcmMedia.getMediaId().toString());
            /* @formatter:off */
            return Media.builder()
                    .active(lcmMedia.getActive().toString())
                    .clientId((dynamoMedia == null) ? null : dynamoMedia.getClientId())
                    .derivativesList(extractDerivatives(lcmMedia))
                    .domain((dynamoMedia == null) ? null : dynamoMedia.getDomain())
                    .domainData(extractDomainFields(lcmMedia, dynamoMedia))
                    .domainId((dynamoMedia == null) ? String.valueOf(lcmMedia.getDomainId()) : dynamoMedia.getDomainId())
                    .fileName(lcmMedia.getFileName())
                    .fileSize(lcmMedia.getFileSize() * KB_TO_BYTES_CONVERTER)
                    .width(lcmMedia.getWidth())
                    .height(lcmMedia.getHeight())
                    .lastUpdated(lcmMedia.getLastUpdateDate())
                    .lcmMediaId(lcmMedia.getMediaId().toString())
                    .mediaGuid((dynamoMedia == null) ? null : dynamoMedia.getMediaGuid())
                    .provider((dynamoMedia == null) ? (lcmMedia.getProvider() == null ? null
                                                                    : providerProperties.getProperty(lcmMedia.getProvider().toString()))
                                                    : dynamoMedia.getProvider())
                    .sourceUrl((dynamoMedia == null) ? null : dynamoMedia.getSourceUrl())
                    .userId(lcmMedia.getLastUpdatedBy())
                    .commentList(extractCommentList(lcmMedia))
                    .domainDerivativeCategory(lcmMedia.getFormatId() == null || !lcmMedia.getFormatId().equals(2) ?
                                                                                  null : LODGING_VIRTUAL_TOUR_DERIVATIVE_TYPE)
                    .build();
            /* @formatter:on */
        };
    }

    /**
     * Builds a comment list from the LCM media comment. There is only one comment in LCM, but the response expects a list.
     *
     * @param lcmMedia Data object containing media data from LCM.
     * @return The latest status of a media file.
     */
    private List<String> extractCommentList(final LcmMedia lcmMedia) {
        if (lcmMedia.getComment() != null && !lcmMedia.getComment().isEmpty()) {
            final List<String> comments = new ArrayList<>();
            comments.add(lcmMedia.getComment());
            return comments;
        }
        return null;
    }

    /**
     * Pulls the latest processing status of media files. When a file doesn't have any process logs the file is
     * considered old and therefore published.
     *
     * @param fileNames File names for which the status is required.
     * @return The latest status of a media filesS.
     */
    private Map<String, String> getLatestStatus(List<String> fileNames) {
        List<MediaProcessLog> logs = processLogDao.findMediaStatus(fileNames);
        logs = logs == null ? new ArrayList<>() : logs;
        final Map<String, List<MediaProcessLog>> fileNameLogListMap = new HashMap<>();
        JSONUtil.divideStatusListToMap(logs, fileNameLogListMap, fileNames.size());

        return fileNames.stream().distinct().collect(Collectors.toMap(String::toString, fileName -> {
            final List<MediaProcessLog> logList = fileNameLogListMap.get(fileName);
            final ActivityMapping activityStatus = (logList == null) ? null : getLatestActivityMapping(logList);
            return activityStatus == null ? "PUBLISHED" : activityStatus.getStatusMessage();
        }));
    }

    /**
     * Finds the latest activity that is part of the activity white list. The list is expected to
     * be ordered from oldest to latest.
     *
     * @param logList The list to search.
     * @return The found activity mapping. {@code null} if no activity is found.
     */
    private ActivityMapping getLatestActivityMapping(List<MediaProcessLog> logList) {
        ActivityMapping activityMapping = null;
        for (int i = logList.size() - 1; i >= 0; i--) {
            final MediaProcessLog mediaProcessLog = logList.get(i);
            activityMapping = JSONUtil.getActivityMappingFromList(activityWhiteList, mediaProcessLog.getActivityType(), mediaProcessLog.getMediaType());
            if (activityMapping == null) {
                continue;
            } else {
                return activityMapping;
            }
        }
        return activityMapping;
    }

    /**
     * Extract domain data to be added to a media data response.
     *
     * @param lcmMedia    Data object containing media data from LCM.
     * @param dynamoMedia Data object containing media data form the Media DB.
     * @return A map of domain data.
     */
    private Map<String, Object> extractDomainFields(final LcmMedia lcmMedia, final Media dynamoMedia) {
        Map<String, Object> domainData = null;
        if (dynamoMedia != null && dynamoMedia.getDomainFields() != null && !dynamoMedia.getDomainFields().isEmpty()) {
            try {
                domainData = OBJECT_MAPPER.readValue(dynamoMedia.getDomainFields(), new TypeReference<HashMap<String, Object>>() {
                });
                dynamoMedia.setDomainData(domainData);
            } catch (IOException e) {
                LOGGER.warn("Domain fields not stored in proper JSON format for media id {}.", dynamoMedia.getMediaGuid(), e);
            }
        }
        final Map<String, Object> lcmDomainData = new HashMap<String, Object>();
        extractLcmDomainFields(lcmMedia, dynamoMedia, lcmDomainData);
        extractLcmRooms(lcmMedia, lcmDomainData);
        return lcmDomainData.isEmpty() ? domainData : lcmDomainData;
    }

    /**
     * Extract LCM domain data and stores into a passed map to be interpreted the same way as if it would have been pulled in JSON from
     * the media DB.
     *
     * @param lcmMedia      Data object containing media data from LCM.
     * @param dynamoMedia   Data object containing media data form the Media DB.
     * @param lcmDomainData Map to store extracted LCM data into.
     */
    private void extractLcmDomainFields(final LcmMedia lcmMedia, final Media dynamoMedia, final Map<String, Object> lcmDomainData) {
        if (lcmMedia.getCategory() != null) {
            if (lcmMedia.getCategory().equals(LCM_HERO_CATEGORY)) {
                lcmDomainData.put(FIELD_PROPERTY_HERO, "true");
                if (dynamoMedia != null && dynamoMedia.getDomainData() != null && dynamoMedia.getDomainData().get(FIELD_CATEGORY_ID) != null) {
                    lcmDomainData.put(FIELD_CATEGORY_ID, String.valueOf(dynamoMedia.getDomainData().get(FIELD_CATEGORY_ID)));
                }
            } else {
                lcmDomainData.put(FIELD_CATEGORY_ID, lcmMedia.getCategory().toString());
            }
        }
    }

    /**
     * Extract LCM domain room data and stores into a passed map to be interpreted the same way as if it would have been pulled in
     * JSON from the media DB.
     *
     * @param lcmMedia      Data object containing media data from LCM.
     * @param lcmDomainData Map to store extracted LCM data into.
     */
    private void extractLcmRooms(final LcmMedia lcmMedia, final Map<String, Object> lcmDomainData) {
        final Map<String, Object> roomResult = roomGetSproc.execute(lcmMedia.getMediaId());
        final List<LcmMediaRoom> lcmMediaRoomList = (List<LcmMediaRoom>) roomResult.get(SQLRoomGetSproc.MEDIA_SET);
        if (!lcmMediaRoomList.isEmpty()) {
            final List<Map<String, String>> roomList = lcmMediaRoomList.stream().map(room -> {
                Map<String, String> roomData = new HashMap<>();
                roomData.put(FIELD_ROOM_ID, String.valueOf(room.getRoomId()));
                roomData.put(FIELD_ROOM_HERO, room.getRoomHero().toString());
                return roomData;
            }).collect(Collectors.toList());
            lcmDomainData.put(FIELD_ROOMS, roomList);
        }
    }

    @Override
    public LcmMedia getContentProviderName(final String dcpFileName) {
        final Map<String, Object> fileResult = mediaContentProviderNameGetSproc.execute(dcpFileName);
        final List<LcmMedia> lcmMediaList = (List<LcmMedia>) fileResult.get(SQLMediaContentProviderNameGetSproc.MEDIA_ATTRS);
        if (!lcmMediaList.isEmpty()) {
            final Optional<LcmMedia> lcmMediaOptional = lcmMediaList.stream().filter(media -> media.getFilProcessedBool()).findFirst();
            if (lcmMediaOptional.isPresent()) {
                return lcmMediaOptional.get();
            }
        }
        return null;
    }

    /**
     * Extract derivative data from the media objects.
     * JSON from the media DB.
     *
     * @param lcmMedia Data object containing media data from LCM.
     * @return
     */
    private List<Map<String, Object>> extractDerivatives(final LcmMedia lcmMedia) {
        List<Map<String, Object>> derivatives = null;
        if (lcmMedia.getDerivatives() != null && !lcmMedia.getDerivatives().isEmpty()) {
            derivatives = lcmMedia.getDerivatives().stream().filter(derivative -> derivative.getFileProcessed())
                    .map(derivative -> {
                        final Map<String, Object> derivativeData = new HashMap<>();
                        derivativeData.put(FIELD_DERIVATIVE_LOCATION, imageRootPath + derivative.getFileName());
                        derivativeData.put(FIELD_DERIVATIVE_TYPE, derivative.getMediSizeType());
                        derivativeData.put(FIELD_DERIVATIVE_WIDTH, derivative.getWidth());
                        derivativeData.put(FIELD_DERIVATIVE_HEIGHT, derivative.getHeight());
                        derivativeData.put(FIELD_DERIVATIVE_FILE_SIZE, derivative.getFileSize() * KB_TO_BYTES_CONVERTER);
                        return derivativeData;
                    }).collect(Collectors.toList());
        }
        return derivatives;
    }

}
