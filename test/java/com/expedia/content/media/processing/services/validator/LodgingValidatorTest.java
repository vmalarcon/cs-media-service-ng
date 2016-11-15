package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.dao.MediaDomainCategoriesDao;
import com.expedia.content.media.processing.services.dao.PropertyRoomTypeGetIDSproc;
import com.expedia.content.media.processing.services.dao.RoomType;
import com.expedia.content.media.processing.services.dao.RoomTypeDao;
import com.expedia.content.media.processing.services.dao.SKUGroupCatalogItemDao;
import com.expedia.content.media.processing.services.dao.domain.MediaCategory;
import com.expedia.content.media.processing.services.dao.domain.MediaSubCategory;
import com.expedia.content.media.processing.services.dao.sql.SQLMediaDomainCategoriesSproc;
import org.codehaus.plexus.util.ReflectionUtils;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.assertEquals;
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
public class LodgingValidatorTest {

    @Mock
    SKUGroupCatalogItemDao mockSKUGroupCatalogItemDao;

    @Mock
    SQLMediaDomainCategoriesSproc mockSQLMediaDomainCategoriesSproc;

    @Mock
    PropertyRoomTypeGetIDSproc mockPropertyRoomTypeGetIDSproc;

    @Mock
    Properties mockProviderProperties;

    LodgingAddValidator lodgingAddValidator;
    LodgingValidator lodgingValidator;
    Map<String, Object> mediaProviderMockResults;
    Map<String, Object> catMockResults;
    final String LOCALID = "1033";
    List<MediaCategory> mockMediaCategories;
    List<MediaSubCategory> mockMediaSubCategories;
    MediaDomainCategoriesDao mockMediaDomainCategoriesDao;
    RoomTypeDao roomTypeDao;
    List<RoomType> mockRoomTypes;
    Map<String, Object> mockRoomResults = new HashMap<>();
    Set<Map.Entry<Object, Object>> providerMapping;

    @BeforeClass
    public static void setUp() {
        System.setProperty("EXPEDIA_ENVIRONMENT", "test");
        System.setProperty("AWS_REGION", "us-west-2");
    }

    @Before
    public void initialize() throws NoSuchFieldException, IllegalAccessException {

        lodgingAddValidator = new LodgingAddValidator();
        lodgingValidator = new LodgingValidator();
        mediaProviderMockResults = new HashMap<>();
        catMockResults = new HashMap<>();
        mockMediaCategories = new ArrayList<>();
        mockMediaCategories.add(new MediaCategory("3", "1033", "Primary Image"));
        mockMediaCategories.add(new MediaCategory("4", "1033", "Lobby"));
        mockMediaCategories.add(new MediaCategory("1", "1033", ""));
        mockMediaSubCategories = new ArrayList<>();
        mockMediaSubCategories.add(new MediaSubCategory("3", "3", "1033", "Featured Image"));
        mockMediaSubCategories.add(new MediaSubCategory("4", "10000", "1033", "Interior Entrance"));
        mockMediaSubCategories.add(new MediaSubCategory("4", "10001", "1033", "Lobby"));
        mockMediaSubCategories.add(new MediaSubCategory("1", "0", "1033", ""));
        catMockResults.put(SQLMediaDomainCategoriesSproc.MEDIA_SUB_CATEGORY_RESULT_SET, mockMediaSubCategories);
        catMockResults.put(SQLMediaDomainCategoriesSproc.MEDIA_CATEGORY_RESULT_SET, mockMediaCategories);
        mockMediaDomainCategoriesDao = spy(new MediaDomainCategoriesDao(mockSQLMediaDomainCategoriesSproc));
        mockRoomTypes = new ArrayList<>();
        mockRoomTypes.add(new RoomType(222, 555, new Timestamp(1339150200000L), "phoenix", "phoenix"));
        mockRoomTypes.add(new RoomType(333, 444, new Timestamp(1339150200000L), "phoenix", "phoenix"));
        mockRoomResults = new HashMap<>();
        mockRoomResults.put(PropertyRoomTypeGetIDSproc.ROOM_TYPE_RESULT_SET, mockRoomTypes);
        roomTypeDao = spy(new RoomTypeDao(mockPropertyRoomTypeGetIDSproc));
        ReflectionUtils.setVariableValueInObject(lodgingAddValidator, "providerProperties", mockProviderProperties);
        ReflectionUtils.setVariableValueInObject(lodgingAddValidator, "skuGroupCatalogItemDao", mockSKUGroupCatalogItemDao);
        ReflectionUtils.setVariableValueInObject(lodgingAddValidator, "mediaDomainCategoriesDao", mockMediaDomainCategoriesDao);
        ReflectionUtils.setVariableValueInObject(lodgingValidator, "mediaDomainCategoriesDao", mockMediaDomainCategoriesDao);
        ReflectionUtils.setVariableValueInObject(lodgingAddValidator, "roomTypeDao", roomTypeDao);
        ReflectionUtils.setVariableValueInObject(lodgingValidator, "roomTypeDao", roomTypeDao);
        providerMapping = new HashSet<>();
        providerMapping.add(new org.apache.commons.collections4.keyvalue.DefaultMapEntry("1", "EPC Internal User"));
        providerMapping.add(new org.apache.commons.collections4.keyvalue.DefaultMapEntry("6", "SCORE"));
        when(mockProviderProperties.entrySet()).thenReturn(providerMapping);
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
                        "          \"subcategoryId\": \"10000\"," +
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
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 0);
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockProviderProperties, times(1)).entrySet();
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
                        "          \"subcategoryId\": \"10000\"," +
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
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 1);
        assertTrue(errorList.get(0).equals("The provided domainId does not exist."));
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockProviderProperties, times(1)).entrySet();
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
                        "          \"subcategoryId\": \"10000\"," +
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
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 1);
        assertTrue(errorList.get(0).equals("The provided mediaProvider does not exist."));
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockProviderProperties, times(1)).entrySet();
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
                        "          \"subcategoryId\": \"66\"," +
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
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 1);
        assertTrue(errorList.get(0).equals("The provided category does not exist."));
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockProviderProperties, times(1)).entrySet();
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
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 0);
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockProviderProperties, times(1)).entrySet();
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
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 0);
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockProviderProperties, times(1)).entrySet();
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
                        "          \"subcategoryId\": \"10000\"," +
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
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 1);
        assertTrue(errorList.get(0).equals("The following roomIds [5678] do not belong to the property."));
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockProviderProperties, times(1)).entrySet();
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
                        "          \"subcategoryId\": \"10000\"," +
                        "          \"propertyHero\": \"true\" " +
                        "                      }" +
                        " }";
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        final List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        when(mockSKUGroupCatalogItemDao.skuGroupExists(anyInt())).thenReturn(Boolean.TRUE);
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 0);
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockProviderProperties, times(1)).entrySet();
        verify(mockMediaDomainCategoriesDao, times(1)).subCategoryIdExists(any(OuterDomain.class), eq("1033"));
        verifyZeroInteractions(mockPropertyRoomTypeGetIDSproc);
    }

    @Test
    public void testMediaProviderIgnoreCase() throws Exception {
        final String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello.jpg\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\", " +
                        "    \"domainProvider\": \"epc InTerNaL user\", " +
                        "    \"domainFields\": { " +
                        "          \"subcategoryId\": \"10000\"," +
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
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 0);
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockProviderProperties, times(1)).entrySet();
        verify(mockMediaDomainCategoriesDao, times(1)).subCategoryIdExists(any(OuterDomain.class), eq("1033"));
        verify(mockPropertyRoomTypeGetIDSproc, times(1)).execute(any(OuterDomain.class));
    }

    @Test
    public void testDuplicateRoom() throws Exception {
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
                        "          \"subcategoryId\": \"10000\"," +
                        "          \"propertyHero\": \"true\"," +
                        "          \"rooms\": [ " +
                        "               {" +
                        "                 \"roomId\": \"222\", " +
                        "                 \"roomHero\": \"true\" " +
                        "               }, " +
                        "               {" +
                        "                 \"roomId\": \"222\", " +
                        "                 \"roomHero\": \"false\" " +
                        "               }" +
                        "                     ]" +
                        "                       }" +
                        " }";
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        final List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        when(mockSKUGroupCatalogItemDao.skuGroupExists(anyInt())).thenReturn(Boolean.TRUE);
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 1);
        assertTrue(errorList.get(0).equals("The request contains duplicate rooms."));
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockProviderProperties, times(1)).entrySet();
        verify(mockMediaDomainCategoriesDao, times(1)).subCategoryIdExists(any(OuterDomain.class), eq("1033"));
        verify(mockPropertyRoomTypeGetIDSproc, times(1)).execute(anyInt());
    }

    @Test
    public void testSubCategory0() throws Exception {
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
                        "          \"subcategoryId\": \"0\"," +
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
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 0);
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockProviderProperties, times(1)).entrySet();
        verify(mockMediaDomainCategoriesDao, times(1)).subCategoryIdExists(any(OuterDomain.class), eq("1033"));
        verify(mockPropertyRoomTypeGetIDSproc, times(1)).execute(any(OuterDomain.class));
    }

    @Test
    public void testSubCategory3MediaAdd() throws Exception {
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
                        "          \"subcategoryId\": \"3\"," +
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
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 1);
        assertTrue(errorList.get(0).equals("The provided category does not exist."));
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockProviderProperties, times(1)).entrySet();
        verify(mockMediaDomainCategoriesDao, times(1)).subCategoryIdExists(any(OuterDomain.class), eq("1033"));
        verify(mockPropertyRoomTypeGetIDSproc, times(1)).execute(any(OuterDomain.class));
    }

    @Test
    public void testSubCategory3AcquireMedia() throws Exception {
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
                        "          \"subcategoryId\": \"3\"," +
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
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<String> errorList = lodgingValidator.validateImages(imageMessageList);
        assertEquals(errorList.size(), 0);
        verify(mockMediaDomainCategoriesDao, times(1)).subCategoryIdExists(any(OuterDomain.class), eq("1033"));
        verify(mockPropertyRoomTypeGetIDSproc, times(1)).execute(any(OuterDomain.class));
    }
    
    @Test
    public void testInvalidRoomsField() throws Exception {
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
                        "          \"subcategoryId\": \"10000\"," +
                        "          \"propertyHero\": \"true\"," +
                        "          \"rooms\": [ " +
                        "               {" +                      
                        "                 \"roomHero\": \"true\" " +
                        "               }" +
                        "                     ]" +
                        "                       }" +
                        " }";
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        final List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        when(mockSKUGroupCatalogItemDao.skuGroupExists(anyInt())).thenReturn(Boolean.TRUE);
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        when(mockPropertyRoomTypeGetIDSproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        verify(mockSKUGroupCatalogItemDao, times(1)).skuGroupExists(anyInt());
        verify(mockProviderProperties, times(1)).entrySet();
        verify(mockMediaDomainCategoriesDao, times(1)).subCategoryIdExists(any(OuterDomain.class), eq("1033"));
        assertTrue(errorList.size() == 1);
        final String errorMessage = errorList.get(0);
        assertTrue("Some room-entries have no roomId key.".equals(errorMessage));

    }

    @Test
    public void testRoomsNotAList()
    {
        final String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello.jpg\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\", " +
                        "    \"domainProvider\": \"EPC Internal User\", " +
                        "    \"domainFields\": " +
                        "     {" +
                        "          \"subcategoryId\": \"10000\"," +
                        "          \"propertyHero\": \"true\"," +
                        "          \"rooms\": 1" +
                        "     }" +
                        " }";
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        final List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        when(mockSKUGroupCatalogItemDao.skuGroupExists(anyInt())).thenReturn(Boolean.TRUE);
        when(mockSQLMediaDomainCategoriesSproc.execute(LOCALID)).thenReturn(catMockResults);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 1);
        final String errorMessage = errorList.get(0);
        assertTrue("The rooms field must be a list.".equals(errorMessage));
    }

}
