package com.expedia.content.media.processing.services;

import static com.expedia.content.media.processing.pipeline.domain.Domain.LODGING;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.expedia.content.media.processing.services.dao.domain.Thumbnail;
import com.expedia.content.media.processing.services.reqres.TempDerivativeMessage;
import com.expedia.content.media.processing.services.util.FileNameUtil;

import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Component;

import com.amazonaws.util.IOUtils;
import com.expedia.content.media.processing.pipeline.domain.Metadata;
import com.expedia.content.media.processing.pipeline.util.LodgingUtil;
import com.expedia.content.media.processing.pipeline.util.TemporaryWorkFolder;
import com.google.common.base.Joiner;

import expedia.content.solutions.metrics.annotations.Timer;

/**
 * Thumbnail processing related functionality.
 */
@Component
public class ThumbnailProcessor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ThumbnailProcessor.class);
    
    private static final int THUMBNAIL_WIDTH = 180;
    private static final int THUMBNAIL_HEIGHT = 180;
    private static final String S3_PREFIX = "s3:/";
    private static final String DERIVATIVE_TYPE = "t";
    
    @Autowired
    private ResourceLoader resourceLoader;
    @Value("${service.temp.work.folder}")
    private String tempWorkFolder;
    @Value("${media.thumbnail.bucket.region}")
    private String regionName;
    @Value("${service.thumbnail.output.location}")
    private String thumbnailOuputLocation;
    
    /**
     * Create the thumbnail and save it in S3.
     *
     * @param url Input URL for the image (HTTP and s3 supported).
     * @param guid GUID to use for the image.
     * @param domain Domain the image image belongs to.
     * @param domainId DomainId for the image.
     * @return URL Path for the resulting thumbnail on S3.
     */
    public Thumbnail createThumbnail(final String url, final String guid, final String domain, final String domainId) {
        return createGenericThumbnail(url, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, "0", guid, domain, domainId);
    }
    
    /**
     * Create a Temporary Derivative and save it in S3
     *
     * @param tempDerivativeMessage
     * @return URL Path for the resulting temporary derivative on S3
     */
    public String createTempDerivativeThumbnail(TempDerivativeMessage tempDerivativeMessage) {
        final String guid = UUID.randomUUID().toString();
        
        return createGenericThumbnail(tempDerivativeMessage.getFileUrl(), tempDerivativeMessage.getWidth(),
                tempDerivativeMessage.getHeight(), tempDerivativeMessage.getRotation(), guid, "tempderivative", null).getLocation();
    }
    
    /**
     * Generic method for creating a Thumbnail for an imageMessage or creating a temporary derivative
     *
     * @param fileUrl the url of the sent file
     * @param width the width of the returned thumbnail
     * @param height the height of the returned thumbnail
     * @param rotation the rotation of the returned thumbnail
     * @param guid the guid of the message
     * @param domain domain of an imageMessage
     * @param domainId domainId of an imageMessage
     * @return thumbnailUrl the url of the generated thumbnail
     */
    private Thumbnail createGenericThumbnail(final String fileUrl, final int width, final int height, final String rotation, final String guid,
            final String domain, final String domainId) {
            
        LOGGER.debug("Creating thumbnail url=[{}] guid=[{}]", fileUrl, guid);
        String thumbnailUrl;
        Path thumbnailPath;
        Path sourcePath;
        Thumbnail thumbnail = null;
        final Path workPath = Paths.get(tempWorkFolder);
        try (TemporaryWorkFolder workFolder = new TemporaryWorkFolder(workPath)) {
            if (fileUrl.toLowerCase(Locale.US).startsWith(S3_PREFIX)) {
                sourcePath = fetchS3(fileUrl, guid, workFolder.getWorkPath());
            } else {
                sourcePath = fetchUrl(fileUrl, guid, workFolder.getWorkPath());
            }
            thumbnailPath = generateThumbnail(sourcePath, width, height, (rotation == null) ? "0" : rotation, (domainId != null));
            thumbnailUrl = computeS3thumbnailPath(thumbnailPath, guid, domain, domainId);
            LOGGER.debug("Writing thumbnail: " + thumbnailUrl);
            final WritableResource writableResource = (WritableResource) resourceLoader.getResource(thumbnailUrl);
            try (OutputStream out = writableResource.getOutputStream(); FileInputStream file = new FileInputStream(thumbnailPath.toFile())) {
                out.write(IOUtils.toByteArray(file));
                thumbnail = buildThumbnail(thumbnailPath, thumbnailUrl, sourcePath);
            }
            LOGGER.debug("Wrote thumbnail: " + thumbnailUrl);
            thumbnailUrl = thumbnailUrl.replaceFirst(S3_PREFIX, "https://s3-" + this.regionName + ".amazonaws.com");
        } catch (Exception e) {
            LOGGER.error("Unable to generate thumbnail with url: " + fileUrl, e);
            throw new RuntimeException("Unable to generate thumbnail with url: " + fileUrl + " and GUID: " + guid, e);
        }
        LOGGER.debug("Created thumbnail url=[{}] guid=[{}]", fileUrl, guid);
        return thumbnail;
    }
    
    /**
     * Fetch HTTP URL. Starts with {@code http://} or {@code https://}.
     *
     * @param url Image URL to fetch.
     * @param guid GUID for the image.
     * @param workPath Temporary working folder to use for downloading the image.
     * @return Path where the image is downloaded.
     * @throws IOException When unable to fetch the HTTP URL.
     */
    @Timer(name = "FetchHttpUrlTimer")
    private Path fetchUrl(final String url, final String guid, final Path workPath) throws IOException {
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
     * @param url Image URL to fetch from S3.
     * @param guid GUID for the image.
     * @param workPath Temporary working folder to use for downloading the image.
     * @return Path where the image is downloaded.
     * @throws IOException When unable to fetch the S3 URL.
     */
    @Timer(name = "FetchS3UrlTimer")
    private Path fetchS3(final String url, final String guid, final Path workPath) throws IOException {
        LOGGER.debug("Fetching S3 URL -> " + url);
        final Resource resource = resourceLoader.getResource(url);
        final Path filePath = Paths.get(workPath.toString(), guid + ".jpg");
        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath.toString())) {
            IOUtils.copy(resource.getInputStream(), fileOutputStream);
        }
        LOGGER.debug("Fetched S3 URL -> " + url);
        return filePath;
    }
    
    /**
     * Generates the thumbnail using ImageMagick.
     * 
     * @param sourcePath Locally saved image to convert to thumbnail.
     * @param width
     * @param height
     * @param rotation @return Path to the thumbnail stored locally.
     * @param addThumbnailExtension boolean value to determine wheter to a a _t extension to the file
     * @throws IM4JavaException Thrown when the thumbnail generation fails.
     * @throws InterruptedException Thrown if the convert command fails.
     * @throws IOException Thrown if the convert command fails to read or write.
     */
    @Timer(name = "TumbnailGenerationTimer")
    private Path generateThumbnail(final Path sourcePath, int width, int height, String rotation, boolean addThumbnailExtension)
            throws IOException, InterruptedException, IM4JavaException {
        LOGGER.debug("Generating thumbnail -> " + sourcePath);
        Path thumbnailPath = Paths.get(sourcePath.toString());
        if (addThumbnailExtension) {
            thumbnailPath = Paths.get(sourcePath.toString().replace(".jpg", "_t.jpg"));
        }
        final IMOperation operation = new IMOperation();
        operation.limit("thread");
        operation.addRawArgs("2");
        operation.units("PixelsPerInch");
        operation.resize(width, height, "!");
        operation.rotate(Double.valueOf(rotation));
        operation.addImage(sourcePath.toString());
        operation.addImage(thumbnailPath.toString());
        
        final ConvertCmd convertCmd = new ConvertCmd();
        LOGGER.debug("convert.thumb> {}", "convert " + operation.getCmdArgs().toString().replaceAll(",", ""));
        convertCmd.run(operation);
        verifyCommandResult(convertCmd);
        LOGGER.debug("Generated thumbnail" + sourcePath);
        return thumbnailPath;
    }
    
    /**
     * Verify the result of an ImageMagick execution
     *
     * @param convertCommand ImageMagick command executed.
     * @throws IM4JavaException thrown when any error/warning is returned form the command execution
     */
    private static void verifyCommandResult(final ConvertCmd convertCommand) throws IM4JavaException {
        final List<String> errorTexts = convertCommand.getErrorText();
        final String errorText = Joiner.on('\n').join(errorTexts);
        LOGGER.debug("command results=[{}]", errorText);
        if (!org.apache.commons.lang.StringUtils.isEmpty(errorText)) {
            throw new IM4JavaException("IM Exception. Please see log for details");
        }
    }
    
    /**
     * Builds the S3 storage location and file name for the source file.
     * 
     * @param thumbnailPath The thumbnail work file.
     * @param guid The assigned media guid to the media file.
     * @param domain The domain the media belongs to.
     * @param domainId The id, in the domain, of the item the media belongs to.
     * @return The storage location of the thumbnail.
     */
    private String computeS3thumbnailPath(final Path thumbnailPath, final String guid, final String domain, final String domainId) {
        if (domainId == null) {
            return thumbnailOuputLocation + domain + "/" + guid;
        } else {
            final String fileName = thumbnailPath.getFileName().toString();
            final String domainPath = LODGING.name().equalsIgnoreCase(domain) ? LodgingUtil.buildFolderPath(Integer.parseInt(domainId)) : domainId;
            return thumbnailOuputLocation + domain.toLowerCase(Locale.US) + domainPath + domainId + (fileName.contains(guid) ? "" : "_" + guid) + "_"
                    + fileName;
        }
    }
    
    /**
     * Build a thumbnail from the given path
     * 
     * @param thumbnailPath path for the thumbnail.
     * @param url thumbnail location url;
     * @param sourcePath path for the source image.
     * @return
     */
    private Thumbnail buildThumbnail(Path thumbnailPath, String url, Path sourcePath) {
        final Metadata thumbnailMetadata = FileNameUtil.getBasicMetadata(thumbnailPath);
        final Metadata sourceMetadata = FileNameUtil.getBasicMetadata(sourcePath);
        if (thumbnailMetadata != null) {
            return Thumbnail.builder().thumbnailMetadata(thumbnailMetadata)
                    .sourceMetadata(sourceMetadata)
                    .location(url)
                    .type(DERIVATIVE_TYPE)
                    .build();
        }
        return null;
    }
}
