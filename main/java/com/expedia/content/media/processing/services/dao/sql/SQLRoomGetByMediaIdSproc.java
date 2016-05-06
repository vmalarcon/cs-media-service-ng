package com.expedia.content.media.processing.services.dao.sql;


import java.sql.Types;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.stereotype.Repository;

/**
 * Call a MSSQL Sproc [RoomTypeGetByMediaID] in LCM in order to retrieve data from the Media,RoomTypeCatalogItemID, 
 * CatalogItemMedia, and Paragraph (derivatives) tables to get current rooms of the media id.
 */
@Repository
public class SQLRoomGetByMediaIdSproc extends SQLRoomGetSproc {
    
    @Autowired
    public SQLRoomGetByMediaIdSproc(final DataSource dataSource) {
        super(dataSource, "RoomTypeGetByMediaID");
        declareParameter(new SqlParameter("@pMediaID", Types.INTEGER));
        declareParameter(new SqlReturnResultSet(ROOM_SET, new RoomRowMapper()));
    }

}
