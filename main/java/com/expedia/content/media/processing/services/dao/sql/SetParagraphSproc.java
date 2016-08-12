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
 * Sproc to update paragraph in the DB
 */
@Component
public class SetParagraphSproc extends StoredProcedure {

    private static final FormattedLogger LOGGER = new FormattedLogger(SetParagraphSproc.class);

    private static final String PROC_NAME = "ParagraphItemSet#02";

    @Autowired
    public SetParagraphSproc(DataSource dataSource) {
        super(dataSource, PROC_NAME);
        declareParameter(new SqlParameter("@pCatalogItemID", Types.INTEGER));
        declareParameter(new SqlParameter("@pSectionTypeID", Types.INTEGER));
        declareParameter(new SqlParameter("@pParagraphNbr", Types.INTEGER));
        declareParameter(new SqlParameter("@pParagraphMediaID", Types.INTEGER));
        declareParameter(new SqlParameter("@pLangID", Types.INTEGER));
        declareParameter(new SqlParameter("@pParagraphTxt", Types.VARCHAR));
        declareParameter(new SqlParameter("@pEffectiveStartDate", Types.TIMESTAMP));
        declareParameter(new SqlParameter("@pEffectiveEndDate", Types.TIMESTAMP));
        declareParameter(new SqlParameter("@pSemanticDesc", Types.VARCHAR));
        declareParameter(new SqlParameter("@pParagraphTypeID", Types.INTEGER));
        declareParameter(new SqlParameter("@pMediaSizeTypeID", Types.INTEGER));
        declareParameter(new SqlParameter("@pLastUpdatedBy", Types.VARCHAR));
        declareParameter(new SqlParameter("@pLastUpdateLocation", Types.VARCHAR));
        declareParameter(new SqlParameter("@pUpdateParagraphNbr", Types.INTEGER));
    }

    public void setParagraph(int catalogItemId,
                             int sectionTypeId,
                             int paragraphNbr,
                             Integer paragraphMediaId,
                             int langId,
                             String paragraphTxt,
                             Timestamp effectiveStartDate,
                             Timestamp effectiveEndDate,
                             Integer semanticDesc,
                             int paragraphTypeId,
                             Integer mediaSizeTypeId,
                             String lastUpdatedBy,
                             String lastUpdateLocation,
                             Integer updateParagraphNbr) {
        LOGGER.info("Calling Sproc={} " +
                "CatalogItemId={} " +
                "SectionTypeId={} " +
                "ParagraphNbr={} " +
                "ParagraphMediaId={} " +
                "LangId={} " +
                "ParagraphTxt={} " +
                "EffectiveStartDate={} " +
                "EffectiveEndDate={} " +
                "SemanticDesc={} " +
                "ParagraphTypeId={} " +
                "MediaSizeTypeId={} " +
                "LastUpdatedBy={} " +
                "LastUpdateLocation={} " +
                "UpdateParagraphNbr={}",
                PROC_NAME,
                catalogItemId,
                sectionTypeId,
                paragraphNbr,
                paragraphMediaId,
                langId,
                paragraphTxt,
                effectiveStartDate,
                effectiveEndDate,
                semanticDesc,
                paragraphTypeId,
                mediaSizeTypeId,
                lastUpdatedBy,
                lastUpdateLocation,
                updateParagraphNbr);
        execute(catalogItemId,
                sectionTypeId,
                paragraphNbr,
                paragraphMediaId,
                langId,
                paragraphTxt,
                effectiveStartDate,
                effectiveEndDate,
                semanticDesc,
                paragraphTypeId,
                mediaSizeTypeId,
                lastUpdatedBy,
                lastUpdateLocation,
                updateParagraphNbr);
    }
}
