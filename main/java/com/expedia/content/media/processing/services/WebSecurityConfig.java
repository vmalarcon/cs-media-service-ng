package com.expedia.content.media.processing.services;

import com.expedia.content.media.processing.services.util.MediaServiceUrl;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;

/**
 * spring security class, initialize Authenticated user from properties file and set different permission for webservice URL
 */
@Configuration
@EnableWebMvcSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final String ROLE = "USER";

    @Autowired
    private CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Value("${media.service.authorized.users}")
    private String authorizedUsers;

    /**
     * This section defines the user accounts which can be used for authentication as well as the roles each user has.
     *
     * @param auth The AuthenticationManagerBuilder to use.
     * @throws Exception
     * @see org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter#configure
     * (org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder)
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        final String[] arrayUser = StringUtils.split(authorizedUsers, ",");
        for (int i = 0; i < arrayUser.length; i++) {
            auth.inMemoryAuthentication().
                    withUser(arrayUser[i]).password("").roles(ROLE);
        }
    }

    /**
     * This section defines the security policy for the app.
     * Authentication error is handled in CustomAuthenticationEntryPoint.
     * CSRF headers are disabled
     *
     * @param http The HttpSecurity to modify.
     * @throws Exception
     * @see org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter#configure
     * (org.springframework.security.config.annotation.web.builders.HttpSecurity)
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().antMatchers("/", MediaServiceUrl.ACQUIRE_MEDIA.getUrl()).permitAll();
        http.httpBasic().and().authorizeRequests().
                antMatchers(HttpMethod.POST, MediaServiceUrl.MEDIA_STATUS.getUrl()).authenticated().
                antMatchers(HttpMethod.PUT, MediaServiceUrl.MEDIA_IMAGES.getUrl()).authenticated().
                antMatchers(HttpMethod.POST, MediaServiceUrl.MEDIA_IMAGES.getUrl()).authenticated().
                antMatchers(HttpMethod.GET, MediaServiceUrl.MEDIA_IMAGES.getUrl() + "/**").authenticated().
                antMatchers(HttpMethod.POST, MediaServiceUrl.MEDIA_SOURCEURL.getUrl()).authenticated().
                antMatchers(HttpMethod.POST, MediaServiceUrl.MEDIA_TEMP_DERIVATIVE.getUrl()).authenticated().
                antMatchers(HttpMethod.GET, MediaServiceUrl.MEDIA_BY_DOMAIN.getUrl() + "/**/domainId/**").authenticated().
                antMatchers(HttpMethod.GET, MediaServiceUrl.MEDIA_DOMAIN_CATEGORIES.getUrl() + "/**").authenticated().
                antMatchers(HttpMethod.GET, MediaServiceUrl.MEDIA_DOWLOAD.getUrl() + "/**").authenticated();
        http.csrf().disable();
        //handle 401 case when Authentication header is missing.
        http.exceptionHandling().authenticationEntryPoint(authenticationEntryPoint);
        //handle 401 case when input user is wrong
        http.httpBasic().authenticationEntryPoint(authenticationEntryPoint);
    }

}
