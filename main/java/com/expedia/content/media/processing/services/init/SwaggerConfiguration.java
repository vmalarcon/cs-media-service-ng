package com.expedia.content.media.processing.services.init;

import com.mangofactory.swagger.configuration.SpringSwaggerConfig;
import com.mangofactory.swagger.plugin.EnableSwagger;
import com.mangofactory.swagger.plugin.SwaggerSpringMvcPlugin;
import com.wordnik.swagger.model.ApiInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

@EnableSwagger
public class SwaggerConfiguration {

    private SpringSwaggerConfig springSwaggerConfig;

    @Value("${swagger.service.version}")
    private String serviceVersion;

    @Value("${swagger.service.title}")
    private String serviceTitle;

    @Value("${swagger.service.description}")
    private String serviceDescription;

    @Value("${swagger.service.termsPath}")
    private String serviceTermsPath;

    @Value("${swagger.service.email}")
    private String serviceEmail;

    @Value("${swagger.service.licenceType}")
    private String serviceLicenceType;

    @Value("${swagger.service.licencePath}")
    private String serviceLicencePath;


    @Autowired
    public void setSpringSwaggerConfig(SpringSwaggerConfig springSwaggerConfig) {
        this.springSwaggerConfig = springSwaggerConfig;
    }

    @Bean
    public SwaggerSpringMvcPlugin customImplementation() {
        return new SwaggerSpringMvcPlugin(this.springSwaggerConfig)
                .apiInfo(apiInfo())
                .includePatterns("/service/.*?")
                .apiVersion(this.serviceVersion);
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
                this.serviceTitle,
                this.serviceDescription,
                this.serviceTermsPath,
                this.serviceEmail,
                this.serviceLicenceType,
                this.serviceLicencePath);
    }
}
