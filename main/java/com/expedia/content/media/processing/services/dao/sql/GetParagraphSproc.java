package com.expedia.content.media.processing.services.dao.sql;

import com.expedia.content.media.processing.services.dao.domain.Paragraph;
import com.expedia.content.media.processing.services.util.TimeZoneWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Sproc to retrieve paragraph from the DB
 */
@Component
public class GetParagraphSproc extends StoredProcedure {

    private static final String PROC_NAME = "ParagraphItemGet#03";
    private static final String PROC_RESULT = "paragraph";
    public static final int P_SECTION_TYPE_ID = 3;
    public static final int P_PARAGRAPH_NBR = 1;

    @Autowired
    public GetParagraphSproc(DataSource dataSource) {
        super(dataSource, PROC_NAME);
        declareParameter(new SqlParameter("@pCatalogItemID", Types.INTEGER));
        declareParameter(new SqlParameter("@pSectionTypeID", Types.INTEGER));
        declareParameter(new SqlParameter("@pParagraphNbr", Types.INTEGER));
        declareParameter(new SqlReturnResultSet(PROC_RESULT, new ParagraphRowMapper()));
    }

    private static class ParagraphRowMapper implements RowMapper<Paragraph> {
        @Override
        public Paragraph mapRow(ResultSet resultSet, int i) throws SQLException {
            Date effectiveEndDate = null;
            Date effectiveStartDate = null;
            if (resultSet.getString("EffectiveStartDate") != null) {
                effectiveStartDate = TimeZoneWrapper.covertLcmTimeZone(resultSet.getString("EffectiveStartDate"));
            }
            if (resultSet.getString("EffectiveEndDate") != null) {
                effectiveEndDate = TimeZoneWrapper.covertLcmTimeZone(resultSet.getString("EffectiveEndDate"));
            }

            final Paragraph paragraph = Paragraph.builder()
                    .catalogItemId(resultSet.getInt("CatalogItemID"))
                    .sectionTypeId(resultSet.getInt("SectionTypeID"))
                    .paragraphNbr(resultSet.getInt("ParagraphNbr"))
                    .paragraphTypeId(resultSet.getShort("ParagraphTypeID"))
                    .langId(resultSet.getInt("LangID"))
                    .paragraphTxt(resultSet.getString("ParagraphTxt"))
                    .effectiveStartDate(effectiveStartDate == null ? null : new Timestamp(effectiveStartDate.getTime()))
                    .effectiveEndDate(effectiveEndDate == null ? null : new Timestamp(effectiveEndDate.getTime()))
                    .paragraphMediaId(resultSet.getInt("ParagraphMediaID"))
                    .mediaSizeTypeId(resultSet.getShort("MediaSizeTypeID"))
                    .updateDate(new Timestamp(TimeZoneWrapper.covertLcmTimeZone(resultSet.getString("UpdateDate")).getTime()))
                    .lastUpdatedBy(resultSet.getString("LastUpdatedBy"))
                    .lastUpdateLocation(resultSet.getString("LastUpdateLocation"))
                    .contentSourceTypeId(resultSet.getString("ContentSourceTypeID"))
                    .build();

            return paragraph;
        }
    }

    public List<Paragraph> getParagraph(int catalogItemId) {
        final Map<String, Object> results = execute(catalogItemId, P_SECTION_TYPE_ID, P_PARAGRAPH_NBR);
        return (List<Paragraph>) results.get(PROC_RESULT);
    }
}
