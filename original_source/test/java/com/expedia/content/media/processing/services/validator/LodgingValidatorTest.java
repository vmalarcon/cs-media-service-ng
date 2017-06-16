package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.DomainCategoriesDao;
import com.expedia.content.media.processing.services.dao.mediadb.MediaDBLodgingReferenceHotelIdDao;
import com.expedia.content.media.processing.services.dao.mediadb.MediaDBLodgingReferenceRoomIdDao;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.expedia.content.media.processing.services.testing.TestingUtil.setFieldValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LodgingValidatorTest {

    @Mock
    private MediaDBLodgingReferenceHotelIdDao mediaDBLodgingReferenceHotelIdDao;
    @Mock
    private MediaDBLodgingReferenceRoomIdDao mediaDBLodgingReferenceRoomIdDao;
    @Mock
    private DomainCategoriesDao mediaDBMediaDomainCategoriesDao;
    @Mock
    private Properties mockProviderProperties;

    LodgingAddValidator lodgingAddValidator;
    LodgingValidator lodgingValidator;
    final String LOCALID = "1033";
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
        setFieldValue(lodgingValidator, "mediaDBLodgingReferenceRoomIdDao", mediaDBLodgingReferenceRoomIdDao);
        setFieldValue(lodgingValidator, "mediaDBMediaDomainCategoriesDao", mediaDBMediaDomainCategoriesDao);
        setFieldValue(lodgingAddValidator, "lodgingValidator", lodgingValidator);
        setFieldValue(lodgingAddValidator, "providerProperties", mockProviderProperties);
        setFieldValue(lodgingAddValidator, "mediaDBLodgingReferenceHotelIdDao", mediaDBLodgingReferenceHotelIdDao);
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
        when(mediaDBLodgingReferenceRoomIdDao.getInvalidRoomIds(eq(imageMessage.getOuterDomainData()))).thenReturn(new ArrayList<>());
        when(mediaDBMediaDomainCategoriesDao.subCategoryIdExists(eq(imageMessage.getOuterDomainData()), eq(LOCALID))).thenReturn(true);
        when(mediaDBLodgingReferenceHotelIdDao.domainIdExists(eq("123"))).thenReturn(true);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 0);
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
        when(mediaDBLodgingReferenceRoomIdDao.getInvalidRoomIds(eq(imageMessage.getOuterDomainData()))).thenReturn(new ArrayList<>());
        when(mediaDBMediaDomainCategoriesDao.subCategoryIdExists(eq(imageMessage.getOuterDomainData()), eq(LOCALID))).thenReturn(true);
        when(mediaDBLodgingReferenceHotelIdDao.domainIdExists(eq("123"))).thenReturn(false);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 1);
        assertTrue(errorList.get(0).equals("The provided domainId does not exist."));
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
        when(mediaDBLodgingReferenceRoomIdDao.getInvalidRoomIds(eq(imageMessage.getOuterDomainData()))).thenReturn(new ArrayList<>());
        when(mediaDBMediaDomainCategoriesDao.subCategoryIdExists(eq(imageMessage.getOuterDomainData()), eq(LOCALID))).thenReturn(true);
        when(mediaDBLodgingReferenceHotelIdDao.domainIdExists(eq("123"))).thenReturn(true);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 1);
        assertTrue(errorList.get(0).equals("The provided mediaProvider does not exist."));
        verify(mockProviderProperties, times(1)).entrySet();
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
        when(mediaDBLodgingReferenceRoomIdDao.getInvalidRoomIds(eq(imageMessage.getOuterDomainData()))).thenReturn(new ArrayList<>());
        when(mediaDBMediaDomainCategoriesDao.subCategoryIdExists(eq(imageMessage.getOuterDomainData()), eq(LOCALID))).thenReturn(false);
        when(mediaDBLodgingReferenceHotelIdDao.domainIdExists(eq("123"))).thenReturn(true);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 1);
        assertTrue(errorList.get(0).equals("The provided category does not exist."));
        verify(mockProviderProperties, times(1)).entrySet();
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
        when(mediaDBLodgingReferenceRoomIdDao.getInvalidRoomIds(eq(imageMessage.getOuterDomainData()))).thenReturn(new ArrayList<>());
        when(mediaDBMediaDomainCategoriesDao.subCategoryIdExists(eq(imageMessage.getOuterDomainData()), eq(LOCALID))).thenReturn(true);
        when(mediaDBLodgingReferenceHotelIdDao.domainIdExists(eq("123"))).thenReturn(true);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 0);
        verify(mockProviderProperties, times(1)).entrySet();
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
        when(mediaDBLodgingReferenceRoomIdDao.getInvalidRoomIds(eq(imageMessage.getOuterDomainData()))).thenReturn(new ArrayList<>());
        when(mediaDBMediaDomainCategoriesDao.subCategoryIdExists(eq(imageMessage.getOuterDomainData()), eq(LOCALID))).thenReturn(true);
        when(mediaDBLodgingReferenceHotelIdDao.domainIdExists(eq("123"))).thenReturn(true);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 0);
        verify(mockProviderProperties, times(1)).entrySet();
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
        when(mediaDBLodgingReferenceRoomIdDao.getInvalidRoomIds(eq(imageMessage.getOuterDomainData()))).thenReturn(Arrays.asList("5678"));
        when(mediaDBMediaDomainCategoriesDao.subCategoryIdExists(eq(imageMessage.getOuterDomainData()), eq(LOCALID))).thenReturn(true);
        when(mediaDBLodgingReferenceHotelIdDao.domainIdExists(eq("123"))).thenReturn(true);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 1);
        assertTrue(errorList.get(0).equals("The following roomIds [5678] do not belong to the property."));
        verify(mockProviderProperties, times(1)).entrySet();

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
        when(mediaDBLodgingReferenceRoomIdDao.getInvalidRoomIds(eq(imageMessage.getOuterDomainData()))).thenReturn(new ArrayList<>());
        when(mediaDBMediaDomainCategoriesDao.subCategoryIdExists(eq(imageMessage.getOuterDomainData()), eq(LOCALID))).thenReturn(true);
        when(mediaDBLodgingReferenceHotelIdDao.domainIdExists(eq("123"))).thenReturn(true);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 0);
        verify(mockProviderProperties, times(1)).entrySet();
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
        when(mediaDBLodgingReferenceRoomIdDao.getInvalidRoomIds(eq(imageMessage.getOuterDomainData()))).thenReturn(new ArrayList<>());
        when(mediaDBMediaDomainCategoriesDao.subCategoryIdExists(eq(imageMessage.getOuterDomainData()), eq(LOCALID))).thenReturn(true);
        when(mediaDBLodgingReferenceHotelIdDao.domainIdExists(eq("123"))).thenReturn(true);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 0);
        verify(mockProviderProperties, times(1)).entrySet();
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
        when(mediaDBLodgingReferenceRoomIdDao.getInvalidRoomIds(eq(imageMessage.getOuterDomainData()))).thenReturn(new ArrayList<>());
        when(mediaDBMediaDomainCategoriesDao.subCategoryIdExists(eq(imageMessage.getOuterDomainData()), eq(LOCALID))).thenReturn(true);
        when(mediaDBLodgingReferenceHotelIdDao.domainIdExists(eq("123"))).thenReturn(true);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 1);
        assertTrue(errorList.get(0).equals("The request contains duplicate rooms."));
        verify(mockProviderProperties, times(1)).entrySet();
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
        when(mediaDBLodgingReferenceRoomIdDao.getInvalidRoomIds(eq(imageMessage.getOuterDomainData()))).thenReturn(new ArrayList<>());
        when(mediaDBMediaDomainCategoriesDao.subCategoryIdExists(eq(imageMessage.getOuterDomainData()), eq(LOCALID))).thenReturn(true);
        when(mediaDBLodgingReferenceHotelIdDao.domainIdExists(eq("123"))).thenReturn(true);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 0);
        verify(mockProviderProperties, times(1)).entrySet();
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
        when(mediaDBLodgingReferenceRoomIdDao.getInvalidRoomIds(eq(imageMessage.getOuterDomainData()))).thenReturn(new ArrayList<>());
        when(mediaDBMediaDomainCategoriesDao.subCategoryIdExists(eq(imageMessage.getOuterDomainData()), eq(LOCALID))).thenReturn(true);
        when(mediaDBLodgingReferenceHotelIdDao.domainIdExists(eq("123"))).thenReturn(true);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 1);
        assertTrue(errorList.get(0).equals("The provided category does not exist."));
        verify(mockProviderProperties, times(1)).entrySet();
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
        when(mediaDBLodgingReferenceRoomIdDao.getInvalidRoomIds(eq(imageMessage.getOuterDomainData()))).thenReturn(new ArrayList<>());
        when(mediaDBMediaDomainCategoriesDao.subCategoryIdExists(eq(imageMessage.getOuterDomainData()), eq(LOCALID))).thenReturn(true);
        when(mediaDBLodgingReferenceHotelIdDao.domainIdExists(eq("123"))).thenReturn(true);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        verify(mockProviderProperties, times(1)).entrySet();
        assertTrue(errorList.size() == 1);
        final String errorMessage = errorList.get(0);
        assertTrue("Some room-entries have no roomId key.".equals(errorMessage));

    }

    @Test
    public void testRoomsNotAList() {
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
        when(mediaDBLodgingReferenceRoomIdDao.getInvalidRoomIds(eq(imageMessage.getOuterDomainData()))).thenReturn(new ArrayList<>());
        when(mediaDBMediaDomainCategoriesDao.subCategoryIdExists(eq(imageMessage.getOuterDomainData()), eq(LOCALID))).thenReturn(true);
        when(mediaDBLodgingReferenceHotelIdDao.domainIdExists(eq("123"))).thenReturn(true);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 1);
        final String errorMessage = errorList.get(0);
        assertTrue("The rooms field must be a list.".equals(errorMessage));
    }

    @Test
    public void testInvalidDomain() throws Exception {
        final String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello.jpg\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"ContentRepo\", " +
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
        when(mediaDBLodgingReferenceRoomIdDao.getInvalidRoomIds(eq(imageMessage.getOuterDomainData()))).thenReturn(new ArrayList<>());
        when(mediaDBMediaDomainCategoriesDao.subCategoryIdExists(eq(imageMessage.getOuterDomainData()), eq(LOCALID))).thenReturn(true);
        when(mediaDBLodgingReferenceHotelIdDao.domainIdExists(eq("123"))).thenReturn(true);
        final List<String> errorList = lodgingAddValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 1);
        String errorMessage = errorList.get(0);
        assertEquals("The provided domain does not exist.", errorMessage);
    }

}
