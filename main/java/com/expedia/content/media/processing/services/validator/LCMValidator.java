package com.expedia.content.media.processing.services.validator;


import com.expedia.content.media.processing.services.dao.CatalogItemMediaLstSproc;
import com.expedia.content.media.processing.services.dao.MediaDomainCategoriesDao;
import com.expedia.content.media.processing.services.dao.MediaProcessLog;
import com.expedia.content.media.processing.services.dao.SQLMediaLogSproc;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

public class LCMValidator {

    @Autowired
    private CatalogItemMediaLstSproc catalogItemMediaLstSproc;

    final Map<String, Object> results = sqlMediaLogSproc.execute(fileNameAll);
    final List<MediaProcessLog> mediaLogStatuses = (List<MediaProcessLog>) results.get(SQLMediaLogSproc.MEDIAS_RESULT_SET);
}
