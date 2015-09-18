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
 * Customer Authentication class to intercept exception.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomAuthenticationEntryPoint.class);

    @Counter(name = "UnauthorizedRequestCounter")
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException auth)
            throws IOException, ServletException {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized.");
        LOGGER.info("Unauthorized exception222");

    }
}
