package com.expedia.www.cs.media;

import org.springframework.boot.ResourceBanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.DefaultResourceLoader;

@SpringBootApplication
@ComponentScan(basePackages = "com.expedia")
//@ImportResource("classpath:media-services.xml")
@SuppressWarnings({"PMD.UseUtilityClass"})
public class Starter extends SpringBootServletInitializer {
	public static void main(String[] args) throws Exception {
		final SpringApplication application = new SpringApplicationBuilder()
				.banner(new ResourceBanner(new DefaultResourceLoader().getResource("banner.txt")))
				.child(Starter.class)
				.build();
		application.run(args);
	}
}
