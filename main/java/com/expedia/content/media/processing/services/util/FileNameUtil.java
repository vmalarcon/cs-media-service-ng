package com.expedia.content.media.processing.services.util;

import com.amazonaws.util.StringUtils;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;

import com.expedia.content.media.processing.services.dao.domain.Media;
import org.apache.commons.io.FilenameUtils;


import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Utility class for resolving file names
 * --ALL PROVIDERS ADDED TO THE ENUM SHOULD USE THE FUNCTION guidProviderNameToFileNameFunction--
 */
public class FileNameUtil {

    /**
     * --THIS FUNCTION SHOULD BE USED FOR ALL FUTURE PROVIDERS ADDED TO THE ENUM--
     * This function takes in the ImageMessage with mediaGuid and returns the fileName in the following format:
     * EID_ProviderName_MediaGUID.jpg
     *
     */
    private static final Function<ImageMessage, String> guidProviderNameToFileNameFunction = (consumedImageMessage) -> {
        final String fileNameFromMediaGUID = consumedImageMessage.getOuterDomainData().getDomainId() + "_" + StringUtils.replace(consumedImageMessage.getOuterDomainData().getProvider(), " ", "")
                + "_" + consumedImageMessage.getMediaGuid() + "." + FilenameUtils.getExtension(consumedImageMessage.getFileUrl());
        return fileNameFromMediaGUID;
    };

    /**
     * This function takes in the ImageMessage and returns the fileName from the imageMessage if it is already set and if it is not
     * it is set in the following format:
     * baseNameOfFileURL.jpg
     *
     */
    private static final Function<ImageMessage, String> fileURLToFileNameFunction = (consumedImageMessage) -> {
        if (StringUtils.isNullOrEmpty(consumedImageMessage.getFileName())) {
            final String fileNameFromFileUrl =
                    FilenameUtils.getBaseName(consumedImageMessage.getFileUrl()) + "." + FilenameUtils.getExtension(consumedImageMessage.getFileUrl());
            return fileNameFromFileUrl;
        }
        return consumedImageMessage.getFileName();
    };

    /**
     * Mapping enum for LCM:MediaProvider to function logic for resolving a fileName
     */
    public enum MediaProvider {
        DESPEGAR("despegar", guidProviderNameToFileNameFunction),
        FREETOBOOK("freetobook", guidProviderNameToFileNameFunction),
        EPC_INTERNAL_USER("epc internal user", guidProviderNameToFileNameFunction),
        EPC_EXTERNAL_USER("epc external user", guidProviderNameToFileNameFunction),
        SCORE("score", guidProviderNameToFileNameFunction),
        EPC_LEGACY("epc legacy", guidProviderNameToFileNameFunction),
        MOBILE("mobile", guidProviderNameToFileNameFunction),
        EEM_MIGRATION("eem migration", guidProviderNameToFileNameFunction),
        OTHER("other", guidProviderNameToFileNameFunction),
        GIGWALK("gigwalk", guidProviderNameToFileNameFunction),
        TV_TRIP("tv trip", guidProviderNameToFileNameFunction),
        SMART_SHOOT("smart shoot", guidProviderNameToFileNameFunction),
        CONCIERGE_360("concierge 360", guidProviderNameToFileNameFunction),
        CARIBWEBSERVICE("caribwebservice", guidProviderNameToFileNameFunction),
        ELONG("elong", guidProviderNameToFileNameFunction),
        EASY_VIEW_MEDIA("easy view media", guidProviderNameToFileNameFunction),
        EVOLVING_PHOTOGRAPHY("evolving photography", guidProviderNameToFileNameFunction),
        FCE_DESIGN("fce design", guidProviderNameToFileNameFunction),
        FUSION("360 fusion", guidProviderNameToFileNameFunction),
        HD_MEDIA("hd media", guidProviderNameToFileNameFunction),
        HOTEL_PROVIDED("hotel provided", guidProviderNameToFileNameFunction),
        ICE_PORTAL("ice portal"),
        MOVING_PICTURES("moving pictures", guidProviderNameToFileNameFunction),
        EYENAV_360("eyenav 360", guidProviderNameToFileNameFunction),
        NTT("ntt", guidProviderNameToFileNameFunction),
        PANOMATICS_ASIA("panomatics asia", guidProviderNameToFileNameFunction),
        PREVU("prevu", guidProviderNameToFileNameFunction),
        PROSEARCHPLUS("prosearchplus", guidProviderNameToFileNameFunction),
        RAINBIRD_PHOTOGRAPHY("rainbird photography", guidProviderNameToFileNameFunction),
        REAL_BIG_TOURS("real big tours", guidProviderNameToFileNameFunction),
        RTV_INC("rtv, inc.", guidProviderNameToFileNameFunction),
        SEE_VIRTUAL_360("see virtual 360", guidProviderNameToFileNameFunction),
        SHOW_HOTEL("show hotel", guidProviderNameToFileNameFunction),
        TESTURE("testure", guidProviderNameToFileNameFunction),
        VFMLEONARDO("vfmleonardo"),
        VISUAL_HOTELS("visual hotels", guidProviderNameToFileNameFunction),
        VISION_ANGULAR("vision angular", guidProviderNameToFileNameFunction),
        VR_NATIONAL("vr national", guidProviderNameToFileNameFunction),
        VRX_STUDIOS("vrx studios", guidProviderNameToFileNameFunction),
        WORLDGUIDE("360 worldguide", guidProviderNameToFileNameFunction),
        HOTELS("hotels", guidProviderNameToFileNameFunction),
        EXPEDIA("expedia", guidProviderNameToFileNameFunction),
        SPENCER_CREATIVE("spencer creative", guidProviderNameToFileNameFunction),
        NIKHILESH_HAVAL("nikhilesh haval", guidProviderNameToFileNameFunction),
        TITANIO("titanio", guidProviderNameToFileNameFunction),
        DIGITAL_CREATIVITY_INC("100 digital creativity, inc", guidProviderNameToFileNameFunction),
        PEGASUS("pegasus", guidProviderNameToFileNameFunction),
        HOTELBEDS("hotelbeds", guidProviderNameToFileNameFunction),
        JUMBOTOUR("jumbotour", guidProviderNameToFileNameFunction),
        HOMEAWAY("homeaway", guidProviderNameToFileNameFunction),
        WOTIF("wotif", guidProviderNameToFileNameFunction),
        EVIIVO("eviivo", guidProviderNameToFileNameFunction),
        PRODUCT_API_TEST("productapi-test", guidProviderNameToFileNameFunction),
        ORBITZ("orbitz", guidProviderNameToFileNameFunction),
        REPLACEPROVIDER("replaceprovider", guidProviderNameToFileNameFunction),
        TOURICO("Tourico", guidProviderNameToFileNameFunction),
        WOORI("Woori", guidProviderNameToFileNameFunction);

        private final String name;
        private final Function<ImageMessage, String> function;

        // FOR LEGACY PROVIDERS ONLY
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

        private static Optional<MediaProvider> findMediaProviderByProviderName(String providerName) {
            return Stream.of(MediaProvider.values()).filter(mediaProvider -> mediaProvider.getName().equals(providerName.toLowerCase())).findFirst();
        }
    }


    /**
     * resolve FileName by the MediaProvider name
     *
     * @param imageMessage
     */
    public static String resolveFileNameByProvider(ImageMessage imageMessage) {
        String providerName = imageMessage.getOuterDomainData().getProvider();
        Optional<MediaProvider> mediaProvider = MediaProvider.findMediaProviderByProviderName(providerName);
        if (mediaProvider.isPresent()) {
            return mediaProvider.get().function.apply(imageMessage);
        }
        return guidProviderNameToFileNameFunction.apply(imageMessage);
    }

    /**
     * resolve which String to put in the fileName field for MediaGet and GetByDomainID
     *
     * @param media
     * @return fileName to display
     */
    public static String resolveFileNameToDisplay(final Media media) {
        return (media.getProvidedName() == null) ?
                (media.getFileUrl() == null) ? media.getFileName() :
                        getFileNameFromUrl(media.getFileUrl()) :
                media.getProvidedName();
    }

    /**
     * returns the fileName from a url
     * Example:  getFileNameFromUrl("https://www.ACoolWebSite.com/CoolPics/LargePics/BestQuality/aPrettySweetPic.jpg") will
     * return "aPrettySweetPic.jpg"
     *
     * @param url
     * @return fileName from the input url
     */
    public static String getFileNameFromUrl(final String url) {
        return FilenameUtils.getBaseName(url)
                + "." + FilenameUtils.getExtension(url);
    }

    /**
     * returns the fileName from a url
     * Example:  getFileNameFromUrl("https://www.ACoolWebSite.com/CoolPics/LargePics/BestQuality/aPrettySweetPic.jpeg", ".jpg") will
     * return "aPrettySweetPic.jpg"
     *
     * @param url
     * @param extension
     * @return fileName from the input url
     */
    public static String getFileNameFromUrl(final String url, final String extension) {
        return FilenameUtils.getBaseName(url) + extension;
    }

}
