package com.expedia.content.media.processing.services.util;

import com.expedia.content.media.processing.pipeline.domain.Domain;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.OuterDomain;
import com.expedia.content.media.processing.services.dao.domain.Media;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FileNameUtilTest {

    @Test
    public void testResolveFileNameByProvider() {

        ImageMessage message = new ImageMessage.ImageMessageBuilder()
                .fileName("expedia.jpg")
                .fileUrl("http://i.imgur.com/expedia.jpg")
                .providedName(FileNameUtil.MediaProvider.HOTEL_PROVIDED.toString())
                .mediaGuid("222")
                .outerDomainData(new OuterDomain(Domain.LODGING, "123", "Hotel Provided", null, null))
                .build();

        String result = FileNameUtil.resolveFileNameByProvider(message);
        assertEquals("123_HotelProvided_222.jpg", result);
    }

    @Test
    public void testResolveFileNameToDisplay() {

        Media media = new Media();
        media.setFileName("expedia.jpg");
        media.setProvidedName("Ice Portal");
        media.setFileUrl("http://i.imgur.com/expedia.jpg");

        String result = FileNameUtil.resolveFileNameToDisplay(media);
        assertEquals("Ice Portal", result);

    }
}
