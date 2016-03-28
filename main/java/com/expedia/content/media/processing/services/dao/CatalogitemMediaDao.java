package com.expedia.content.media.processing.services.dao;

import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.domain.LcmMediaRoom;

import java.util.List;

public interface CatalogitemMediaDao {

    public void updateCatalogItem(ImageMessage imageMessage, int mediaId, int domainId);
    public List<LcmMediaRoom> getLcmRoomsByMediaId(final int mediaId);

    public void addOrUpdateParagraph(final int roomId, final int mediaId);

    public void deleteParagraph(final int catalogItemId);

    public void deleteCatalogItem(final int catalogItemId, final int mediaId);

    public void addCatalogItemForRoom(final int roomId, int mediaId,ImageMessage imageMessage);
}
