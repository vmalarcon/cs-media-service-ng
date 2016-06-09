package com.expedia.content.media.processing.services.dao;

import com.amazonaws.util.StringUtils;
import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaAndDerivative;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaDerivative;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaRoom;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.domain.MediaProcessLog;
import com.expedia.content.media.processing.services.dao.dynamo.DynamoMediaRepository;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaContentProviderNameGetSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaGetSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaItemGetSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaListSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLRoomGetByCatalogItemIdSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLRoomGetByMediaIdSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLRoomGetSproc;
import com.expedia.content.media.processing.services.reqres.Comment;
import com.expedia.content.media.processing.services.reqres.DomainIdMedia;
import com.expedia.content.media.processing.services.reqres.MediaByDomainIdResponse;
import com.expedia.content.media.processing.services.reqres.MediaGetResponse;
import com.expedia.content.media.processing.services.util.ActivityMapping;
import com.expedia.content.media.processing.services.util.FileNameUtil;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Media data access operations through LCM and the Dynamo MediaDB.
 */
@Component
public class LcmDynamoMediaDao implements MediaDao {

    private static final int LCM_HERO_CATEGORY = 3;
    private static final long KB_TO_BYTES_CONVERTER = 1024L;
    private static final String LODGING_VIRTUAL_TOUR_DERIVATIVE_TYPE = "VirtualTour";
    private static final String ACTIVE_FILTER_ALL = "all";
    private static final String ACTIVE_FILTER_TRUE = "true";
    private static final String ACTIVE_FILTER_FALSE = "false";
    private static final String FIELD_SUBCATEGORY_ID = "subcategoryId";
    private static final String FIELD_PROPERTY_HERO = "propertyHero";
    private static final String FIELD_ROOM_ID = "roomId";
    private static final String FIELD_ROOM_HERO = "roomHero";
    private static final String FIELD_ROOMS = "rooms";
    private static final String FIELD_DERIVATIVE_LOCATION = "location";
    private static final String FIELD_DERIVATIVE_TYPE = "type";
    private static final String FIELD_DERIVATIVE_WIDTH = "width";
    private static final String FIELD_DERIVATIVE_HEIGHT = "height";
    private static final String FIELD_DERIVATIVE_FILE_SIZE = "fileSize";
    private static final String RESPONSE_FIELD_LCM_MEDIA_ID = "lcmMediaId";
    private static final String PUBLISHED_ACTIVITY = "PUBLISHED";
    private static final String REJECTED_ACTIVITY = "REJECTED";
    private static final String DUPLICATE_ACTIVITY = "DUPLICATE";
    private static final String NULL_FILTER = null;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS ZZ");
    private static final Logger LOGGER = LoggerFactory.getLogger(LcmDynamoMediaDao.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Integer FORMAT_ID_2 = 2;
    private static final Integer ACTIVITY_COUNT_THRESHOLD = 12;

    @Autowired
    private SQLMediaListSproc lcmMediaListSproc;
    @Autowired
    private SQLMediaItemGetSproc lcmMediaItemSproc;
    @Autowired
    private SQLMediaGetSproc lcmMediaSproc;
    @Autowired
    private SQLRoomGetByMediaIdSproc roomGetByMediaIdSproc;
    @Autowired
    private SQLRoomGetByCatalogItemIdSproc roomGetByCatalogItemIdSproc;
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
    public Media getMediaByGuid(String guid) {
        return mediaRepo.getMedia(guid);
    }

    @Override
        public List<Media> getMediaByMediaId(String mediaId) {
        return mediaRepo.getMediaByMediaId(mediaId);
    }

    @Override
    public void saveMedia(Media media) {
        mediaRepo.saveMedia(media);
    }

    /**
     * Builds a @MediaByDomainIdResponse
     *
     * @param domain Domain the item belongs too.
     * @param domainId The id of the domain item media items are needed.
     * @param activeFilter Filters active or inactive media. If "all" or null is provided all items are returned.
     * @param derivativeFilter Inclusive filter of derivatives. A null or empty string will not exclude any derivatives.
     * @param derivativeCategoryFilter Inclusive filter of media. A null or empty string will not exclude any media.
     * @param pageSize Positive integer to filter the number of media displayed per page. pageSize is inclusive with pageIndex.
     * @param pageIndex Positive integer to filter the page to display. pageIndex is inclusive with pageSize.
     * @return A @MediaByDomainIdResponse
     * @throws Exception
     */
    @Override
    @SuppressWarnings("unchecked")
    public MediaByDomainIdResponse getMediaByDomainId(final Domain domain, final String domainId, final String activeFilter, final String derivativeFilter,
                                                      final String derivativeCategoryFilter, final Integer pageSize, final Integer pageIndex) throws Exception {
        List<Media> domainIdMedia = mediaRepo.loadMedia(domain, domainId).stream().map(media -> completeMedia(media, derivativeFilter)).collect(Collectors.toList());
        if (Domain.LODGING.equals(domain)) {
            extractLcmData(domainId, derivativeFilter, domainIdMedia);
        }
        final boolean isActiveFilterAll = activeFilter == null || activeFilter.isEmpty() || activeFilter.equals(ACTIVE_FILTER_ALL);
        Stream<Media> mediaStream = domainIdMedia.stream();
        if (!isActiveFilterAll) {
            mediaStream = mediaStream.filter(media -> ((activeFilter.equals(ACTIVE_FILTER_TRUE) && ACTIVE_FILTER_TRUE.equals(media.getActive()))
                    || (activeFilter.equals(ACTIVE_FILTER_FALSE) && (media.getActive() == null || media.getActive().equals(ACTIVE_FILTER_FALSE)))));
        }
        domainIdMedia = mediaStream.sorted((media1, media2) -> compareMedia(media1, media2, domain)).collect(Collectors.toList());
        final Integer totalMediaCount = (int) domainIdMedia.stream().filter(media -> media.getFileName() != null).map(Media::getFileName).distinct().count();
        if (pageSize != null || pageIndex != null) {
            final String errorResponse = validatePagination(totalMediaCount, pageSize, pageIndex);
            if (errorResponse == null) {
                domainIdMedia = (List<Media>) paginateItems(domainIdMedia.stream(), pageSize, pageIndex).collect(Collectors.toList());
            } else {
                throw new Exception(errorResponse);
            }
        }
        final Map<String, String> fileStatus = buildFileNameStatusMap(domainIdMedia, domainId);
        domainIdMedia.stream().forEach(media -> media.setStatus(fileStatus.get(media.getFileName())));
        final Function<Media, DomainIdMedia> buildImage = media -> new DomainIdMedia(transformSingleMediaForResponse(media));
        final Boolean skipCategoryFiltering = derivativeCategoryFilter == null || derivativeCategoryFilter.isEmpty();
        final List<DomainIdMedia> images = domainIdMedia.stream()
                .map(buildImage)
                .filter(media -> skipCategoryFiltering || media.getDomainDerivativeCategory() == null ? derivativeCategoryFilter.contains("null") : derivativeCategoryFilter.contains(media.getDomainDerivativeCategory()))
                .collect(Collectors.toList());
        return MediaByDomainIdResponse.builder().domain(domain.getDomain()).domainId(domainId).totalMediaCount(totalMediaCount)
                .images(images).build();
    }

    /**
     * TODO Once all media from LCM is is migrated in the media DB only completeMedia(mediaRepo.getMedia(mediaGUID), nullFilter) and the latest status will be needed.
     * Builds a @MediaGetResponse by mediaGuid (Supports LcmMediaId as well, but this feature will be deprecated in feature releases)
     *
     *
     * @param mediaGUID String identifier for a Media
     * @return A @MediaGetResponse
     */
    @Override
    public MediaGetResponse getMediaByGUID(String mediaGUID) {
        final boolean isLodgingNoGuid = mediaGUID.matches("\\d+");
        final Media media = isLodgingNoGuid ? resolveMediaByLcmMediaId(mediaGUID) : resolveMediaByGuid(mediaGUID);
        resolveMediaLatestStatus(media, isLodgingNoGuid);
        return transformSingleMediaForResponse(media);
    }

    /**
     * Extracts media data for a Hotel from LCM
     *
     * @param domainId Id of a hotel
     * @param derivativeFilter Inclusive filter of derivatives. A null or empty string will not exclude any derivatives.
     * @param domainIdMedia MediaDB media data
     */
    @SuppressWarnings("unchecked")
    private void extractLcmData(final String domainId, final String derivativeFilter, final List<Media> domainIdMedia) {
        final Integer domainIdInt = Integer.parseInt(domainId);
        final Map<String, Media> mediaLcmMediaIdMap = domainIdMedia.stream()
                .filter(media -> media.getLcmMediaId() != null && !"null".equals(media.getLcmMediaId()))
                .collect(Collectors.toMap(Media::getLcmMediaId, media -> media));
        final List<LcmMediaAndDerivative> mediaDerivativeItems = (List<LcmMediaAndDerivative>) lcmMediaListSproc.execute(domainIdInt).get(SQLMediaListSproc.MEDIA_SET);
        final Map<Integer, List<LcmMediaAndDerivative>> lcmMediaMap =
                mediaDerivativeItems.stream().collect(Collectors.groupingBy(mediaAndDerivative -> mediaAndDerivative.getMediaId()));

        final List<LcmMediaRoom> mediaRoomItems = (List<LcmMediaRoom>) roomGetByCatalogItemIdSproc.execute(domainIdInt).get(SQLRoomGetSproc.ROOM_SET);
        final Map<Integer, List<LcmMediaRoom>> lcmRoomMediaMap = mediaRoomItems.stream().collect(
                Collectors.groupingBy(mediaRoom -> mediaRoom.getMediaId()));

        final Function<Integer, Media> completeMediaWithLcmData = mediaId ->
                convertMedia(mediaLcmMediaIdMap, lcmRoomMediaMap).apply(convertLcmMediaAndDerivativeToLcmMedia(lcmMediaMap, mediaId, derivativeFilter));
        /* @formatter:off */
        final List<Media> lcmMediaList = lcmMediaMap.keySet().stream().parallel()
                .map(completeMediaWithLcmData)
                .collect(Collectors.toList());
        /* @formatter:on */
        domainIdMedia.removeAll(mediaLcmMediaIdMap.values());
        domainIdMedia.addAll(0, lcmMediaList);
    }

    /**
     * Resolves a Media by it's LcmMediaID
     *
     * @param lcmMediaIdString lcmMediaID of a Media
     * @return a completed Media object that corresponds to the LcmMediaID
     */
    @SuppressWarnings("unchecked")
    private Media resolveMediaByLcmMediaId(String lcmMediaIdString) {
        Integer lcmMediaId = Integer.parseInt(lcmMediaIdString);
        String domainId = null;
        final List<LcmMedia> lcmMediaList = (List<LcmMedia>) lcmMediaSproc.execute(lcmMediaId).get(SQLMediaGetSproc.MEDIA_SET);
        if (lcmMediaList.isEmpty()) {
            lcmMediaId = null;
        } else {
            domainId = lcmMediaList.get(0).getDomainId().toString();
        }
        return lcmMediaId == null ? null : resolveMedia(lcmMediaId, domainId, new HashMap<>());
    }

    /**
     * Resolves a Media by it's MediaGUID
     *
     * @param mediaGUID MediaGuid of a Media
     * @return a completed Media object that corresponds to the MediaGuid
     */
    private Media resolveMediaByGuid(String mediaGUID) {
        Integer lcmMediaId = null;
        String domainId = null;
        final Map<String, Media> mediaLcmMediaIdMap = new HashMap<>();
        final Media guidMedia = completeMedia(mediaRepo.getMedia(mediaGUID), NULL_FILTER);
        final boolean isLodgingWithGuid = guidMedia != null && Domain.LODGING.getDomain().equals(guidMedia.getDomain());
        if (isLodgingWithGuid) {
            if (guidMedia.getLcmMediaId() != null) {
                mediaLcmMediaIdMap.put(guidMedia.getLcmMediaId(), guidMedia);
                lcmMediaId = Integer.parseInt(guidMedia.getLcmMediaId());
            }
            domainId = guidMedia.getDomainId();
        }
        return lcmMediaId == null ? guidMedia : resolveMedia(lcmMediaId, domainId, mediaLcmMediaIdMap);
    }

    /**
     * Adds corresponding room data and merges LcmMedia and MediaDB data for a Media object
     *
     * @param lcmMediaId LcmMediaID for a Media
     * @param domainId DomainID of a Media
     * @param mediaLcmMediaIdMap Map of LcmMediaId to a Media
     * @return  completed Media Object
     */
    @SuppressWarnings("unchecked")
    private Media resolveMedia(Integer lcmMediaId, String domainId, Map<String, Media> mediaLcmMediaIdMap) {
        final Map<String, Object> roomResult = roomGetByMediaIdSproc.execute(lcmMediaId);
        final Map<Integer, List<LcmMediaRoom>> lcmRoomMediaMap = new HashMap<>();
        lcmRoomMediaMap.put(lcmMediaId, (List<LcmMediaRoom>) roomResult.get(SQLRoomGetByMediaIdSproc.ROOM_SET));
        return convertMedia(mediaLcmMediaIdMap, lcmRoomMediaMap).apply(buildLcmMedia(domainId, NULL_FILTER).apply(lcmMediaId));
    }


    /**
     * Get and Set the latest status for a Media.
     *
     * @param media Media to get the status for
     * @param isLodgingNoGuid Boolean to determine whether to get the Status from MediaDB or LCM
     */
    private void resolveMediaLatestStatus(Media media, Boolean isLodgingNoGuid) {
        if (media != null) {
            String status;
            if (isLodgingNoGuid) {
                final List<String> fileNames = new ArrayList<>();
                fileNames.add(media.getFileName());
                status = getLatestStatus(fileNames).get(media.getFileName());
            } else {
                status = getLatestStatus(media.getMediaGuid());
            }
            if (status != null) {
                media.setStatus(status);
            }
        }
    }

    /**
     * Returns a ResponseEntity if the pageSize and pageIndex are not both null or not null.
     * 
     * @param totalMediaCount Total number of items belonging to the domain.
     * @param pageSize Size of the page.
     * @param pageIndex Which page to fetch items for.
     * @return A ResponseEntity if the pageSize and pageIndex are not both null or not null.
     */
    private String validatePagination(Integer totalMediaCount, Integer pageSize, Integer pageIndex) {
        if ((pageSize != null && pageIndex == null) || (pageSize == null && pageIndex != null)) {
            return "pageSize and pageIndex are inclusive, either both fields can be null or not null";
        }
        if (pageSize < 1 || pageIndex < 1) {
            return "pageSize and pageIndex can only be positive integer values";
        }
        if (pageSize * pageIndex > totalMediaCount) {
            return "pageIndex is out of bounds";
        }
        return null;
    }

    /**
     * Returns a page of items from a stream. Some request ask not for all items, but for only a page's worth of items.
     * The generics aren't specified in the stream to allow different types to be paginated.
     * 
     * @param items Item stream.
     * @param pageSize Size of the page.
     * @param pageIndex Which page to fetch items for.
     * @return A stream of items belonging to the desired page.
     */
    @SuppressWarnings("rawtypes")
    private Stream paginateItems(Stream items, Integer pageSize, Integer pageIndex) {
        final int indexStart = pageSize * (pageIndex - 1);
        final int indexEnd = pageSize;
        return items.skip(indexStart)
                    .limit(indexEnd);
    }

    /**
     * builds a Map of FileNames to Latest Status. If a media has a guid the status is extracted from the MediaDB
     * otherwise the statuses are extracted from LCM
     *
     * @param mediaList list of Media
     * @param domainId Id of a Domain
     * @return Map of filenames to their latest statuses
     */
    private Map<String, String> buildFileNameStatusMap(List<Media> mediaList, String domainId) {
        final Map<String, String> fileStatus = new HashMap<>();
        final List<String> fileNamesLCM = new ArrayList<>();
        final List<Media> mediaDBMedia = new ArrayList<>();
        mediaList.stream().forEach(media -> {
            if (media.getMediaGuid() == null) {
                if (media.getFileName() != null && !fileNamesLCM.contains(media.getFileName())) {
                    fileNamesLCM.add(media.getFileName());
                }
            } else {
                mediaDBMedia.add(media);
            }
        });
        if (!fileNamesLCM.isEmpty()) {
            fileStatus.putAll(getStatusLoop(paramLimit, fileNamesLCM));
        }
        if (!mediaDBMedia.isEmpty()) {
            fileStatus.putAll(getMediaDBStatus(mediaDBMedia, domainId));
        }
        return fileStatus;
    }

    /**
     * Get the Latest Status of a list of Medias from the MediaDB
     * From the list of medias a list of MediaGuids is produced which then a query is done on the MediaProcessLog table
     *
     * @param medias List of Medias
     * @param domainId the domainId of the Medias
     * @return a Map of fileNames to their Latest Status
     */
    private Map<String, String> getMediaDBStatus(List<Media> medias, String domainId) {
        final List<String> mediaGuids = medias.stream().map(Media::getMediaGuid).collect(Collectors.toList());
        final Map<String, String> filenameStatusMap = new HashMap<>();
        final Map<String, List<MediaProcessLog>> processLogMap = mediaRepo.getProcessLogByDomainId(domainId, mediaGuids)
                .stream().collect(Collectors.groupingBy(MediaProcessLog::getMediaGuid));

        medias.stream().forEach(media -> {
            List<MediaProcessLog> sortedLog = processLogMap.get(media.getMediaGuid());
            if (sortedLog != null && sortedLog.size() > 1) {
                sortedLog = processLogMap.get(media.getMediaGuid()).stream().sorted((log1, log2) -> log1.getStatusDate().compareTo(log2.getStatusDate()))
                        .collect(Collectors.toList());
            }
            if (sortedLog != null && sortedLog.size() > 0) {
                filenameStatusMap.put(media.getFileName(), getLatestActivityMapping(sortedLog));
            }
        });

        return filenameStatusMap;
    }


    /**
     * because of the parameter length limitation in Sproc 'MediaProcessLogGetByFilename', if the fileNames length is bigger than the limitation
     * we call the sproc multiple times to get the result.
     *
     * @param limit Sproc parameter limitation.
     * @param fileNames File names to fetch the status for.
     * @return List of all status mapped to their file name.
     */
    private Map<String, String> getStatusLoop(int limit, List<String> fileNames) {
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
     * Pulls the latest processing status of media files. When a file doesn't have any process logs the file is
     * considered old and therefore published.
     *
     * @param fileNames File names for which the status is required.
     * @return The latest status of a media file.
     */
    @Override
    public Map<String, String> getLatestStatus(List<String> fileNames) {
        List<MediaProcessLog> logs = processLogDao.findMediaStatus(fileNames);
        logs = logs == null ? new ArrayList<>() : logs;
        final Map<String, List<MediaProcessLog>> fileNameLogListMap = new HashMap<>();
        JSONUtil.divideStatusListToMap(logs, fileNameLogListMap, fileNames.size());

        return fileNames.stream().distinct().collect(Collectors.toMap(String::toString, fileName -> {
            final List<MediaProcessLog> logList = fileNameLogListMap.get(fileName);
            return getLatestActivityMapping(logList);
        }));
    }

    /**
     * Get Latest Status of a Media by guid
     *
     * @param mediaGuid GUID identifier for a Media
     * @return The latest status of a media file.
     */
    public String getLatestStatus(String mediaGuid) {
        final List<MediaProcessLog> processLogList = mediaRepo.getMediaProcessLogByMediaGUID(mediaGuid).stream()
                .sorted((log1, log2) -> log1.getStatusDate().compareTo(log2.getStatusDate()))
                .collect(Collectors.toList());
        return getLatestActivityMapping(processLogList);
    }


    /**
     * Finds the latest activity that is part of the activity white list. The list is expected to
     * be ordered from oldest to latest.
     * If the latest status is REJECTED or DUPLICATE a check is done to see if the Media has been PUBLISHED previously
     *
     * @param logList The list to search.
     * @return The found activity mapping. PUBLISHED if no activity is found, for the media most be old.
     */
    private String getLatestActivityMapping(List<MediaProcessLog> logList) {
        if (logList != null) {
            for (int i = logList.size() - 1; i >= 0; i--) {
                final MediaProcessLog mediaProcessLog = logList.get(i);
                final ActivityMapping activityMapping = JSONUtil.getActivityMappingFromList(activityWhiteList, mediaProcessLog.getActivityType(), mediaProcessLog.getMediaType());
                if (activityMapping != null) {
                    return REJECTED_ACTIVITY.equals(activityMapping.getStatusMessage()) || DUPLICATE_ACTIVITY.equals(activityMapping.getStatusMessage()) ?
                            checkPreviouslyPublished(logList, activityMapping.getStatusMessage()) : activityMapping.getStatusMessage();
                }
            }
        }
        return PUBLISHED_ACTIVITY;
    }

    /**
     * checks if a Published Log exists in the list of logs
     * If the size of the list is too small a confident guess can be made that the LatestStatus is the real status
     * Else check that there is a Published log
     *
     * @param logs list of mediaProcessLog
     * @param latestStatus The latest status of a media from the DB
     * @return the Status of the original Media
     */
    private String checkPreviouslyPublished(List<MediaProcessLog> logs, String latestStatus) {
        if (logs.size() < ACTIVITY_COUNT_THRESHOLD) {
            return latestStatus;
        } else {
            final Predicate<MediaProcessLog> publishedCheck = mediaProcessLog -> {
                ActivityMapping activity = JSONUtil.getActivityMappingFromList(activityWhiteList, mediaProcessLog.getActivityType(), mediaProcessLog.getMediaType());
                return activity != null && PUBLISHED_ACTIVITY.equals(activity.getStatusMessage());
            };
            return logs.stream().filter(publishedCheck).findFirst().isPresent() ? PUBLISHED_ACTIVITY : latestStatus;
        }
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
            final Boolean media1Hero = isPropertyHero(media1);
            final Boolean media2Hero = isPropertyHero(media2);
            final int compareHero = media2Hero.compareTo(media1Hero);
            if (compareHero != 0) {
                return compareHero;
            }
            final int commpareSubId = getSubId(media1).compareTo(getSubId(media2));
            if (commpareSubId != 0) {
                return commpareSubId;
            }
        }
        return 0;
    }

    private String getSubId(Media media) {
        if (media.getDomainData() != null) {
            final String subId = (String) media.getDomainData().get("subcategoryId");
            if (subId != null) {
                return subId;
            }
        }
        return "";
    }
    
    /**
     * Verifies if the media is the property hero image. Should only be done for LODGING images.
     * @param media The media to verify.
     * @return {@code true} if the image is a property hero image, {@code false} otherwise;
     */
    private Boolean isPropertyHero(Media media) {
        if (media.getDomainData() != null) {
            final Object propertyHeroValue = media.getDomainData().get("propertyHero");
            if (propertyHeroValue instanceof Boolean) {
                return (Boolean) propertyHeroValue;
            } else {
                return Boolean.valueOf((String) media.getDomainData().get("propertyHero"));
            }
        }
        return false;
    }

    /**
     * From a domain id builds a media item from LCM data. Allows an inclusive filter for derivatives.
     *
     * @param domainId Id of the domain object for which the media is required.
     * @param derivativeFilter Inclusive filter of derivatives. A null or empty string will not exclude any derivatives.
     * @return
     */
    @SuppressWarnings("unchecked")
    private Function<Integer, LcmMedia> buildLcmMedia(final String domainId, final String derivativeFilter) {
        /* @formatter:off */
        return mediaId -> {
            final boolean skipDerivativeFiltering = derivativeFilter == null || derivativeFilter.isEmpty();
            final Map<String, Object> mediaResult = lcmMediaItemSproc.execute(Integer.parseInt(domainId), mediaId);
            final LcmMedia media = ((List<LcmMedia>) mediaResult.get(SQLMediaItemGetSproc.MEDIA_SET)).get(0);
            media.setDerivatives(((List<LcmMediaDerivative>) mediaResult.get(SQLMediaItemGetSproc.MEDIA_DERIVATIVES_SET)).stream()
                    .filter(derivative -> (skipDerivativeFiltering || derivativeFilter.contains(derivative.getMediaSizeType())))
                    .collect(Collectors.toList()));
            return media;
        };
        /* @formatter:on */
    }

    /**
     * Completes values of the media object. Mostly converts JSON string fields and populates the related list or map fields.
     * 
     * @param media The media to update and complete. This object is modified and returned.
     * @param derivativeFilter Inclusive filter of derivatives. A null or empty string will not exclude any derivatives.
     * @return the updated passed media object.
     */
    @SuppressWarnings("PMD.AvoidCatchingNPE")
    private Media completeMedia(final Media media, final String derivativeFilter) {
        if (media != null) {
            if (!"null".equals(media.getDomainFields()) && media.getDomainFields() != null) {
                try {
                    media.setDomainData(OBJECT_MAPPER.readValue(media.getDomainFields(), new TypeReference<Map<String, Object>>() {}));
                    final Object lcmMediaIdObject = media.getDomainData().get("lcmMediaId");
                    if (lcmMediaIdObject != null) {
                        final Integer lcmMediaId = lcmMediaIdObject instanceof Integer ? (Integer) lcmMediaIdObject : Integer.parseInt((String) lcmMediaIdObject);
                        media.setLcmMediaId(lcmMediaId.toString());
                    }
                } catch (IOException | NullPointerException e) {
                    LOGGER.warn("Domain fields not stored in proper JSON format for media id {}.", media.getMediaGuid(), e);
                }
            }
            if (media.getDerivatives() != null) {
                try {
                    final boolean skipDerivativeFiltering = derivativeFilter == null || derivativeFilter.isEmpty();
                    final CollectionType collectionType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, Map.class);
                    final List<Map<String, Object>> mediaDerivatives = OBJECT_MAPPER.readValue(media.getDerivatives(), collectionType);
                    media.setDerivativesList(mediaDerivatives.stream()
                            .filter(derivative -> (skipDerivativeFiltering || derivativeFilter.contains((String) derivative.get("type"))))
                            .collect(Collectors.toList()));
                } catch (IOException e) {
                    LOGGER.warn("Derivatives not stored in proper JSON format for media id {}.", media.getMediaGuid(), e);
                }
            }
        }
        return media;
    }

    /**
     * Converts a list of LcmLcmMediaAndDerivative belonging to on LCM media id to an LcmMedia instance.
     *
     * @param lcmMediaMap The map containing all of the LcmLcmMediaAndDerivative lists.
     * @param mediaId The id of media to convert.
     * @return The converted LcmMedia.
     */
    private LcmMedia convertLcmMediaAndDerivativeToLcmMedia(final Map<Integer, List<LcmMediaAndDerivative>> lcmMediaMap, final Integer mediaId, String derivativeFilter) {
        final List<LcmMediaAndDerivative> mediaList = lcmMediaMap.get(mediaId);
        final LcmMediaAndDerivative firstMediaItem = mediaList.get(0);
        final boolean skipDerivativeFiltering = derivativeFilter == null || derivativeFilter.isEmpty();
        /* @formatter:off */
        final List<LcmMediaDerivative> derivatives = mediaList.stream()
                .map(mediaItem -> LcmMediaDerivative.builder()
                        .mediaId(mediaItem.getMediaId())
                        .fileName(mediaItem.getDerivativeFileName())
                        .fileProcessed(mediaItem.getFileProcessed())
                        .mediaSizeTypeId(mediaItem.getDerivativeSizeTypeId())
                        .width(mediaItem.getDerivativeWidth())
                        .height(mediaItem.getDerivativeHeight())
                        .fileSize(mediaItem.getDerivativeFileSize())
                        .build())
                .filter(derivative -> (skipDerivativeFiltering || derivativeFilter.contains(derivative.getMediaSizeType())))
                .collect(Collectors.toList());

        final Date lastUpdateDate = firstMediaItem.getMediaLastUpdateDate().after(firstMediaItem.getLastUpdateDate()) ?
                firstMediaItem.getMediaLastUpdateDate() : firstMediaItem.getLastUpdateDate();
        final String lastUpdatedBy = firstMediaItem.getMediaLastUpdateDate().after(firstMediaItem.getLastUpdateDate()) ?
                firstMediaItem.getMediaLastUpdatedBy() : firstMediaItem.getLastUpdatedBy();

        return LcmMedia.builder()
                .mediaId(firstMediaItem.getMediaId())
                .provider(firstMediaItem.getProvider())
                .active(firstMediaItem.getActive())
                .fileName(firstMediaItem.getFileName())
                .width(firstMediaItem.getWidth())
                .height(firstMediaItem.getHeight())
                .lastUpdatedBy(lastUpdatedBy)
                .fileSize(firstMediaItem.getFileSize())
                .lastUpdateDate(lastUpdateDate)
                .category(firstMediaItem.getCategory())
                .comment(firstMediaItem.getComment())
                .formatId(firstMediaItem.getFormatId())
                .derivatives(derivatives)
                .build();
        /* @formatter:off */
    }

    /**
     * Function that converts LCM media item to a media item to return.
     *
     * @param mediaEidMap A map of media DB items that have an EID.
     * @param lcmRoomMediaMap Map of rooms related to LCM media id.
     * @return The converted LCM media.
     */
    private Function<LcmMedia, Media> convertMedia(final Map<String, Media> mediaEidMap, final Map<Integer, List<LcmMediaRoom>> lcmRoomMediaMap) {
        return lcmMedia -> {
            final Media dynamoMedia = mediaEidMap.get(lcmMedia.getMediaId().toString());
            /* @formatter:off */
            final Media.MediaBuilder mediaBuilder = Media.builder()
                    .active(lcmMedia.getActive().toString())
                    .derivativesList(extractDerivatives(lcmMedia))
                    .domainData(extractDomainFields(lcmMedia, dynamoMedia, lcmRoomMediaMap))
                    .fileName(lcmMedia.getFileName())
                    .fileSize(lcmMedia.getFileSize() * KB_TO_BYTES_CONVERTER)
                    .width(lcmMedia.getWidth())
                    .height(lcmMedia.getHeight())
                    .lastUpdated(lcmMedia.getLastUpdateDate())
                    .lcmMediaId(lcmMedia.getMediaId().toString())
                    .userId(lcmMedia.getLastUpdatedBy())
                    .commentList(extractCommentList(lcmMedia))
                    .domainDerivativeCategory(FORMAT_ID_2.equals(lcmMedia.getFormatId()) ? LODGING_VIRTUAL_TOUR_DERIVATIVE_TYPE : null);

            if (dynamoMedia == null) {
                mediaBuilder.domain(Domain.LODGING.getDomain())
                        .domainId(String.valueOf(lcmMedia.getDomainId()))
                        .provider(lcmMedia.getProvider() == null ? null
                                : providerProperties.getProperty(lcmMedia.getProvider().toString()));
            } else {
                mediaBuilder.fileUrl(dynamoMedia.getFileUrl())
                        .clientId(dynamoMedia.getClientId())
                        .providedName(dynamoMedia.getProvidedName())
                        .domain(dynamoMedia.getDomain())
                        .domainId(dynamoMedia.getDomainId())
                        .mediaGuid(dynamoMedia.getMediaGuid())
                        .provider(dynamoMedia.getProvider())
                        .sourceUrl(dynamoMedia.getSourceUrl());
            }

            return mediaBuilder.build();
            /* @formatter:on */
        };
    }

    /**
     * Builds a comment list from the LCM media comment. There is only one comment in LCM, but the response expects a list.
     *
     * @param lcmMedia Data object containing media data from LCM.
     * @return The comments of a media file.
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
     * Extract domain data to be added to a media data response.
     *
     * @param lcmMedia Data object containing media data from LCM.
     * @param dynamoMedia Data object containing media data form the Media DB.
     * @param lcmRoomMediaMap Map of rooms related to LCM media id.
     * @return A map of domain data.
     */
    private Map<String, Object> extractDomainFields(final LcmMedia lcmMedia, final Media dynamoMedia,
                                                    final Map<Integer, List<LcmMediaRoom>> lcmRoomMediaMap) {
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
        extractLcmRooms(lcmMedia, lcmDomainData, lcmRoomMediaMap);
        return lcmDomainData.isEmpty() ? domainData : lcmDomainData;
    }

    /**
     * Extract LCM domain data and stores into a passed map to be interpreted the same way as if it would have been pulled in JSON from
     * the media DB.
     *
     * @param lcmMedia Data object containing media data from LCM.
     * @param dynamoMedia Data object containing media data form the Media DB.
     * @param lcmDomainData Map to store extracted LCM data into.
     */
    private void extractLcmDomainFields(final LcmMedia lcmMedia, final Media dynamoMedia, final Map<String, Object> lcmDomainData) {
        if (lcmMedia.getCategory() != null) {
            if (lcmMedia.getCategory().equals(LCM_HERO_CATEGORY)) {
                lcmDomainData.put(FIELD_PROPERTY_HERO, "true");
                if (dynamoMedia != null && dynamoMedia.getDomainData() != null && dynamoMedia.getDomainData().get(FIELD_SUBCATEGORY_ID) != null) {
                    lcmDomainData.put(FIELD_SUBCATEGORY_ID, String.valueOf(dynamoMedia.getDomainData().get(FIELD_SUBCATEGORY_ID)));
                }
            } else {
                lcmDomainData.put(FIELD_SUBCATEGORY_ID, lcmMedia.getCategory().toString());
            }
        }
    }

    /**
     * Extract LCM domain room data and stores into a passed map to be interpreted the same way as if it would have been pulled in
     * JSON from the media DB.
     *
     * @param lcmMedia Data object containing media data from LCM.
     * @param lcmDomainData Map to store extracted LCM data into.
     * @param lcmRoomMediaMap Map of rooms related to LCM media id.
     */
    @SuppressWarnings("unchecked")
    private void extractLcmRooms(final LcmMedia lcmMedia, final Map<String, Object> lcmDomainData,
                                 final Map<Integer, List<LcmMediaRoom>> lcmRoomMediaMap) {
        final List<LcmMediaRoom> lcmMediaRoomList = lcmRoomMediaMap.get(lcmMedia.getMediaId());
        if (lcmMediaRoomList != null && !lcmMediaRoomList.isEmpty()) {
            final List<Map<String, String>> roomList = lcmMediaRoomList.stream().map(room -> {
                Map<String, String> roomData = new HashMap<>();
                roomData.put(FIELD_ROOM_ID, String.valueOf(room.getRoomId()));
                roomData.put(FIELD_ROOM_HERO, room.getRoomHero().toString());
                return roomData;
            }).collect(Collectors.toList());
            lcmDomainData.put(FIELD_ROOMS, roomList);
        }
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
                        derivativeData.put(FIELD_DERIVATIVE_TYPE, derivative.getMediaSizeType());
                        derivativeData.put(FIELD_DERIVATIVE_WIDTH, derivative.getWidth());
                        derivativeData.put(FIELD_DERIVATIVE_HEIGHT, derivative.getHeight());
                        derivativeData.put(FIELD_DERIVATIVE_FILE_SIZE, derivative.getFileSize() * KB_TO_BYTES_CONVERTER);
                        return derivativeData;
                    }).collect(Collectors.toList());
        }
        return derivatives;
    }

    @SuppressWarnings("unchecked")
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
     * Transforms a media for a media get response format.
     *
     * @param media The media to transform
     * @return The media response with the transformed media.
     */
    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "CPD-START"})
    private static MediaGetResponse transformSingleMediaForResponse(Media media) {
        if (media != null) {
        /* @formatter:off */
            setResponseLcmMediaId(media);
            return MediaGetResponse.builder()
                    .mediaGuid(media.getMediaGuid())
                    .fileUrl(media.getFileUrl())
                    .fileName(FileNameUtil.resolveFileNameToDisplay(media))
                    .active(media.getActive())
                    .width(media.getWidth())
                    .height(media.getHeight())
                    .fileSize(media.getFileSize())
                    .status(media.getStatus())
                    .lastUpdatedBy(StringUtils.isNullOrEmpty(media.getUserId()) ? media.getClientId() : media.getUserId())
                    .lastUpdateDateTime(DATE_FORMATTER.print(media.getLastUpdated().getTime()))
                    .domain(media.getDomain())
                    .domainId(media.getDomainId())
                    .domainProvider(media.getProvider())
                    .domainFields(media.getDomainData())
                    .derivatives(media.getDerivativesList())
                    .domainDerivativeCategory(media.getDomainDerivativeCategory())
                    .comments((media.getCommentList() == null) ? null : media.getCommentList().stream()
                            .map(comment -> Comment.builder().note(comment)
                                    .timestamp(DATE_FORMATTER.print(media.getLastUpdated().getTime())).build())
                            .collect(Collectors.toList()))
                    .build();
        /* @formatter:on */
        }
        return null;
    }

    /**
     * Sets the LCM media id in the media object. The LCM id is put as a field of the domain data since it's
     * expected there in the response JSON payload.
     *
     * @param media The media object to update.
     */
    private static void setResponseLcmMediaId(Media media) {
        if (media.getLcmMediaId() != null) {
            if (media.getDomainData() == null) {
                media.setDomainData(new HashMap<>());
            }
            media.getDomainData().put(RESPONSE_FIELD_LCM_MEDIA_ID, media.getLcmMediaId());
        }
    }
}
