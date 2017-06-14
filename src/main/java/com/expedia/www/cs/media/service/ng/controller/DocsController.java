package com.expedia.www.cs.media.service.ng.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/docs")
public class DocsController {

    @RequestMapping("/")
    public String home() {
        return "redirect:/docs/index.html";
    }

    @RequestMapping("/swagger")
    public String swagger() {
        return "redirect:/swagger-ui.html";
    }

    @RequestMapping("/static")
    public String docs() {
        return "redirect:/docs/index.html";
    }
}
