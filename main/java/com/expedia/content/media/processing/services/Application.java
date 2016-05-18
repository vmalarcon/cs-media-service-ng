package com.expedia.content.media.processing.services;

import org.im4java.process.ProcessStarter;
import org.springframework.boot.ResourceBanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.expedia.content.media.processing.pipeline.util.OSDetector;

import expedia.content.solutions.metrics.annotations.Gauge;
import expedia.content.solutions.metrics.spring.EnableMetrics;

/**
 * MPP media service application.
 * This class has the main Spring configuration and also the bootstrap for the application.
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.expedia.content.media.processing")
@ImportResource("classpath:media-services.xml")
@EnableMetrics
@EnableTransactionManagement
@SuppressWarnings({"PMD.UseUtilityClass","PMD.UnusedPrivateField"})
public class Application extends SpringBootServletInitializer {

    @Gauge(name = "isALive")
    private static final int LIVE_COUNT = 1;
    
    public static void main(String[] args) throws Exception {
        if (OSDetector.detectOS() == OSDetector.OS.WINDOWS) {
            final String path = System.getenv("PATH").replace('\\', '/');
            ProcessStarter.setGlobalSearchPath(path);
        }
        final SpringApplication application = new SpringApplicationBuilder()
                .banner(new ResourceBanner(new DefaultResourceLoader().getResource("banner.txt")))
                .child(Application.class)
                .build();
        application.run(args);
    }

}
