package com.expedia.content.media.processing.services.dao.mediadb;

import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;
import java.util.Arrays;

/**
 * Util class for MediaDB SQL status
 */
public class MediaDBSQLUtil {
    private static final FormattedLogger LOGGER = new FormattedLogger(MediaDBSQLUtil.class);
    private MediaDBSQLUtil() {
        // no-op
    }

    /**
     * A helper method to set elements of an Array into a PreparedStatement.
     *
     * @param statement The PreparedStatement to set values in.
     * @param startIndex The start index to start setting elements.
     * @param array The array of elements to set int he PreparedStatement.
     * @return The final index +1 for setting elements after the array.
     */
    public static int setArray(PreparedStatement statement, final int startIndex, String[] array) {
        int index = startIndex;
        for(final String el : array) {
            try {
                statement.setString(startIndex, el);
                index++;
            } catch (Exception e) {
                LOGGER.error("couldn't set element={} from array={}", el, Arrays.toString(array));
            }
        }
        return index;
    }

    /**
     * A helper method to set tokens for an array of elements in a SQL Query String with \"IN (?)\".
     * i.e. for an array of 3 elements and a query string: FROM table WHERE value IN (?) -> FROM table WHERE value IN (?,?,?)
     *
     * @param sqlString The SQL Query string to add tokens to.
     * @param array The array to add tokens for.
     * @return The SQL Query String with new tokens added.
     */
    public static String setSQLTokensWithArray(String sqlString, String[] array) {
        final StringBuilder tokens = new StringBuilder();
        char delimiter = '\0';
        for (int i = 0; i < array.length; i++) {
            if (delimiter == '\0') {
                tokens.append('?');
                delimiter = ',';
            }
            tokens.append(delimiter).append('?');
        }
        return StringUtils.replace(sqlString, "IN (?)", "IN (" + tokens.toString() + ")");
    }
}
