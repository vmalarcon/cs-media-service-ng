package com.expedia.content.media.processing.services.util;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.amazonaws.util.IOUtils;
import com.amazonaws.util.StringUtils;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.Metadata;
import com.expedia.content.media.processing.pipeline.util.TemporaryWorkFolder;
import com.expedia.content.media.processing.services.dao.domain.Thumbnail;

import expedia.content.solutions.metrics.annotations.Timer;

/**
 * Utility class for resolving file names --ALL PROVIDERS ADDED TO THE ENUM
 * SHOULD USE THE FUNCTION guidProviderNameToFileNameFunction--
 */
public class FileNameUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(FileNameUtil.class);
	private static final String S3_PREFIX = "s3:/";
	private static final String DERIVATIVE_TYPE = "t";
	/**
	 * --THIS FUNCTION SHOULD BE USED FOR ALL FUTURE PROVIDERS ADDED TO THE
	 * ENUM-- This function takes in the ImageMessage with mediaGuid and returns
	 * the fileName in the following format: EID_ProviderName_MediaGUID.jpg
	 */
	private static final Function<ImageMessage, String> guidProviderNameToFileNameFunction = (consumedImageMessage) -> {
		final String fileNameFromMediaGUID = consumedImageMessage.getOuterDomainData().getDomainId() + "_"
				+ consumedImageMessage.getOuterDomainData().getProvider() + "_" + consumedImageMessage.getMediaGuid()
				+ "." + FilenameUtils.getExtension(consumedImageMessage.getFileUrl());
		return fileNameFromMediaGUID;
	};

	/**
	 * This function takes in the ImageMessage and returns the fileName from the
	 * imageMessage if it is already set and if it is not it is set in the
	 * following format: baseNameOfFileURL.jpg
	 */
	private static final Function<ImageMessage, String> fileURLToFileNameFunction = (consumedImageMessage) -> {
		if (StringUtils.isNullOrEmpty(consumedImageMessage.getFileName())) {
			final String fileNameFromFileUrl = FilenameUtils.getBaseName(consumedImageMessage.getFileUrl()) + "."
					+ FilenameUtils.getExtension(consumedImageMessage.getFileUrl());
			return fileNameFromFileUrl;
		}
		return consumedImageMessage.getFileName();
	};

	/**
	 * Mapping enum for LCM:MediaProvider to function logic for resolving a
	 * fileName
	 */
	public enum MediaProvider {
		FREETOBOOK("freetobook", guidProviderNameToFileNameFunction), EPC_INTERNAL_USER(
				"epc internal user"), EPC_EXTERNAL_USER("epc external user"), EPC_LEGACY("epc legacy"), MOBILE(
						"mobile"), EEM_MIGRATION("eem migration"), SCORE("score"), OTHER("other"), GIGWALK(
								"gigwalk"), TV_TRIP("tv trip"), SMART_SHOOT("smart shoot"), CONCIERGE_360(
										"concierge 360"), CARIBWEBSERVICE("caribwebservice"), ELONG(
												"elong"), EASY_VIEW_MEDIA("easy view media"), EVOLVING_PHOTOGRAPHY(
														"evolving photography"), FCE_DESIGN("fce design"), FUSION(
																"360 fusion"), HD_MEDIA("hd media"), HOTEL_PROVIDED(
																		"hotel provided"), ICE_PORTAL(
																				"ice portal"), MOVING_PICTURES(
																						"moving pictures"), EYENAV_360(
																								"eyenav 360"), NTT(
																										"ntt"), PANOMATICS_ASIA(
																												"panomatics asia"), PREVU(
																														"prevu"), PROSEARCHPLUS(
																																"prosearchplus"), RAINBIRD_PHOTOGRAPHY(
																																		"rainbird photography"), REAL_BIG_TOURS(
																																				"real big tours"), RTV_INC(
																																						"rtv, inc."), SEE_VIRTUAL_360(
																																								"see virtual 360"), SHOW_HOTEL(
																																										"show hotel"), TESTURE(
																																												"testure"), VFMLEONARDO(
																																														"vfmleonardo"), VISUAL_HOTELS(
																																																"visual hotels"), VISION_ANGULAR(
																																																		"vision angular"), VR_NATIONAL(
																																																				"vr national"), VRX_STUDIOS(
																																																						"vrx studios"), WORLDGUIDE(
																																																								"360 worldguide"), HOTELS(
																																																										"hotels"), EXPEDIA(
																																																												"expedia"), SPENCER_CREATIVE(
																																																														"spencer creative"), NIKHILESH_HAVAL(
																																																																"nikhilesh haval"), TITANIO(
																																																																		"titanio"), DIGITAL_CREATIVITY_INC(
																																																																				"100 digital creativity, inc"), PEGASUS(
																																																																						"pegasus"), HOTELBEDS(
																																																																								"hotelbeds"), JUMBOTOUR(
																																																																										"jumbotour"), HOMEAWAY(
																																																																												"homeaway"), WOTIF(
																																																																														"wotif"), EVIIVO(
																																																																																"eviivo"), PRODUCT_API_TEST(
																																																																																		"productapi-test"), ORBITZ(
																																																																																				"orbitz"), REPLACEPROVIDER(
																																																																																						"replaceprovider");

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
			return Stream.of(MediaProvider.values())
					.filter(mediaProvider -> mediaProvider.getName().equals(providerName.toLowerCase())).findFirst();
		}
	}

	/**
	 * resolve FileName by the MediaProvider name
	 *
	 * @param imageMessage
	 */
	public static String resolveFileNameByProvider(ImageMessage imageMessage) {
		final String providerName = imageMessage.getOuterDomainData().getProvider();
		final Optional<MediaProvider> mediaProvider = MediaProvider.findMediaProviderByProviderName(providerName);
		if (mediaProvider.isPresent()) {
			return mediaProvider.get().function.apply(imageMessage);
		}
		return guidProviderNameToFileNameFunction.apply(imageMessage);
	}

	/**
	 * Retrieve basic metadatas for a file.
	 * 
	 * @param sourcePath
	 *            source path.
	 * @return
	 */
	public static Metadata getBasicMetadata(Path sourcePath) {
		BufferedImage bufferedImage;
		int height = 0;
		int width = 0;
		Long sourceSize = 0L;
		if (sourcePath != null) {
			try {
				bufferedImage = ImageIO.read(sourcePath.toFile());
				height = bufferedImage.getHeight();
				width = bufferedImage.getWidth();
				LOGGER.debug("Media width: " + width);
				sourceSize = sourcePath.toFile().length();
				LOGGER.debug("Media size: " + sourceSize);
				return Metadata.builder().fileSize((int) sourcePath.toFile().length()).width(width).height(height)
						.build();
			} catch (Exception e) {
				LOGGER.debug("Unable to extract the metadas for the given url file: " + sourcePath.getFileName());
			}
		}
		return null;
	}

	public static Path getSourcePath(final String fileUrl, final String guid, TemporaryWorkFolder workFolder,
			ResourceLoader resourceLoader) throws IOException {
		if (fileUrl.toLowerCase(Locale.US).startsWith(S3_PREFIX)) {
			return fetchS3(fileUrl, guid, workFolder.getWorkPath(), resourceLoader);
		} else {
			return fetchUrl(fileUrl, guid, workFolder.getWorkPath());
		}
	}

	/**
	 * Build a thumbnail from the given path
	 * 
	 * @param thumbnailPath
	 *            path for the thumbnail.
	 * @param url
	 *            thumbnail location url;
	 * @param sourcePath
	 *            path for the source image.
	 * @return
	 */
	public static Thumbnail buildThumbnail(Path thumbnailPath, String url, Path sourcePath) {

		final Metadata sourceMetadata = FileNameUtil.getBasicMetadata(sourcePath);
		if (thumbnailPath == null) {
			return Thumbnail.builder().sourceMetadata(sourceMetadata).build();
		}
		final Metadata thumbnailMetadata = FileNameUtil.getBasicMetadata(thumbnailPath);
		return Thumbnail.builder().thumbnailMetadata(thumbnailMetadata).sourceMetadata(sourceMetadata).location(url)
				.type(DERIVATIVE_TYPE).build();
	}

	/**
	 * Fetch HTTP URL. Starts with {@code http://} or {@code https://}.
	 *
	 * @param url
	 *            Image URL to fetch.
	 * @param guid
	 *            GUID for the image.
	 * @param workPath
	 *            Temporary working folder to use for downloading the image.
	 * @return Path where the image is downloaded.
	 * @throws IOException
	 *             When unable to fetch the HTTP URL.
	 */
	@Timer(name = "FetchHttpUrlTimer")
	private static Path fetchUrl(final String url, final String guid, final Path workPath) throws IOException {
		LOGGER.debug("Fetching HTTP URL -> " + url);
		final URL validUrl = new URL(url);
		final Path filePath = Paths.get(workPath.toString(), guid + ".jpg");
		try (FileOutputStream fileOutputStream = new FileOutputStream(filePath.toString())) {
			IOUtils.copy(validUrl.openStream(), fileOutputStream);
		}
		LOGGER.debug("Fetched HTTP URL -> " + url);
		return filePath;
	}

	/**
	 * Fetch S3 URL. Starts with {@code s3://}.
	 *
	 * @param url
	 *            Image URL to fetch from S3.
	 * @param guid
	 *            GUID for the image.
	 * @param workPath
	 *            Temporary working folder to use for downloading the image.
	 * @return Path where the image is downloaded.
	 * @throws IOException
	 *             When unable to fetch the S3 URL.
	 */
	@Timer(name = "FetchS3UrlTimer")
	private static Path fetchS3(final String url, final String guid, final Path workPath, ResourceLoader resourceLoader)
			throws IOException {
		LOGGER.debug("Fetching S3 URL -> " + url);
		final Resource resource = resourceLoader.getResource(url);
		final Path filePath = Paths.get(workPath.toString(), guid + ".jpg");
		try (FileOutputStream fileOutputStream = new FileOutputStream(filePath.toString())) {
			IOUtils.copy(resource.getInputStream(), fileOutputStream);
		}
		LOGGER.debug("Fetched S3 URL -> " + url);
		return filePath;
	}
}
