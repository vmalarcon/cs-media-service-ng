package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.dao.MediaDomainCategoriesDao;
import com.expedia.content.media.processing.services.dao.MediaProvider;
import com.expedia.content.media.processing.services.dao.MediaProviderDao;
import com.expedia.content.media.processing.services.dao.MediaProviderSproc;
import com.expedia.content.media.processing.services.dao.MediaSubCategory;
import com.expedia.content.media.processing.services.dao.PropertyRoomTypeGetIDSproc;
import com.expedia.content.media.processing.services.dao.RoomType;
import com.expedia.content.media.processing.services.dao.RoomTypeDao;
import com.expedia.content.media.processing.services.dao.SKUGroupCatalogItemDao;
import com.expedia.content.media.processing.services.dao.SQLMediaDomainCategoriesSproc;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ContextConfiguration(locations = "classpath:media-services.xml")
@RunWith(MockitoJUnitRunner.class)
public class LCMValidatorTest {

    @Mock
    SKUGroupCatalogItemDao mockSKUGroupCatalogItemDao;

    @Mock
    MediaProviderSproc mockMediaProviderSproc;

    @Mock
    SQLMediaDomainCategoriesSproc mockSQLMediaDomainCategoriesSproc;

    @Mock
    PropertyRoomTypeGetIDSproc mockPropertyRoomTypeGetIDSproc;

    LCMValidator lcmValidator;
    Map<String, Object> mediaProviderMockResults;
    Map<String, Object> catMockResults;
    final String LOCALID = "1033";
    List<MediaSubCategory> mockMediaSubCategories;
    MediaProviderDao mockMediaProviderDao;
    MediaDomainCategoriesDao mockMediaDomainCategoriesDao;
    RoomTypeDao roomTypeDao;
    List<RoomType> mockRoomTypes;
    Map<String, Object> mockRoomResults = new HashMap<>();

    @BeforeClass
    public static void setUp() {
        System.setProperty("EXPEDIA_ENVIRONMENT", "test");
        System.setProperty("AWS_REGION", "us-west-2");
    }

    @Before
    public void initialize() throws NoSuchFieldException, IllegalAccessException {

        lcmValidator = new LCMValidator();
        List<MediaProvider> mediaProviders = new ArrayList<>();
        MediaProvider mediaProvider = new MediaProvider(1, "EPC Internal User", new Timestamp(1339150200000L),
                "phoenix", null);
        mediaProviders.add(mediaProvider);
        mediaProviderMockResults = new HashMap<>();
        catMockResults = new HashMap<>();
        mediaProviderMockResults.put(MediaProviderSproc.MEDIA_PROVIDER_MAPPER_RESULT_SET, mediaProviders);
        mockMediaSubCategories = new ArrayList<>();
        mockMediaSubCategories.add(new MediaSubCategory("3", "3", "1033", "Featured Image"));
        mockMediaSubCategories.add(new MediaSubCategory("4", "10000", "1033", "Interior Entrance"));
        mockMediaSubCategories.add(new MediaSubCategory("4", "10001", "1033", "Lobby"));
        catMockResults.put(SQLMediaDomainCategoriesSproc.MEDIA_SUB_CATEGORY_RESULT_SET, mockMediaSubCategories);
        mockMediaProviderDao = spy(new MediaProviderDao(mockMediaProviderSproc));
        mockMediaDomainCategoriesDao = spy(new MediaDomainCategoriesDao(mockSQLMediaDomainCategoriesSproc));
        mockRoomTypes = new ArrayList<>();
        mockRoomTypes.add(new RoomType(222, 555, new Timestamp(1339150200000L), "phoenix", "phoenix"));
        mockRoomTypes.add(new RoomType(333, 444, new Timestamp(1339150200000L), "phoenix", "phoenix"));
        mockRoomResults = new HashMap<>();
        mockRoomResults.put(PropertyRoomTypeGetIDSproc.ROOM_TYPE_RESULT_SET, mockRoomTypes);
        roomTypeDao = spy(new RoomTypeDao(mockPropertyRoomTypeGetIDSproc));
        mockRoomTypes = new ArrayList<>();
        mockRoomTypes.add(new RoomType(222, 555, new Timestamp(1339150200000L), "phoenix", "phoenix"));
        mockRoomTypes.add(new RoomType(333, 444, new Timestamp(1339150200000L), "phoenix", "phoenix"));
        mockRoomResults = new HashMap<>();
        mockRoomResults.put(PropertyRoomTypeGetIDSproc.ROOM_TYPE_RESULT_SET, mockRoomTypes);
        ReflectionUtil.setFieldValue(lcmValidator, "skuGroupCatalogItemDao", mockSKUGroupCatalogItemDao);
        ReflectionUtil.setFieldValue(lcmValidator, "mediaProviderDao", mockMediaProviderDao);
        ReflectionUtil.setFieldValue(lcmValidator, "mediaDomainCategoriesDao", mockMediaDomainCategoriesDao);
        ReflectionUtil.setFieldValue(lcmValidator, "roomTypeDao", roomTypeDao);
    }

    @Test
    public void testLCMValidationSuccess() throws Exception {
        final String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello.jpg\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\", " +
                        "    \"domainProvider\": \"EPC Internal User\", " +
                        "    \"domainFields\": { " +
                        "          \"category\": \"3\"," +
                        "          \"propertyHero\": \"true\"," +
                        "          \"rooms\": [ " +
                        "               {" +
                        "                 \"roomId\": \"222\", " +
                        "                 \"roomHero\": \"true\" " +
                        "               }" +
                        "                     ]" +
                        "                       }" +
                        " }";
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        final List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        when(mockSKUGroupCatalogItemDao.skuGroupExists(anyInt())).thenReturn(Boolean.TRUE);
        when(mockMediaProviderSproc.execute()).thenReturn(mediaProviderMockResults);
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<Map<String, String>> errorList = lcmValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 0);
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockMediaProviderSproc, times(1)).execute();
        verify(mockMediaProviderDao, times(1)).mediaProviderExists("EPC Internal User");
        verify(mockMediaDomainCategoriesDao, times(1)).subCategoryIdExists(any(OuterDomain.class), eq("1033"));
        verify(mockPropertyRoomTypeGetIDSproc, times(1)).execute(any(OuterDomain.class));
    }

    @Test
    public void testDomainIdDoesNotExist() throws Exception {
        final String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello.jpg\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\", " +
                        "    \"domainProvider\": \"EPC Internal User\", " +
                        "    \"domainFields\": { " +
                        "          \"category\": \"3\"," +
                        "          \"propertyHero\": \"true\"," +
                        "          \"rooms\": [ " +
                        "               {" +
                        "                 \"roomId\": \"222\", " +
                        "                 \"roomHero\": \"true\" " +
                        "               }" +
                        "                     ]" +
                        "                       }" +
                        " }";
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        final List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        when(mockSKUGroupCatalogItemDao.skuGroupExists(anyInt())).thenReturn(Boolean.FALSE);
        when(mockMediaProviderSproc.execute()).thenReturn(mediaProviderMockResults);
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<Map<String, String>> errorList = lcmValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 1);
        assertTrue(errorList.get(0).get("error").equals("The domainId does not exist in LCM."));
        assertTrue(errorList.get(0).get("fileName").equals("Something"));
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockMediaProviderSproc, times(1)).execute();
        verify(mockMediaProviderDao, times(1)).mediaProviderExists("EPC Internal User");
        verify(mockMediaDomainCategoriesDao, times(1)).subCategoryIdExists(any(OuterDomain.class), eq("1033"));
        verify(mockPropertyRoomTypeGetIDSproc, times(1)).execute(any(OuterDomain.class));
    }

    @Test
    public void testMediaProviderDoesNotExist() throws Exception {
        final String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello.jpg\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\", " +
                        "    \"domainProvider\": \"does not exist\", " +
                        "    \"domainFields\": { " +
                        "          \"category\": \"3\"," +
                        "          \"propertyHero\": \"true\"," +
                        "          \"rooms\": [ " +
                        "               {" +
                        "                 \"roomId\": \"222\", " +
                        "                 \"roomHero\": \"true\" " +
                        "               }" +
                        "                     ]" +
                        "                       }" +
                        " }";
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        final List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        when(mockSKUGroupCatalogItemDao.skuGroupExists(anyInt())).thenReturn(Boolean.TRUE);
        when(mockMediaProviderSproc.execute()).thenReturn(mediaProviderMockResults);
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<Map<String, String>> errorList = lcmValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 1);
        assertTrue(errorList.get(0).get("error").equals("The mediaProvider does not exist in LCM."));
        assertTrue(errorList.get(0).get("fileName").equals("Something"));
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockMediaProviderSproc, times(1)).execute();
        verify(mockMediaProviderDao, times(1)).mediaProviderExists("does not exist");
        verify(mockMediaDomainCategoriesDao, times(1)).subCategoryIdExists(any(OuterDomain.class), eq("1033"));
        verify(mockPropertyRoomTypeGetIDSproc, times(1)).execute(any(OuterDomain.class));
    }

    @Test
    public void testMediaCategoryDoesNotExist() throws Exception {
        final String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello.jpg\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\", " +
                        "    \"domainProvider\": \"EPC Internal User\", " +
                        "    \"domainFields\": { " +
                        "          \"category\": \"66\"," +
                        "          \"propertyHero\": \"true\"," +
                        "          \"rooms\": [ " +
                        "               {" +
                        "                 \"roomId\": \"222\", " +
                        "                 \"roomHero\": \"true\" " +
                        "               }" +
                        "                     ]" +
                        "                       }" +
                        " }";
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        final List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        when(mockSKUGroupCatalogItemDao.skuGroupExists(anyInt())).thenReturn(Boolean.TRUE);
        when(mockMediaProviderSproc.execute()).thenReturn(mediaProviderMockResults);
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<Map<String, String>> errorList = lcmValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 1);
        assertTrue(errorList.get(0).get("error").equals("The category does not exist in LCM."));
        assertTrue(errorList.get(0).get("fileName").equals("Something"));
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockMediaProviderSproc, times(1)).execute();
        verify(mockMediaProviderDao, times(1)).mediaProviderExists("EPC Internal User");
        verify(mockMediaDomainCategoriesDao, times(1)).subCategoryIdExists(any(OuterDomain.class), eq("1033"));
        verify(mockPropertyRoomTypeGetIDSproc, times(1)).execute(any(OuterDomain.class));
    }

    @Test
    public void testNoDomainFields() throws Exception {
        final String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello.jpg\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\", " +
                        "    \"domainProvider\": \"EPC Internal User\" " +
                        " }";
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        final List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        when(mockSKUGroupCatalogItemDao.skuGroupExists(anyInt())).thenReturn(Boolean.TRUE);
        when(mockMediaProviderSproc.execute()).thenReturn(mediaProviderMockResults);
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<Map<String, String>> errorList = lcmValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 0);
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockMediaProviderSproc, times(1)).execute();
        verify(mockMediaProviderDao, times(1)).mediaProviderExists("EPC Internal User");
        verify(mockMediaDomainCategoriesDao, times(1)).subCategoryIdExists(any(OuterDomain.class), eq("1033"));
        verifyZeroInteractions(mockPropertyRoomTypeGetIDSproc);
    }

    @Test
    public void testNoCategory() throws Exception {
        final String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello.jpg\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\", " +
                        "    \"domainProvider\": \"EPC Internal User\", " +
                        "    \"domainFields\": { " +
                        "          \"not a category\": \"3\"," +
                        "          \"propertyHero\": \"true\"," +
                        "          \"rooms\": [ " +
                        "               {" +
                        "                 \"roomId\": \"222\", " +
                        "                 \"roomHero\": \"true\" " +
                        "               }" +
                        "                     ]" +
                        "                       }" +
                        " }";
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        final List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        when(mockSKUGroupCatalogItemDao.skuGroupExists(anyInt())).thenReturn(Boolean.TRUE);
        when(mockMediaProviderSproc.execute()).thenReturn(mediaProviderMockResults);
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<Map<String, String>> errorList = lcmValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 0);
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockMediaProviderSproc, times(1)).execute();
        verify(mockMediaProviderDao, times(1)).mediaProviderExists("EPC Internal User");
        verify(mockMediaDomainCategoriesDao, times(1)).subCategoryIdExists(any(OuterDomain.class), eq("1033"));
        verify(mockPropertyRoomTypeGetIDSproc, times(1)).execute(any(OuterDomain.class));
    }

    @Test
    public void testRoomDoesNotExist() throws Exception {
        final String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello.jpg\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\", " +
                        "    \"domainProvider\": \"EPC Internal User\", " +
                        "    \"domainFields\": { " +
                        "          \"category\": \"3\"," +
                        "          \"propertyHero\": \"true\"," +
                        "          \"rooms\": [ " +
                        "               {" +
                        "                 \"roomId\": \"5678\", " +
                        "                 \"roomHero\": \"true\" " +
                        "               }" +
                        "                     ]" +
                        "                       }" +
                        " }";
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        final List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        when(mockSKUGroupCatalogItemDao.skuGroupExists(anyInt())).thenReturn(Boolean.TRUE);
        when(mockMediaProviderSproc.execute()).thenReturn(mediaProviderMockResults);
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<Map<String, String>> errorList = lcmValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 1);
        assertTrue(errorList.get(0).get("error").equals("The room does not belong to the property in LCM."));
        assertTrue(errorList.get(0).get("fileName").equals("Something"));
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockMediaProviderSproc, times(1)).execute();
        verify(mockMediaProviderDao, times(1)).mediaProviderExists("EPC Internal User");
        verify(mockMediaDomainCategoriesDao, times(1)).subCategoryIdExists(any(OuterDomain.class), eq("1033"));
        verify(mockPropertyRoomTypeGetIDSproc, times(1)).execute(anyInt());
    }

    @Test
    public void testNoRooms() throws Exception {
        final String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello.jpg\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\", " +
                        "    \"domainProvider\": \"EPC Internal User\", " +
                        "    \"domainFields\": { " +
                        "          \"category\": \"3\"," +
                        "          \"propertyHero\": \"true\" " +
                        "                      }" +
                        " }";
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        final List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        when(mockSKUGroupCatalogItemDao.skuGroupExists(anyInt())).thenReturn(Boolean.TRUE);
        when(mockMediaProviderSproc.execute()).thenReturn(mediaProviderMockResults);
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<Map<String, String>> errorList = lcmValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 0);
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockMediaProviderSproc, times(1)).execute();
        verify(mockMediaProviderDao, times(1)).mediaProviderExists("EPC Internal User");
        verify(mockMediaDomainCategoriesDao, times(1)).subCategoryIdExists(any(OuterDomain.class), eq("1033"));
        verifyZeroInteractions(mockPropertyRoomTypeGetIDSproc);
    }
}
