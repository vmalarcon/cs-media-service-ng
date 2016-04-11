package com.expedia.content.media.processing.services.util;

import org.junit.Test;

import java.sql.SQLException;

public class TimeZoneWrapperTest {

    @Test(expected = SQLException.class)
    public void testParseException() throws SQLException{
        String dateAsInput = "1980:12:12:12";
        TimeZoneWrapper.covertLcmTimeZone(dateAsInput);
    }

    @Test
    public void testConvertNormal() throws SQLException{
        String dateAsInput = "2016-04-08 13:39:00";
        TimeZoneWrapper.covertLcmTimeZone(dateAsInput);
    }
}
