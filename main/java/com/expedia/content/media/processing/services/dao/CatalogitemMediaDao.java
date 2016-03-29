package com.expedia.content.media.processing.services.dao;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaRoom;

import java.util.List;

public interface CatalogitemMediaDao {

     void updateCatalogItem(ImageMessage imageMessage, int mediaId, int domainId);
     List<LcmMediaRoom> getLcmRoomsByMediaId(final int mediaId);

     void addOrUpdateParagraph(final int roomId, final int mediaId);

     /**
      * mean un associate the paragraph with the mediaId.
      * @param catalogItemId
      */
     void deleteParagraph(final int catalogItemId);

     void deleteCatalogItem(final int catalogItemId, final int mediaId);

     void addCatalogItemForRoom(final int roomId, int mediaId,ImageMessage imageMessage);
}
