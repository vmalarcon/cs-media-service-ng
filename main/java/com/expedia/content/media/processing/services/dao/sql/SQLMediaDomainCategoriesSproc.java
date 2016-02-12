package com.expedia.content.media.processing.services.dao.sql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.stereotype.Repository;

import com.expedia.content.media.processing.services.dao.domain.MediaCategory;
import com.expedia.content.media.processing.services.dao.domain.MediaSubCategory;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Call a MSSQL Sproc [MediaCategoryLst] in LCM in order to retrieve data from MediaCategoryLoc and MediaSubCategoryLoc tables
 */
@Repository
public class SQLMediaDomainCategoriesSproc extends StoredProcedure {

    public static final String MEDIA_CATEGORY_RESULT_SET = "mediaCategories";
    public static final String MEDIA_SUB_CATEGORY_RESULT_SET = "mediaSubCategories";

    @Autowired
    public SQLMediaDomainCategoriesSproc(final DataSource dataSource) {
        super(dataSource, "MediaCategoryLst");
        declareParameter(new SqlParameter("@pLangId", Types.SMALLINT));
        declareParameter(new SqlReturnResultSet(MEDIA_CATEGORY_RESULT_SET, new MediaCategoryRowMapper()));
        declareParameter(new SqlReturnResultSet(MEDIA_SUB_CATEGORY_RESULT_SET, new MediaSubCategoryRowMapper()));
    }

    /**
     * Spring {@link RowMapper} implementation to converts a result set to a object
     * {@link MediaCategory}
     */
    private class MediaCategoryRowMapper implements RowMapper<MediaCategory> {
        @Override
        public MediaCategory mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            final int mediaCategoryID = resultSet.getInt("MediaCategoryID");
            final int langID = resultSet.getInt("LangID");
            final String mediaCategoryName = resultSet.getString("MediaCategoryName");
            return new MediaCategory(String.valueOf(mediaCategoryID), String.valueOf(langID), mediaCategoryName);
        }
    }

    /**
     * Spring {@link RowMapper} implementation to converts a result set to a object
     * {@link MediaSubCategory}
     */
    private class MediaSubCategoryRowMapper implements RowMapper<MediaSubCategory> {
        @Override
        public MediaSubCategory mapRow(final ResultSet resultSet, final int rowNum) throws SQLException {
            final int mediaCategoryID = resultSet.getInt("MediaCategoryID");
            final int mediaSubCategoryID = resultSet.getInt("MediaSubCategoryID");
            final int langID = resultSet.getInt("LangID");
            final String mediaSubCategoryName = resultSet.getString("MediaSubCategoryName");
            return  new MediaSubCategory(String.valueOf(mediaCategoryID), String.valueOf(mediaSubCategoryID), String.valueOf(langID), mediaSubCategoryName);
        }
    }
}
