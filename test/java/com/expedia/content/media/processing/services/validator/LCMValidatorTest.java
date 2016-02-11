package com.expedia.content.media.processing.services.validator;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.SKUGroupCatalogItemDao;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(locations = "classpath:media-services.xml")
public class LCMValidatorTest {

    LCMValidator lcmValidator;

    @BeforeClass
    public static void setUp() {
        System.setProperty("EXPEDIA_ENVIRONMENT", "test");
        System.setProperty("AWS_REGION", "us-west-2");
    }


    @Test
    public void testDomainIdExists() throws Exception {
        lcmValidator = new LCMValidator();
        final String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello.jpg\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\", " +
                        "    \"domainProvider\": \"test\" " +
                        " }";
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        final List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        final SKUGroupCatalogItemDao mockSKUGroupCatalogItemDao = mock(SKUGroupCatalogItemDao.class);
        when(mockSKUGroupCatalogItemDao.gteSKUGroup(anyInt())).thenReturn(Boolean.TRUE);
        ReflectionUtil.setFieldValue(lcmValidator, "skuGroupCatalogItemDao", mockSKUGroupCatalogItemDao);
        final List<Map<String, String>> errorList = lcmValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 0);
        verify(mockSKUGroupCatalogItemDao, times(1)).gteSKUGroup(anyInt());
    }

    @Test
    public void testDomainIdDoesNotExist() throws Exception {
        lcmValidator = new LCMValidator();
        final String jsonMsg =
                "         { " +
                        "    \"fileUrl\": \"http://well-formed-url/hello.jpg\"," +
                        "    \"fileName\": \"Something\", " +
                        "    \"mediaGuid\": \"media-uuid\", " +
                        "    \"domain\": \"Lodging\", " +
                        "    \"domainId\": \"123\", " +
                        "    \"userId\": \"user-id\", " +
                        "    \"domainProvider\": \"test\" " +
                        " }";
        final ImageMessage imageMessage = ImageMessage.parseJsonMessage(jsonMsg);
        final List<ImageMessage> imageMessageList = new ArrayList<>();
        imageMessageList.add(imageMessage);
        final SKUGroupCatalogItemDao mockSKUGroupCatalogItemDao = mock(SKUGroupCatalogItemDao.class);
        when(mockSKUGroupCatalogItemDao.gteSKUGroup(anyInt())).thenReturn(Boolean.FALSE);
        ReflectionUtil.setFieldValue(lcmValidator, "skuGroupCatalogItemDao", mockSKUGroupCatalogItemDao);
        final List<Map<String, String>> errorList = lcmValidator.validateImages(imageMessageList);
        assertTrue(errorList.size() == 1);
        assertTrue(errorList.get(0).get("error").equals("The domainId does not exist in LCM."));
        assertTrue(errorList.get(0).get("fileName").equals("Something"));
        verify(mockSKUGroupCatalogItemDao, times(1)).gteSKUGroup(anyInt());
    }
}
