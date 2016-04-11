package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.pipeline.util.TimeZoneUtil;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;

public class TimeZoneWrapper {
    private TimeZoneWrapper() {
    }

    public static Date covertLcmTimeZone(String dateAsString) throws SQLException {
        Date date = null;
        try {
            date = TimeZoneUtil.convertLCMTimeZoneDate(dateAsString);
        } catch (ParseException ex) {
            throw new SQLException(ex);
        }
        return date;
    }
}
