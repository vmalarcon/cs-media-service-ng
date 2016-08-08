package com.expedia.content.media.processing.services.util;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class TimeZoneWrapperTest {

    private Date date;
    private Date epochDate = new Date(0);

    @Test
    public void testParseException() throws Exception {
        String dateAsInput = "1980:12:12:12";
        date = TimeZoneWrapper.covertLcmTimeZone(dateAsInput);
        assertEquals(epochDate, date);
    }

    @Test
    public void testConvertNormal() throws Exception {
        String dateAsInput = "2016-04-08 13:39:00";
        date = TimeZoneWrapper.covertLcmTimeZone(dateAsInput);
        assertNotNull(date);
        assertNotEquals(epochDate, date);
    }

    @Test
    public void testConvertNull() throws Exception {
        String dateAsInput = null;
        date = TimeZoneWrapper.covertLcmTimeZone(dateAsInput);
        assertEquals(epochDate, date);
    }

    @Test
    public void testConvertEmpty() throws Exception {
        String dateAsInput = "";
        date = TimeZoneWrapper.covertLcmTimeZone(dateAsInput);
        assertEquals(epochDate, date);
    }

    @Test
    public void testConvertBadFormat() throws Exception {
        String dateAsInput = ".2016E.02016E0";
        date = TimeZoneWrapper.covertLcmTimeZone(dateAsInput);
        assertEquals(epochDate, date);
    }
}
