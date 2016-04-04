package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.CatalogitemMediaDao;
import com.expedia.content.media.processing.services.dao.domain.LcmCatalogItemMedia;
import com.expedia.content.media.processing.services.dao.domain.Media;
import com.expedia.content.media.processing.services.dao.dynamo.DynamoMediaRepository;
import com.expedia.content.media.processing.services.dao.sql.CatalogItemListSproc;
import com.expedia.content.media.processing.services.dao.sql.CatalogItemMediaChgSproc;
import com.expedia.content.media.processing.services.dao.sql.CatalogItemMediaGetSproc;
import com.expedia.content.media.processing.services.dao.sql.MediaLstWithCatalogItemMediaAndMediaFileNameSproc;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CatelogHeroProcesserTest {

    //@Test
    public void test() throws Exception {
        String jsonMsg = "{  \n"
                + "   \"userId\":\"bobthegreat\",\n"
                + "   \"active\":\"true\",\n"
                + "    \"domain\":\"Lodging\",\n"
                + "   \"domainFields\":{  \n"
                + "      \"subcategoryId\":\"22003\",\n"
                + "      \"propertyHero\":\"true\",\n"
                + "      \"rooms\":[  \n"
                + "         {  \n"
                + "            \"roomId\":\"934779\",\n"
                + "            \"roomHero\":\"false\"\n"
                + "         },\n"
                + "         {\n"
                + "             \"roomId\":\"928675\",\n"
                + "            \"roomHero\":\"true\" \n"
                + "         }\n"
                + "      ]\n"
                + "   },\n"
                + "   \"comment\":\"note33\"\n"
                + "}";
        CatelogHeroProcesser catelogHeroProcesser = getCateLogMock();
        catelogHeroProcesser.setOldCategoryForHeroPropertyMedia(ImageMessage.parseJsonMessage(jsonMsg), "41098");
        catelogHeroProcesser.getCatalogItemMeida(41098, 19671339);
        LcmCatalogItemMedia lcmCatalogItemMedia =
                LcmCatalogItemMedia.builder().catalogItemId(41098).mediaId(19671339).mediaUseRank(3).lastUpdatedBy("test").lastUpdateDate(new Date())
                        .build();
        catelogHeroProcesser.setMediaToHero("tst", lcmCatalogItemMedia, true);
        catelogHeroProcesser.unSetOtherMediaHero(41098, "test", 19671339);
        catelogHeroProcesser.getCatalogItemMeida(41098,19671339);
    }

    private CatelogHeroProcesser getCateLogMock() throws Exception {
        CatelogHeroProcesser catelogHeroProcesser = new CatelogHeroProcesser();
        CatalogItemListSproc catalogItemListSproc = mock(CatalogItemListSproc.class);
        CatalogitemMediaDao catalogitemMediaDao = mock(CatalogitemMediaDao.class);
        CatalogItemMediaChgSproc catalogItemMediaChgSproc = mock(CatalogItemMediaChgSproc.class);

        List<LcmCatalogItemMedia> lcmCatalogItemMediaList = new ArrayList<>();
        LcmCatalogItemMedia lcmCatalogItemMedia =
                LcmCatalogItemMedia.builder().catalogItemId(41098).mediaId(19671339).mediaUseRank(3).lastUpdatedBy("test").lastUpdateDate(new Date())
                        .build();
        lcmCatalogItemMediaList.add(lcmCatalogItemMedia);
        Map<String, Object> lcmCatMap = new HashMap<>();
        lcmCatMap.put(CatalogItemListSproc.MEDIA_SET, lcmCatalogItemMediaList);
        when(catalogItemListSproc.execute(anyInt())).thenReturn(lcmCatMap);

        FieldUtils.writeField(catelogHeroProcesser, "catalogItemListSproc", catalogItemListSproc, true);
        Mockito.doNothing().when(catalogItemMediaChgSproc).updateCategory(anyInt(), anyInt(), anyInt(), anyString(), anyString());
        FieldUtils.writeField(catelogHeroProcesser, "catalogItemMediaChgSproc", catalogItemMediaChgSproc, true);
        Mockito.doNothing().when(catalogitemMediaDao).updateCatalogItem(any(), anyInt(), anyInt());
        FieldUtils.writeField(catelogHeroProcesser, "catalogitemMediaDao", catalogitemMediaDao, true);

        List<Media> heroMedia = new ArrayList<>();
        DynamoMediaRepository mediaRepo = mock(DynamoMediaRepository.class);
        Media media =
                Media.builder().lcmMediaId("19671338").mediaGuid("testGuid").domainId("41098").lastUpdated(new Date(new Date().getTime() - 10000)).build();
        heroMedia.add(media);
        when(mediaRepo.retrieveHeroPropertyMedia(anyString(), anyString())).thenReturn(heroMedia);
        FieldUtils.writeField(catelogHeroProcesser, "mediaRepo", mediaRepo, true);

        CatalogItemMediaGetSproc catalogItemMediaGetSproc = mock(CatalogItemMediaGetSproc.class);
        when(catalogItemMediaGetSproc.getMedia(anyInt(), anyInt())).thenReturn(lcmCatalogItemMediaList);
        FieldUtils.writeField(catelogHeroProcesser, "catalogItemMediaGetSproc", catalogItemMediaGetSproc, true);

        MediaLstWithCatalogItemMediaAndMediaFileNameSproc mediaLstWithCatalogItemMediaAndMediaFileNameSproc =
                mock(MediaLstWithCatalogItemMediaAndMediaFileNameSproc.class);
        when(mediaLstWithCatalogItemMediaAndMediaFileNameSproc.getMedia(anyInt())).thenReturn(lcmCatalogItemMediaList);
        FieldUtils.writeField(catelogHeroProcesser, "mediaLstWithCatalogItemMediaAndMediaFileNameSproc", mediaLstWithCatalogItemMediaAndMediaFileNameSproc,
                true);

        return catelogHeroProcesser;
    }
}
