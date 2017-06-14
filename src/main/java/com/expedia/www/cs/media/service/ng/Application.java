package com.expedia.www.cs.media.service.ng;

import expedia.content.solutions.metrics.spring.EnableMetrics;
import expedia.content.solutions.poke.spring.support.EnablePoke;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnablePoke
@EnableMetrics
public class Application {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }
}
