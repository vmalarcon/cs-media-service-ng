package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.pipeline.util.TimeZoneUtil;
import org.apache.commons.lang.StringUtils;

import java.util.Date;

public class TimeZoneWrapper {
    private TimeZoneWrapper() {
    }

    @SuppressWarnings({"PMD.AvoidReassigningParameters"})
    public static Date covertLcmTimeZone(String dateAsString) {
        Date date = new Date(0);
        dateAsString = StringUtils.defaultIfEmpty(dateAsString, StringUtils.EMPTY);
        try {
            date = TimeZoneUtil.convertLCMTimeZoneDate(dateAsString);
        } catch (Exception ex) {
            return date;
        }
        return date;
    }
}
