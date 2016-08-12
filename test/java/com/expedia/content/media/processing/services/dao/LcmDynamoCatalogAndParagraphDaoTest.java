package com.expedia.content.media.processing.services.dao;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaRoom;
import com.expedia.content.media.processing.services.dao.domain.Paragraph;
import com.expedia.content.media.processing.services.dao.sql.AddCatalogItemMediaForRoomsAndRatePlansSproc;
import com.expedia.content.media.processing.services.dao.sql.AddParagraphSproc;
import com.expedia.content.media.processing.services.dao.sql.CatalogItemMediaChgSproc;
import com.expedia.content.media.processing.services.dao.sql.CatalogItemMediaDelSproc;
import com.expedia.content.media.processing.services.dao.sql.GetParagraphSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaItemGetSproc;
import com.expedia.content.media.processing.services.dao.sql.SQLRoomGetByMediaIdSproc;
import com.expedia.content.media.processing.services.dao.sql.SetParagraphSproc;

public class LcmDynamoCatalogAndParagraphDaoTest {

    private CatalogItemMediaChgSproc catalogItemMediaChgSproc;
    private AddCatalogItemMediaForRoomsAndRatePlansSproc addCatalogItemMediaForRoom;
    private SQLRoomGetByMediaIdSproc roomGetSproc;
    private CatalogItemMediaDelSproc catalogItemMediaDelSproc;
    private AddParagraphSproc addParagraphSproc;
    private GetParagraphSproc getParagraphSproc;
    private SetParagraphSproc setParagraphSproc;
    private SQLMediaItemGetSproc lcmMediaItemSproc;
    private ImageMessage imageMessage;

    private LcmDynamoCatalogAndParagraphDao lcmDynamoCatalogAndParagraphDao = null;

    @Before
    public void setUp() throws Exception {
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

        imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        roomGetSproc = mock(SQLRoomGetByMediaIdSproc.class);
        Map<String, Object> roomResult = new HashMap<>();
        LcmMediaRoom lcmMediaRoom = LcmMediaRoom.builder().roomId(123).roomHero(true).build();
        List<LcmMediaRoom> lcmMediaRoomList = new ArrayList<>();
        lcmMediaRoomList.add(lcmMediaRoom);
        roomResult.put("room", lcmMediaRoomList);
        when(roomGetSproc.execute(anyInt())).thenReturn(roomResult);
        catalogItemMediaChgSproc = mock(CatalogItemMediaChgSproc.class);
        Mockito.doNothing().when(catalogItemMediaChgSproc).updateCategory(anyInt(), anyInt(), anyInt(), anyString(), anyString());

        addCatalogItemMediaForRoom = mock(AddCatalogItemMediaForRoomsAndRatePlansSproc.class);
        Mockito.doNothing().when(addCatalogItemMediaForRoom).addCatalogItemMedia(anyInt(), anyInt(), anyInt(), anyBoolean(), anyInt(), anyString(),
                anyBoolean(), anyBoolean(), anyString(), any(ImageMessage.class));

        catalogItemMediaDelSproc = mock(CatalogItemMediaDelSproc.class);
        Mockito.doNothing().when(catalogItemMediaDelSproc).deleteCategory(anyInt(), anyInt());

        addParagraphSproc = mock(AddParagraphSproc.class);
        Mockito.doNothing().when(addParagraphSproc).addParagraph(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyString(), anyObject(), anyObject(),
                anyInt(), anyInt(), anyInt(), anyString(), anyString(), anyInt());

        getParagraphSproc = mock(GetParagraphSproc.class);
        List<Paragraph> paragraphs = new ArrayList<>();
        Paragraph paragraph = Paragraph.builder().contentSourceTypeId("tsrt").build();
        paragraphs.add(paragraph);
        when(getParagraphSproc.getParagraph(anyInt())).thenReturn(paragraphs);

        setParagraphSproc = mock(SetParagraphSproc.class);
        Mockito.doNothing().when(setParagraphSproc).setParagraph(anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyString(), anyObject(), anyObject(),
                anyInt(), anyInt(), anyInt(), anyString(), anyString(), anyInt());

        lcmMediaItemSproc = mock(SQLMediaItemGetSproc.class);
        Map<String, Object> mediaResult = new HashMap<>();
        List<LcmMedia> mediaResultList = new ArrayList<>();
        LcmMedia media = LcmMedia.builder().category(22003).build();
        mediaResultList.add(media);
        mediaResult.put(SQLMediaItemGetSproc.MEDIA_SET, mediaResultList);
        when(lcmMediaItemSproc.execute(anyInt(), anyInt())).thenReturn(mediaResult);

        lcmDynamoCatalogAndParagraphDao = makeMockMediaDao();
    }

    private LcmDynamoCatalogAndParagraphDao makeMockMediaDao() throws NoSuchFieldException, IllegalAccessException {
        LcmDynamoCatalogAndParagraphDao lcmDynamoCatalogAndParagraphDao = new LcmDynamoCatalogAndParagraphDao();
        setFieldValue(lcmDynamoCatalogAndParagraphDao, "roomGetSproc", roomGetSproc);
        setFieldValue(lcmDynamoCatalogAndParagraphDao, "catalogItemMediaChgSproc", catalogItemMediaChgSproc);
        setFieldValue(lcmDynamoCatalogAndParagraphDao, "addCatalogItemMediaForRoom", addCatalogItemMediaForRoom);
        setFieldValue(lcmDynamoCatalogAndParagraphDao, "catalogItemMediaDelSproc", catalogItemMediaDelSproc);
        setFieldValue(lcmDynamoCatalogAndParagraphDao, "addParagraphSproc", addParagraphSproc);
        setFieldValue(lcmDynamoCatalogAndParagraphDao, "getParagraphSproc", getParagraphSproc);
        setFieldValue(lcmDynamoCatalogAndParagraphDao, "setParagraphSproc", setParagraphSproc);
        setFieldValue(lcmDynamoCatalogAndParagraphDao, "lcmMediaItemSproc", lcmMediaItemSproc);
        return lcmDynamoCatalogAndParagraphDao;
    }

    @Test
    public void updateCatalogItem() {
        lcmDynamoCatalogAndParagraphDao.updateCatalogItem(imageMessage, 934779, 41098);
    }

    @Test
    public void deleteCatalogItem() {
        lcmDynamoCatalogAndParagraphDao.deleteCatalogItem(934779, 41098);

    }

    @Test
    public void addCatalogItemForRoom() {
        lcmDynamoCatalogAndParagraphDao.addCatalogItemForRoom(928675, 19671339, 41098, imageMessage);

    }

    @Test
    public void getLcmRoomsByMediaId() {
        List<LcmMediaRoom> lcmMediaRoomList = lcmDynamoCatalogAndParagraphDao.getLcmRoomsByMediaId(19671339);
        assertTrue(lcmMediaRoomList.get(0).getRoomId() == 123);
    }

    @Test
    public void deleteParagraph() {
        lcmDynamoCatalogAndParagraphDao.deleteParagraph(123);
    }

    @Test
    public void addOrUpdateParagraph() {
        lcmDynamoCatalogAndParagraphDao.addOrUpdateParagraph(123, 19671339);

    }

}
