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

@Configuration
@EnableWebMvcSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final String ROLE = "USER";

    @Autowired
    private CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Value("${media.service.authenticated.users}")
    private String permittedUser;

    /**
     * This section defines the user accounts which can be used for authentication as well as the roles each user has.
     *
     * @see org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter#configure
     * (org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder)
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        String[] arrayUser = StringUtils.split(permittedUser, ",");
        for (int i = 0; i < arrayUser.length; i++) {
            auth.inMemoryAuthentication().
                    withUser(arrayUser[i]).password("").roles(ROLE);
        }
    }

    /**
     * This section defines the security policy for the app.
     * /acquireMedia is permitted without authentication.
     * /media/v1/lateststatus is permitted with pre-defined users.
     * CSRF headers are disabled
     *
     * @param http
     * @throws Exception
     * @see org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter#configure
     * (org.springframework.security.config.annotation.web.builders.HttpSecurity)
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().antMatchers("/", MediaServiceUrl.ACQUIREMEDIA.getUrl()).permitAll();
        http.httpBasic().and().authorizeRequests().
                antMatchers(HttpMethod.POST, MediaServiceUrl.MEDIASTATUS.getUrl()).authenticated();
        http.csrf().disable();
        http.exceptionHandling().authenticationEntryPoint(authenticationEntryPoint);
    }

}
