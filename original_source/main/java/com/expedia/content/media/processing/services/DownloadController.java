package com.expedia.content.media.processing.services;

import com.amazonaws.util.IOUtils;
import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import expedia.content.solutions.metrics.annotations.Counter;
import expedia.content.solutions.metrics.annotations.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.stream.Stream;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;

/**
 * Web service controller for media resources.
 */
@RestController
public class DownloadController extends CommonServiceController {
    private static final FormattedLogger LOGGER = new FormattedLogger(DownloadController.class);

    @Autowired
    private ResourcePatternResolver resourcePatternResolver;

    @ResponseBody
    @RequestMapping(value = "/media/s3/download", method = RequestMethod.GET, produces = MediaType.IMAGE_JPEG_VALUE)
    @Counter(name = "S3DownloadCounter")
    @Timer(name = "S3DownloadTimer")
    public ResponseEntity<byte[]> download(@RequestParam("url") String fromUrl,
                                           @RequestParam(value = "contentType", required = false, defaultValue = "image/jpeg") String contentType) throws Exception {
        LOGGER.info("BEGIN Url={} ContentType={}", fromUrl, contentType);
        try {
            final Resource[] resources = resourcePatternResolver.getResources(fromUrl);
            if (Stream.of(resources).filter(Resource::exists).count() > 1) {
                LOGGER.error("Multiple resources matched Url={} Resources={}", fromUrl, Stream.of(resources).count());
                return new ResponseEntity<>(CONFLICT);
            }

            if (Stream.of(resources).noneMatch(Resource::exists)) {
                LOGGER.error("Resource not found Url={}", fromUrl);
                return new ResponseEntity<>(NOT_FOUND);
            }

            final InputStream streamFrom = fetchS3Resource(resources[0]);

            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.valueOf(contentType));

            return new ResponseEntity<>(IOUtils.toByteArray(streamFrom), headers, HttpStatus.OK);
        } catch (Exception e) {
            LOGGER.error(e, "FAILURE Processing image ErrorMessage={} Url={} ContentType={}", e.getMessage(), fromUrl, contentType);
        } finally {
            LOGGER.info("END Url={} ContentType={}", fromUrl, contentType);
        }
        return new ResponseEntity<>(NO_CONTENT);
    }

    @Counter(name = "FetchedS3Resource")
    public InputStream fetchS3Resource(Resource resource) throws Exception {
        LOGGER.info("Opening S3 Resource Url={}", resource.getURL());
        return resource.getInputStream();
    }
}
