package com.expedia.content.media.processing.services;

import com.expedia.content.metrics.aspects.annotations.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Customer Authentication class to handle authentication exception.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomAuthenticationEntryPoint.class);

    /**
     * Builds an response when http Unauthorized exception happen.
     * Note that the {@code @Counter} annotations introduce aspects from metrics-support
     *
     * @param request                http request
     * @param response               http response
     * @param autheticationException throws when request does not include valid username
     */
    @Counter(name = "unauthorizedRequestCounter")
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException autheticationException)
            throws IOException, ServletException {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, autheticationException.getMessage());
        String userName =
                autheticationException.getAuthentication() != null ? autheticationException.getAuthentication().getPrincipal().toString() : "not provided";
        LOGGER.info("Unauthorized user : username={}", userName);
    }
}
