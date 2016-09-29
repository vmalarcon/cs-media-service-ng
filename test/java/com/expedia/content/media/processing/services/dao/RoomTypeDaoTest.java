package com.expedia.content.media.processing.services.dao;


import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RoomTypeDaoTest {

    @Mock
    PropertyRoomTypeGetIDSproc sproc;

    List<RoomType> mockRoomTypes;
    RoomTypeDao roomTypeDao;
    Map<String, Object> mockRoomResults = new HashMap<>();
    Map<String, Object> domainField = new HashMap<>();
    Map<String, String> rooms = new HashMap<>();

    @BeforeClass
    public static void setUp() {
        System.setProperty("EXPEDIA_ENVIRONMENT", "test");
        System.setProperty("AWS_REGION", "us-west-2");
    }

    @Before
    public void initialize() {
        mockRoomTypes = new ArrayList<>();
        mockRoomTypes.add(new RoomType(222, 555, new Timestamp(1339150200000L), "phoenix", "phoenix"));
        mockRoomTypes.add(new RoomType(333, 444, new Timestamp(1339150200000L), "phoenix", "phoenix"));
        mockRoomResults = new HashMap<>();
        mockRoomResults.put(PropertyRoomTypeGetIDSproc.ROOM_TYPE_RESULT_SET, mockRoomTypes);
        roomTypeDao = new RoomTypeDao(sproc);
    }


    @Test
    public  void testRoomTypeIdExists() {
        domainField.put("category", "22005");
        domainField.put("propertyHero", "true");
        rooms.put("roomId", "222");
        rooms.put("roomHero", "true");
        domainField.put("rooms", Arrays.asList(rooms));
        when(sproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<Object> invalidRoomIds = roomTypeDao.getInvalidRoomIds(new OuterDomain(Domain.LODGING, "123", "", "EPC Internal User", domainField));
        assertTrue(invalidRoomIds.isEmpty());
        verify(sproc, times(1)).execute(anyInt());
    }

    @Test
    public  void testRoomTypeIdDoesNotExist() {
        domainField.put("category", "22005");
        domainField.put("propertyHero", "true");
        rooms.put("roomId", "444");
        rooms.put("roomHero", "true");
        domainField.put("rooms", Arrays.asList(rooms));
        when(sproc.execute(anyInt())).thenReturn(mockRoomResults);
        final List<Object> invalidRoomIds = roomTypeDao.getInvalidRoomIds(new OuterDomain(Domain.LODGING, "123", "", "EPC Internal User", domainField));
        assertFalse(invalidRoomIds.isEmpty());
        verify(sproc, times(1)).execute(anyInt());
    }
}
