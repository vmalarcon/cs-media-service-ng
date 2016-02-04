package com.expedia.content.media.processing.services;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.expedia.content.media.processing.services.dao.Category;
import com.expedia.content.media.processing.services.dao.DomainNotFoundException;
import com.expedia.content.media.processing.services.dao.MediaDomainCategoriesDao;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.expedia.content.media.processing.services.util.MediaServiceUrl;

/**
 * Web service controller for domain categories.
 */
@RestController
public class CategoryController extends CommonServiceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryController.class);

    @Autowired
    private MediaDomainCategoriesDao mediaDomainCategoriesDao;

    /**
     * Media domain categories service. Returns all categories for a domain. Can be filtered by a locale.
     *
     * @param headers Request header contains the requestId and the clientId.
     * @param domainName Domain for which the categories are required.
     * @param localeId Id of the locale to filter in.
     * @return Returns a JSON response for the domain categories request.
     */
    @RequestMapping(value = "/media/v1/domaincategories/{domainName}", method = RequestMethod.GET)
    public ResponseEntity<String> domainCategories(final @RequestHeader MultiValueMap<String, String> headers,
                                                   final @PathVariable("domainName") String domainName,
                                                   final @RequestParam(value = "localeId", required = false) String localeId) {
        final String localePath = (localeId == null) ? "" : "?localeId=" + localeId;
        LOGGER.info("RECEIVED REQUEST - url=[{}][{}][{}], requestId=[{}]", MediaServiceUrl.MEDIA_DOMAIN_CATEGORIES.getUrl(), domainName, localePath,
                getRequestId(headers));
        try {
            final String response = getDomainCategories(domainName, localeId);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (DomainNotFoundException e) {
            LOGGER.error("ERROR - JSONMessage=[{}], requestId=[{}]", e, headers.get(REQUEST_ID));
            return buildErrorResponse("Requested resource with ID " + domainName + " was not found.",
                    MediaServiceUrl.MEDIA_DOMAIN_CATEGORIES.getUrl() + domainName + localePath, NOT_FOUND);
        }
    }

    /**
     * query LCM DB to get the Categories of a Domain
     *
     * @param domain The domain to query
     * @param localeId The localization Id to query by
     * @return JSON message of Categories for the specified Domain and LocaleId
     * @throws DomainNotFoundException
     */
    private String getDomainCategories(String domain, String localeId) throws DomainNotFoundException {
        final List<Category> domainCategories = mediaDomainCategoriesDao.getMediaCategoriesWithSubCategories(domain, localeId);
        return JSONUtil.generateJsonByCategoryList(domainCategories, domain);
    }

}
