package com.expedia.content.media.processing.services.init;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebServlet(name = "isActive", value = "/isActive", loadOnStartup = 1)
public class IsAlwaysActiveServlet extends HttpServlet {

    /**
     * If application is active then the Active status (HTTP code 200) is returned; otherwise inactive status is returned (Http code 500).
     * @param request
     * @param response
     * @throws IOException
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "no-cache,no-store,max-age=0");
        response.setStatus(200);
        response.getWriter().write("ACTIVE");
    }

}
