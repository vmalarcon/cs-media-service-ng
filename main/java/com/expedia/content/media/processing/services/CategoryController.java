package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.util.FormattedLogger;
import com.expedia.content.media.processing.pipeline.util.Poker;
import com.expedia.content.media.processing.services.dao.DomainNotFoundException;
import com.expedia.content.media.processing.services.dao.MediaDomainCategoriesDao;
import com.expedia.content.media.processing.services.dao.domain.Category;
import com.expedia.content.media.processing.services.dao.domain.Subcategory;
import com.expedia.content.media.processing.services.util.JSONUtil;
import com.expedia.content.media.processing.services.util.MediaServiceUrl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Web service controller for domain categories.
 */
@RestController
public class CategoryController extends CommonServiceController {

    private static final FormattedLogger LOGGER = new FormattedLogger(CategoryController.class);
    private static final String SKIP_NULL_CATEGORIES = "0";

    @Autowired
    private MediaDomainCategoriesDao mediaDomainCategoriesDao;
    @Value("${cs.poke.hip-chat.room}")
    private String hipChatRoom;
    @Autowired
    private Poker poker;

    /**
     * Media domain categories service. Returns all categories for a domain. Can be filtered by a locale.
     *
     * @param headers Request header contains the requestId and the clientId.
     * @param domainName Domain for which the categories are required.
     * @param localeId Id of the locale to filter in.
     * @return Returns a JSON response for the domain categories request.
     */
    @RequestMapping(value = "/media/v1/domaincategories/{domainName}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, method = RequestMethod.GET)
    @Transactional
    public ResponseEntity<String> domainCategories(final @RequestHeader MultiValueMap<String,String> headers,
            final @PathVariable("domainName") String domainName, final @RequestParam(value = "localeId", required = false) String localeId) {
        final String localePath = (localeId == null) ? "" : "?localeId=" + localeId;
        LOGGER.info("RECEIVED DOMAIN CATEGORIES REQUEST Url={} RequestId={}", MediaServiceUrl.MEDIA_DOMAIN_CATEGORIES.getUrl() + "/" + domainName + localePath,
                getRequestId(headers));
        String response = null;
        try {
            if (localeId != null) {
                if (!StringUtils.isNumeric(localeId) || localeId.length() > 5) {
                    return buildErrorResponse("Requested localeId " + localeId + " must be a number less than 5 characters.",
                            MediaServiceUrl.MEDIA_DOMAIN_CATEGORIES.getUrl() + domainName + localePath, BAD_REQUEST);
                }
            }
            response = getDomainCategories(domainName, localeId);
        } catch (DomainNotFoundException e) {
            LOGGER.error(e, "ERROR ErrorMessage={} DomainName={} RequestId={}", e.getMessage(), domainName, headers.get(REQUEST_ID));
            return buildErrorResponse("Requested resource with ID " + domainName + " was not found.",
                    MediaServiceUrl.MEDIA_DOMAIN_CATEGORIES.getUrl() + "/" + domainName + localePath, NOT_FOUND);
        } catch (Exception ex) {
            LOGGER.error(ex, "ERROR ServiceUrl={} ErrorMessage={} RequestId={} DomainName={}", MediaServiceUrl.MEDIA_DOMAIN_CATEGORIES.getUrl(),
                    ex.getMessage(), headers.get(REQUEST_ID), domainName);
            poker.poke("Media Services failed to process a domainCategories request - RequestId: " + headers.get(REQUEST_ID), hipChatRoom,
                    MediaServiceUrl.MEDIA_DOMAIN_CATEGORIES.getUrl() + "/" + domainName + localePath, ex);
            throw ex;
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * query LCM DB to get the Categories of a Domain
     * if a category has at least one 0 subcategory, the category is not returned
     *
     * @param domain The domain to query
     * @param localeId The localization Id to query by
     * @return JSON message of Categories for the specified Domain and LocaleId without category 0
     * @throws DomainNotFoundException
     */
    private String getDomainCategories(String domain, String localeId) throws DomainNotFoundException {
        final List<Category> domainCategories = mediaDomainCategoriesDao.getMediaCategoriesWithSubCategories(domain, localeId);

        final List<Category> categoriesWithNonNullSubCategories = domainCategories.stream()
                .filter(category -> !containsNullSubCategory(category.getSubcategories()))
                .collect(Collectors.toList());
        return JSONUtil.generateJsonByCategoryList(categoriesWithNonNullSubCategories, domain);
    }

    /**
     * returns true is the category has at least one subcategory 0
     * this is because /media/v1/domaincategories/{domainName} should not display subcategory 0
     *
     * @param subcategories
     * @return
     */
    private boolean containsNullSubCategory(List<Subcategory> subcategories) {
        return subcategories.stream()
                .anyMatch(subcategory -> subcategory.getSubcategoryId().equals(SKIP_NULL_CATEGORIES));
    }
}
