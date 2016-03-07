package com.expedia.content.media.processing.services.util;

import com.amazonaws.util.StringUtils;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;

import org.apache.commons.io.FilenameUtils;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Utility class for resolving file names
 */
public class FileNameUtil {

    /**
     * This function takes in the ImageMessage with mediaGuid and returns the fileName in the following format:
     * EID_ProviderName_MediaGUID.jpg
     *
     */
    public static final Function<ImageMessage, String> guidProviderNameToFileNameFunction = (consumedImageMessage) -> {
        final String fileNameFromMediaGUID = consumedImageMessage.getOuterDomainData().getDomainId() + "_" + consumedImageMessage.getOuterDomainData().getProvider()
                + "_" + consumedImageMessage.getMediaGuid() + "." + FilenameUtils.getExtension(consumedImageMessage.getFileUrl());
        return fileNameFromMediaGUID;
    };

    /**
     * This function takes in the ImageMessage and returns the fileName from the imageMessage if it is already set and if it is not
     * it is set in the following format: 
     * baseNameOfFileURL.jpg
     *
     */
    public static final Function<ImageMessage, String> fileURLToFileNameFunction = (consumedImageMessage) -> {
        if (StringUtils.isNullOrEmpty(consumedImageMessage.getFileName())) {
            final String fileNameFromFileUrl =
                    FilenameUtils.getBaseName(consumedImageMessage.getFileUrl()) + "." + FilenameUtils.getExtension(consumedImageMessage.getFileUrl());
            return fileNameFromFileUrl;
        }
        return consumedImageMessage.getFileName();
    };

    /**
     * Mapping enum for LCM:MediaProvider to logic for resolving fileName
     */
    public enum MediaProvider {
        EPC_INTERNAL_USER("epc internal user"),
        EPC_EXTERNAL_USER("epc external user"),
        EPC_LEGACY("epc legacy"),
        MOBILE("mobile"),
        EEM_MIGRATION("eem migration"),
        SCORE("score"),
        OTHER("other"),
        GIGWALK("gigwalk"),
        TV_TRIP("tv trip"),
        SMART_SHOOT("smart shoot"),
        CONCIERGE_360("concierge 360"),
        CARIBWEBSERVICE("caribwebservice"),
        ELONG("elong"),
        EASY_VIEW_MEDIA("easy view media"),
        EVOLVING_PHOTOGRAPHY("evolving photography"),
        FCE_DESIGN("fce design"),
        FUSION("360 fusion"),
        HD_MEDIA("hd media"),
        HOTEL_PROVIDED("hotel provided"),
        ICE_PORTAL("ice portal"),
        MOVING_PICTURES("moving pictures"),
        EYENAV_360("eyenav 360"),
        NTT("ntt"),
        PANOMATICS_ASIA("panomatics asia"),
        PREVU("prevu"),
        PROSEARCHPLUS("prosearchplus"),
        RAINBIRD_PHOTOGRAPHY("rainbird photography"),
        REAL_BIG_TOURS("real big tours"),
        RTV_INC("rtv, inc."),
        SEE_VIRTUAL_360("see virtual 360"),
        SHOW_HOTEL("show hotel"),
        TESTURE("testure"),
        VFMLEONARDO("vfmleonardo"),
        VISUAL_HOTELS("visual hotels"),
        VISION_ANGULAR("vision angular"),
        VR_NATIONAL("vr national"),
        VRX_STUDIOS("vrx studios"),
        WORLDGUIDE("360 worldguide"),
        HOTELS("hotels"),
        EXPEDIA("expedia"),
        SPENCER_CREATIVE("spencer creative"),
        NIKHILESH_HAVAL("nikhilesh haval"),
        TITANIO("titanio"),
        DIGITAL_CREATIVITY_INC("100 digital creativity, inc"),
        PEGASUS("pegasus"),
        HOTELBEDS("hotelbeds"),
        JUMBOTOUR("jumbotour"),
        HOMEAWAY("homeaway"),
        WOTIF("wotif"),
        EVIIVO("eviivo"),
        FREETOBOOK("freetobook", guidProviderNameToFileNameFunction),
        PRODUCT_API_TEST("productapi-test"),
        ORBITZ("orbitz"),
        REPLACEPROVIDER("replaceprovider");

        private final String name;
        private final Function<ImageMessage, String> function;


        MediaProvider(String mediaProvider) {
            this(mediaProvider, fileURLToFileNameFunction);
        }

        MediaProvider(String mediaProvider, Function<ImageMessage, String> function) {
            this.name = mediaProvider;
            this.function = function;
        }

        private String getName() {
            return name;
        }

        private static Optional<MediaProvider> findProviderByName(String providerName) {
            return Stream.of(values()).filter(mp -> mp.getName().equals(providerName.toLowerCase())).findFirst();
        }
    }


    /**
     * resolve FileName by the MediaProvider name
     *
     * @param imageMessage
     */
    public static String resolveFileNameByProvider(ImageMessage imageMessage) {
        String providerName = imageMessage.getOuterDomainData().getProvider();
        Optional<MediaProvider> mediaProvider = MediaProvider.findProviderByName(providerName);
        if (mediaProvider.isPresent()) {
            return mediaProvider.get().function.apply(imageMessage);
        }
        return guidProviderNameToFileNameFunction.apply(imageMessage);
    }

}
