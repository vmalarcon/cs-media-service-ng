package com.expedia.content.media.processing.services.util;

import com.amazonaws.util.StringUtils;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.services.dao.domain.Media;
import org.apache.commons.io.FilenameUtils;

/**
 * Utility class for resolving file names
 * --ALL PROVIDERS ADDED TO THE ENUM SHOULD USE THE FUNCTION guidProviderNameToFileName--
 */
public class FileNameUtil {
    private FileNameUtil () {
        /** no-on **/
    }

    /**
     * --THIS METHOD SHOULD BE USED FOR ALL FUTURE PROVIDERS ADDED TO THE ENUM--
     * This method takes in the ImageMessage with mediaGuid and returns the fileName in the following format:
     * EID_ProviderName_MediaGUID.jpg
     *
     */
    private static final String guidProviderNameToFileName(ImageMessage consumedImageMessage) {
        final String fileNameFromMediaGUID = consumedImageMessage.getOuterDomainData().getDomainId() + "_" + StringUtils.replace(consumedImageMessage.getOuterDomainData().getProvider(), " ", "")
                + "_" + consumedImageMessage.getMediaGuid() + "." + FilenameUtils.getExtension(consumedImageMessage.getFileUrl());
        return fileNameFromMediaGUID;
    };

    /**
     * Mapping enum for LCM:MediaProvider
     */
    public enum MediaProvider {
        DESPEGAR("despegar"),
        FREETOBOOK("freetobook"),
        EPC_INTERNAL_USER("epc internal user"),
        EPC_EXTERNAL_USER("epc external user"),
        SCORE("score"),
        EPC_LEGACY("epc legacy"),
        MOBILE("mobile"),
        EEM_MIGRATION("eem migration"),
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
        PRODUCT_API_TEST("productapi-test"),
        ORBITZ("orbitz"),
        REPLACEPROVIDER("replaceprovider");

        private final String name;

        MediaProvider(String mediaProvider) {
            this.name = mediaProvider;
        }

        private String getName() {
            return name;
        }
    }


    /**
     * resolve FileName by the MediaProvider name
     *
     * @param imageMessage
     */
    public static String resolveFileName(ImageMessage imageMessage) {
        return guidProviderNameToFileName(imageMessage);
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
