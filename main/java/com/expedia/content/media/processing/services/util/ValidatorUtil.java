package com.expedia.content.media.processing.services.util;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("PMD.UseUtilityClass")
public class ValidatorUtil {

    public static void putErrorMapToList(List<String> list, StringBuffer errorMsg) {
        list.add(errorMsg.toString());
    }

    /**
     * Makes sure the UUID is formatted properly.
     * @param testUuid UUID string to test.
     * @return True if the UUID is formatted properly, False otherwise.
     */
    public static boolean isValidUUID(String testUuid) {
        try {
            if (StringUtils.isEmpty(testUuid)) {
                return false;
            }
            final UUID uuid = UUID.fromString(testUuid);
            return testUuid.equals(uuid.toString());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
