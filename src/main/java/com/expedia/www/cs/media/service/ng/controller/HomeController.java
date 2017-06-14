package com.expedia.www.cs.media.service.ng.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {

    @RequestMapping("/")
    public String home() {
        return "redirect:/docs/index.html";
    }
}
