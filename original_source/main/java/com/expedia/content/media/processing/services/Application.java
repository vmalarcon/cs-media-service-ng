package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.pipeline.util.OSDetector;

import org.im4java.process.ProcessStarter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import expedia.content.solutions.metrics.spring.EnableMetrics;
import expedia.content.solutions.poke.spring.support.EnablePoke;

/**
 * MPP media service application.
 * This class has the main Spring configuration and also the bootstrap for the application.
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.expedia.content.media.processing")
@ImportResource("classpath:media-services.xml")
@EnableMetrics
@EnablePoke
@EnableTransactionManagement
@SuppressWarnings({"PMD.UseUtilityClass"})
public class Application {

    /*public static void main(String[] args) throws Exception {
        if (OSDetector.detectOS() == OSDetector.OS.WINDOWS) {
            final String path = System.getenv("PATH").replace('\\', '/');
            ProcessStarter.setGlobalSearchPath(path);
        }
        final SpringApplication application = new SpringApplicationBuilder()
                .banner(new ResourceBanner(new DefaultResourceLoader().getResource("banner.txt")))
                .child(Application.class)
                .build();
        application.run(args);
    } */
    
    public static void main(String[] args) {
        if (OSDetector.detectOS() == OSDetector.OS.WINDOWS) {
            final String path = System.getenv("PATH").replace('\\', '/');
            ProcessStarter.setGlobalSearchPath(path);
        }
        SpringApplication.run(Application.class, args);
    }

}
