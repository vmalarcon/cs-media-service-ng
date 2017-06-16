package com.expedia.content.media.processing.services;

import com.amazonaws.util.IOUtils;
import com.expedia.content.media.processing.pipeline.domain.ImageMessage;
import com.expedia.content.media.processing.pipeline.domain.Metadata;
import com.expedia.content.media.processing.pipeline.domain.DerivativeType;
import com.expedia.content.media.processing.pipeline.domain.Image;
import com.expedia.content.media.processing.pipeline.domain.ResizeCrop;
import com.expedia.content.media.processing.pipeline.domain.ResizeMethod;
import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.pipeline.util.TemporaryWorkFolder;
import com.expedia.content.media.processing.services.dao.domain.Thumbnail;
import com.expedia.content.media.processing.services.reqres.TempDerivativeMessage;
import com.expedia.content.media.processing.services.util.ImageUtil;
import com.google.common.base.Joiner;
import expedia.content.solutions.metrics.annotations.Timer;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static com.expedia.content.media.processing.services.util.URLUtil.patchURL;

/**
 * Thumbnail processing related functionality.
 */
@Component
public class ThumbnailProcessor {

    private static final FormattedLogger LOGGER = new FormattedLogger(ThumbnailProcessor.class);

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
     * @param imageMessage incoming message.
     * @return
     */
    public Thumbnail createThumbnail(ImageMessage imageMessage) {
        final Integer rotation = (imageMessage.getRotation() == null) ? null : Integer.parseInt(imageMessage.getRotation());
        return createGenericThumbnail(imageMessage.getFileUrl(), THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, rotation,
                imageMessage.getMediaGuid(), imageMessage.getOuterDomainData().getDomain().getDomain(), imageMessage.getOuterDomainData().getDomainId());
    }

    /**
     * Create a Temporary Derivative and save it in S3.
     *
     * @param tempDerivativeMessage
     * @return URL Path for the resulting temporary derivative on S3.
     */
    public String createTempDerivativeThumbnail(TempDerivativeMessage tempDerivativeMessage) {
        final String guid = UUID.randomUUID().toString();
        final Integer rotation = (tempDerivativeMessage.getRotation() == null ? null : Integer.valueOf(tempDerivativeMessage.getRotation()));
        return createGenericThumbnail(patchURL(tempDerivativeMessage.getFileUrl()), tempDerivativeMessage.getWidth(), tempDerivativeMessage.getHeight(),
                rotation, guid, "tempderivative", null).getLocation();
    }

    /**
     * Generic method for creating a Thumbnail for an imageMessage or creating a
     * temporary derivative.
     *
     * @param fileUrl the url of the sent file.
     * @param width the width of the returned thumbnail.
     * @param height the height of the returned thumbnail.
     * @param rotation the rotation of the returned thumbnail.
     * @param guid the guid of the message.
     * @param domain domain of an imageMessage.
     * @param domainId domainId of an imageMessage.
     * @return thumbnailUrl the url of the generated thumbnail.
     */
    @Timer(name = "ThumbnailGenerationTimer")
    private Thumbnail createGenericThumbnail(final String fileUrl, final int width, final int height, final Integer rotation, final String guid,
                                             final String domain, final String domainId) {

        LOGGER.debug("CREATE THUMBNAIL FileUrl={} MediaGuid={}", fileUrl, guid);
        String thumbnailUrl;
        Path thumbnailPath;
        Path sourcePath;
        Thumbnail thumbnail = null;

        final Path workPath = Paths.get(tempWorkFolder);
        try (TemporaryWorkFolder workFolder = new TemporaryWorkFolder(workPath)) {
            sourcePath = fetchUrl(patchURL(fileUrl), guid, workFolder);
            thumbnailPath = generateThumbnail(sourcePath, width, height, (rotation == null) ? 0 : rotation);
            thumbnailUrl = computeS3thumbnailPath(guid, domain, domainId);
            LOGGER.debug("WRITING THUMBNAIL ThumbnailUrl={}", thumbnailUrl);
            final WritableResource writableResource = (WritableResource) resourceLoader.getResource(thumbnailUrl);
            try (OutputStream out = writableResource.getOutputStream(); FileInputStream file = new FileInputStream(thumbnailPath.toFile())) {
                out.write(IOUtils.toByteArray(file));
                LOGGER.debug("WROTE THUMBNAIL ThumbnailUrl={}", thumbnailUrl);
                thumbnailUrl = thumbnailUrl.replaceFirst(S3_PREFIX, "https://s3-" + this.regionName + ".amazonaws.com");
                thumbnail = buildThumbnail(thumbnailPath, thumbnailUrl, sourcePath);
            }
        } catch (Exception e) {
            LOGGER.error(e, "Unable to generate thumbnail FileUrl={}", fileUrl);
            throw new RuntimeException("Unable to generate thumbnail with url: " + fileUrl + " and GUID: " + guid, e);
        }
        LOGGER.debug("CREATED THUMBNAIL FileUrl={} MediaGuid={}", fileUrl, guid);
        return thumbnail;
    }

    /**
     * Generates the thumbnail using ImageMagick.
     * The Order of the operation (Important):
     * 1) Rotate the source image (If the request has a rotation)
     * 2) Resize the image so that the aspect ratio stays constant and the width and height of the image
     * are both greater than or equal to the width and height input
     * 3) Crop (extent) the rotated image
     * 
     * @param sourcePath Locally saved image to convert to thumbnail.
     * @param width Desired width of the thumbnail image.
     * @param height Desired height of the thumbnail image.
     * @param rotation Rotation to apply to the thumbnail image. 
     * @return Path to the thumbnail stored locally.
     * @throws IM4JavaException Thrown when the thumbnail generation fails.
     * @throws InterruptedException Thrown if the convert command fails.
     * @throws IOException Thrown if the convert command fails to read or write.
     */
    private Path generateThumbnail(final Path sourcePath, int width, int height, Integer rotation) throws IOException, InterruptedException,
            IM4JavaException, URISyntaxException {
        LOGGER.debug("GENERATING THUMBNAIL SourcePath={}", sourcePath);

        if (rotation != 0) {
            rotateImage(sourcePath, rotation);
        }
        final Path thumbnailPath = Paths.get(sourcePath.toString());
        final ResizeCrop resizeCrop = scaleThumbnail(thumbnailPath.toUri().toURL(), height, width);
        final IMOperation operation = new IMOperation();
        operation.limit("thread");
        operation.addRawArgs("2");
        operation.units("PixelsPerInch");
        operation.background("black");
        operation.gravity("center");
        operation.resize(resizeCrop.getWidth(), resizeCrop.getHeight());
        operation.extent(width, height);
        operation.orient("top-left");
        operation.addImage(sourcePath.toString());
        operation.addImage(thumbnailPath.toString());

        final ConvertCmd convertCmd = new ConvertCmd();
        LOGGER.debug("CONVERT THUMBNAIL Command={}", "convert " + operation.getCmdArgs().toString().replaceAll(",", ""));
        convertCmd.run(operation);
        verifyCommandResult(convertCmd);
        LOGGER.debug("GENERATED THUMBNAIL SourcePath={}", sourcePath);
        return thumbnailPath;
    }

    private void rotateImage(final Path sourcePath, Integer rotation) throws IOException, InterruptedException,
            IM4JavaException, URISyntaxException {
        final IMOperation operation = new IMOperation();
        operation.limit("thread");
        operation.addRawArgs("2");
        operation.units("PixelsPerInch");
        operation.background("black");
        operation.gravity("center");
        operation.rotate(Double.valueOf(rotation));
        operation.orient("top-left");
        operation.addImage(sourcePath.toString());
        operation.addImage(sourcePath.toString());
        final ConvertCmd convertCmd = new ConvertCmd();
        LOGGER.debug("CONVERT THUMBNAIL Command={}", "convert " + operation.getCmdArgs().toString().replaceAll(",", ""));
        convertCmd.run(operation);
        verifyCommandResult(convertCmd);
    }

    /**
     * Scales the image for the resulting thumbnail. Since ResizeMethod is set to FIXED scaleThumbnail always
     * returns CropInstruction even though the image does not need to be cropped.
     * 
     * @param imagePath The path of the work image to manipulate.
     * @param width Desired width of the thumbnail image.
     * @param height Desired height of the thumbnail image.
     * @return The resize and cropping instructions for the thumbnail.
     */
    private ResizeCrop scaleThumbnail(URL imagePath, int height, int width) throws URISyntaxException {
        final Image image = new Image(imagePath);
        final DerivativeType derivativeType = new DerivativeType();
        derivativeType.setHeight(height);
        derivativeType.setWidth(width);
        derivativeType.setResizeMethod(ResizeMethod.FIXED);
        return new ResizeCrop(image.getHeight(), image.getWidth(), derivativeType);
    }

    /**
     * Verify the result of an ImageMagick execution.
     *
     * @param convertCommand ImageMagick command executed.
     * @throws IM4JavaException thrown when any error/warning is returned form the command execution.
     */
    private static void verifyCommandResult(final ConvertCmd convertCommand) throws IM4JavaException {
        final List<String> errorTexts = convertCommand.getErrorText();
        final String errorText = Joiner.on('\n').join(errorTexts);
        LOGGER.debug("Command Results ErrorMessage={}", errorText);
        if (!org.apache.commons.lang.StringUtils.isEmpty(errorText)) {
            throw new IM4JavaException("IM Exception. Please see log for details");
        }
    }

    /**
     * Builds the S3 storage location and file name for the source file.
     * 
     * @param guid The assigned media guid to the media file.
     * @param domain The domain the media belongs to.
     * @param domainId The id, in the domain, of the item the media belongs to.
     * @return The storage location of the thumbnail.
     */
    private String computeS3thumbnailPath(final String guid, final String domain, final String domainId) {
        if (domainId == null) {
            return thumbnailOuputLocation + domain + "/" + guid + ".jpg";
        } else {
            return thumbnailOuputLocation + domain + "/" + domainId + "/" + guid + ".jpg";
        }
    }

    /**
     * Retrieve the source path base on the fileUrl.
     * 
     * @param fileUrl Image URL to fetch.
     * @param guid GUID for the image.
     * @param workFolder Temporary working folder to use for downloading the image.
     * @return Path where the image is downloaded.
     * @throws IOException When unable to fetch the URL
     */
    @Timer(name = "FetchUrlTimer")
    private Path fetchUrl(final String fileUrl, final String guid, TemporaryWorkFolder workFolder) throws IOException {
        final Resource resource = resourceLoader.getResource(fileUrl);
        LOGGER.debug("FETCHING URL FileUrl={}", fileUrl);
        final Path filePath = Paths.get(workFolder.getWorkPath().toString(), guid + ".jpg");
        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath.toString())) {
            IOUtils.copy(resource.getInputStream(), fileOutputStream);
        }
        LOGGER.debug("FETCHED URL FileUrl={}", fileUrl);
        return filePath;
    }

    /**
     * Build a thumbnail from the given path
     * 
     * @param thumbnailPath path for the thumbnail.
     * @param url thumbnail location url;
     * @param sourcePath path for the source image.
     * @return
     */
    private Thumbnail buildThumbnail(Path thumbnailPath, String url, Path sourcePath) throws Exception {
        final Metadata sourceMetadata = ImageUtil.getBasicImageMetadata(sourcePath);
        final Metadata thumbnailMetadata = ImageUtil.getBasicImageMetadata(thumbnailPath);
        return Thumbnail.builder().thumbnailMetadata(thumbnailMetadata).sourceMetadata(sourceMetadata).location(url).type(DERIVATIVE_TYPE).build();
    }
}
