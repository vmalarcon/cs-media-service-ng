package com.expedia.content.media.processing.services.dao.sql;


import com.expedia.content.media.processing.pipeline.util.FormattedLogger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * Sproc to add paragraph in the DB
 */
@Component
public class AddParagraphSproc extends StoredProcedure {

    private static final FormattedLogger LOGGER = new FormattedLogger(AddParagraphSproc.class);

    private static final String PROC_NAME = "ParagraphItemAdd#03";

    @Autowired
    public AddParagraphSproc(DataSource dataSource) {
        super(dataSource, PROC_NAME);
        declareParameter(new SqlParameter("@pCatalogItemID", Types.INTEGER));
        declareParameter(new SqlParameter("@pSectionTypeID", Types.INTEGER));
        declareParameter(new SqlParameter("@pParagraphNbr", Types.INTEGER));
        declareParameter(new SqlParameter("@pLangID", Types.INTEGER));
        declareParameter(new SqlParameter("@pParagraphTypeID", Types.INTEGER));
        declareParameter(new SqlParameter("@pParagraphTxt", Types.VARCHAR));
        declareParameter(new SqlParameter("@pEffectiveStartDate", Types.TIMESTAMP));
        declareParameter(new SqlParameter("@pEffectiveEndDate", Types.TIMESTAMP));
        declareParameter(new SqlParameter("@pParagraphMediaID", Types.INTEGER));
        declareParameter(new SqlParameter("@pContentProviderID", Types.INTEGER));
        declareParameter(new SqlParameter("@pMediaSizeTypeID", Types.INTEGER));
        declareParameter(new SqlParameter("@pLastUpdatedBy", Types.VARCHAR));
        declareParameter(new SqlParameter("@pLastUpdateLocation", Types.VARCHAR));
        declareParameter(new SqlParameter("@pContentSourceTypeID", Types.INTEGER));
    }

    public void addParagraph(int catalogItemId,
            int sectionTypeId,
            int paragraphNbr,
            int langId,
            int paragraphTypeId,

            String paragraphTxt,
            Timestamp effectiveStartDate,
            Timestamp effectiveEndDate,
            int paragraphMediaId,
            int contentProviderId,

            Integer mediaSizeTypeId,
            String lastUpdatedBy,
            String lastUpdateLocation,
            Integer contentSourceTypeID) {
        LOGGER.info("Calling Sproc={} " +
                        "CatalogItemId={} " +
                        "SectionTypeId={} " +
                        "ParagraphNbr={} "+
                        "LangId={} " +
                        "ParagraphTypeId={} " +

                        "ParagraphTxt={} " +
                        "EffectiveStartDate={} " +
                        "EffectiveEndDate={} " +
                        "ParagraphMediaId={} " +
                        "ContentProviderId={} " +

                        "MediaSizeTypeId={} " +
                        "LastUpdatedBy={} " +
                        "LastUpdateLocation={} " +
                        "ContentSourceTypeID={}",
                PROC_NAME,
                catalogItemId,
                sectionTypeId,
                paragraphNbr,
                langId,
                paragraphTypeId,

                paragraphTxt,
                effectiveStartDate,
                effectiveEndDate,
                paragraphMediaId,
                contentProviderId,

                mediaSizeTypeId,
                lastUpdatedBy,
                lastUpdateLocation,
                contentSourceTypeID);
        execute(catalogItemId,
                sectionTypeId,
                paragraphNbr,
                langId,
                paragraphTypeId,

                paragraphTxt,
                effectiveStartDate,
                effectiveEndDate,
                paragraphMediaId,
                contentProviderId,

                mediaSizeTypeId,
                lastUpdatedBy,
                lastUpdateLocation,
                contentSourceTypeID);
    }
}
