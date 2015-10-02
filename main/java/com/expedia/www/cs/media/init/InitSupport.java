package com.expedia.www.cs.media.init;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.Validate;

import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.filter.OncePerRequestFilter;

import com.expedia.www.platform.isactive.providers.ManifestBasedVersionProvider;
import com.expedia.www.platform.isactive.providers.ManifestReader;
import com.expedia.www.platform.isactive.servlet.BuildInfoServlet;
import com.expedia.www.platform.isactive.servlet.IsActiveServlet;
import com.expedia.www.platform.isactive.servlet.WebAppManifestPathProvider;
import com.expedia.www.platform.monitoring.MonitoringAgent;
import com.yammer.metrics.web.DefaultWebappMetricsFilter;

@Configuration
@EnableConfigurationProperties
@SuppressWarnings({"PMD.SingularField", "PMD.ImmutableField"})
public class InitSupport implements ServletContextAware {

    private static final String HOST_NAME = ManagementFactory.getRuntimeMXBean().getName().split("@")[1].replace('.', '_');
    private ServletContext servletContext;

    @Bean
    public ManifestBasedVersionProvider manifestBasedVersionProvider() {
        return new ManifestBasedVersionProvider(new WebAppManifestPathProvider(servletContext),
                                                new ManifestReader());
    }

    @Bean
    @ConfigurationProperties
    public MonitoringAgentConfig monitoringAgentConfig(ManifestBasedVersionProvider versionProvider) {
        final MonitoringAgentConfig agentConfig = new MonitoringAgentConfig();
        agentConfig.setHostName(HOST_NAME, versionProvider.get());
        return agentConfig;
    }

    @Bean(initMethod = "start")
    public MonitoringAgent monitoringAgent(MonitoringAgentConfig monitoringAgentConfig) throws Exception {
        return new MonitoringAgent(monitoringAgentConfig.getMonitoringAgent());
    }

    @Bean
    public ServletRegistrationBean registBuildInfo() {
        final ServletRegistrationBean isActiveBean = new ServletRegistrationBean(new BuildInfoServlet(), "/buildInfo");
        isActiveBean.setName("buildInfo");
        return isActiveBean;
    }

    @Bean
    public ServletRegistrationBean registIsActive() {
        final ServletRegistrationBean isActiveBean = new ServletRegistrationBean(new IsActiveServlet(), "/isActive");
        isActiveBean.setName("isActive");
        return isActiveBean;
    }

    @Bean
    public FilterRegistrationBean corsFilterRegistrationBean() {
        final FilterRegistrationBean registrationBean = new FilterRegistrationBean();

        registrationBean.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
                httpServletResponse.addHeader("Access-Control-Allow-Origin", "*");
                httpServletResponse.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                httpServletResponse.addHeader("Access-Control-Allow-Headers", "origin, content-type, accept, x-requested-with");
                filterChain.doFilter(httpServletRequest, httpServletResponse);
            }
        });

        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean metricsWebFilterRegistrationBean () {
        final FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        registrationBean.setFilter(new DefaultWebappMetricsFilter());
        return registrationBean;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        Validate.notNull(servletContext, "servletContext is required");
        this.servletContext = servletContext;
    }
}
