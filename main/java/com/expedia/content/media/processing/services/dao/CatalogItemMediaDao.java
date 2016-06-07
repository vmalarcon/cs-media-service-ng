package com.expedia.content.media.processing.services.dao;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaRoom;

import java.util.List;

public interface CatalogItemMediaDao {

     /**
      * update the current media's useRank in catalogItemMedia table in LCM, if hero, set 3, if not hero, set the input subcategoryId.
      * @param imageMessage
      * @param domainId
      * @param mediaId
      */
     void updateCatalogItem(ImageMessage imageMessage, int mediaId, int domainId);

     /**
      * get the associated rooms of the media.
      * @param mediaId
      * @return
      */
     List<LcmMediaRoom> getLcmRoomsByMediaId(final int mediaId);

     /**
      * add hero room for the media,
      * if the roomId does not exist in paragraph table, add it
      * if the roomId exist in paragraph table, update sectionType Id to 3
      * @param roomId
      * @param mediaId
      */
     void addOrUpdateParagraph(final int roomId, final int mediaId);

     /**
      * mean unassociate the paragraph with the mediaId.
      * @param catalogItemId
      */
     void deleteParagraph(final int catalogItemId);

     /**
      * remove the room from a media by delete the record in CatalogItemMedia table.
      * @param catalogItemId
      * @param mediaId
      */
     void deleteCatalogItem(final int catalogItemId, final int mediaId);

     /**
      * Add a record in CaltalogItemMedia table.
      * @param roomId Room id the media is going to attach to.
      * @param mediaId Media to attach to a room.
      * @param expediaId The property id the room belongs to.
      * @param imageMessage The content of the image message.
      */
     void addCatalogItemForRoom(final int roomId, int mediaId, Integer expediaId, ImageMessage imageMessage);
}
