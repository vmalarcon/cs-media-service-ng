package com.expedia.content.media.processing.services.dao.sql;


import java.sql.Types;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.stereotype.Repository;

/**
 * Call a MSSQL Sproc [RoomTypeMediaListByPropertyID] in LCM in order to retrieve data from the Media,RoomTypeCatalogItemID, 
 * CatalogItemMedia, and Paragraph (derivatives) tables to get current rooms of all the media id of a property.
 */
@Repository
public class SQLRoomGetByCatalogItemIdSproc extends SQLRoomGetSproc {
    
    @Autowired
    public SQLRoomGetByCatalogItemIdSproc(final DataSource dataSource) {
        super(dataSource, "RoomTypeMediaListByPropertyID");
        declareParameter(new SqlParameter("@pCatalogItemID", Types.INTEGER));
        declareParameter(new SqlReturnResultSet(ROOM_SET, new RoomRowMapper()));
    }

}
