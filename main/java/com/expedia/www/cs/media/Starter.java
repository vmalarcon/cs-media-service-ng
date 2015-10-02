package com.expedia.www.cs.media;

import org.springframework.boot.ResourceBanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.core.io.DefaultResourceLoader;

@SpringBootApplication
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
