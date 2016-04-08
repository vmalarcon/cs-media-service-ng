package com.expedia.content.media.processing.services.dao.sql;

import com.expedia.content.media.processing.services.dao.domain.LcmMedia;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Call a MSSQL Sproc [MediaItemGet] in LCM in order to retrieve data from the Media, CatalogItemMedia, and MediaFileName (derivatives) tables
 */
@Repository
public class MediaTableGetSproc extends StoredProcedure {

    public static final String MEDIA_SET = "media";

    @Autowired
    public MediaTableGetSproc(final DataSource dataSource) {
        super(dataSource, "MediaGet#18");
        declareParameter(new SqlParameter("@pMediaID", Types.INTEGER));
        declareParameter(new SqlParameter("@pLangID", Types.INTEGER));

        declareParameter(new SqlReturnResultSet(MEDIA_SET, new MediaRowMapper()));
    }

    /**
     * Spring {@link RowMapper} implementation to converts a result set to a object
     * {@link LcmMedia}
     */
    private class MediaRowMapper implements RowMapper<LcmMedia> {
        @Override
        public LcmMedia mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            final String activeFlag = resultSet.getString("StatusCode");
            return LcmMedia.builder()
                    .mediaId(resultSet.getInt("MediaID"))
                    .formatId(resultSet.getInt("MediaFormatID"))
                    .mediaCreditTxt(resultSet.getString("MediaCreditTxt"))
                    .comment(resultSet.getString("MediaCommentTxt"))
                    .mediaDisplayName(resultSet.getString("MediaDisplayName"))
                    .mediaCaptionTxt(resultSet.getString("MediaCaptionTxt"))
                    .fileName(resultSet.getString("ContentProviderMediaName"))
                    .active(activeFlag != null && "A".equals(activeFlag))
                    .fileName(resultSet.getString("ContentProviderMediaName"))
                    .mediaStartHorizontalPct(resultSet.getDouble("MediaStartHorizontalPct"))
                    .mediaDisplayMethodSeqNbr(resultSet.getShort("MediaDisplayMethodSeqNbr"))
                    //todo sen get updatelocation.
                    .build();
        }
    }


}
