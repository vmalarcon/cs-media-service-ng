package com.expedia.www.cs.media.service.ng.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import expedia.content.solutions.metrics.annotations.Counter;
import lombok.Builder;
import lombok.Data;

@RestController
@RequestMapping("/service")
public class HelloController {

    @RequestMapping(value = "/hello/{name}", method = RequestMethod.GET, produces = "application/json")
    @Counter(name = "HelloWorldNameCounter")
    public ResponseEntity<Response> hello(@PathVariable String name) {
        return ResponseEntity.ok(Response.builder().message("Hello - " + name).success(true).build());
    }

    @Data
    @Builder
    private static class Response {
        private boolean success;
        private String message;
    }

}
