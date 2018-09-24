package no.nav.melosys.sikkerhet.oidc;

import org.mitre.openid.connect.client.OIDCAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

/**
 * Spring security {@code @Configuration}
 */
@Configuration
@EnableWebSecurity
@Import(MITREidConfig.class)
public class OIDCSecurityConfig extends WebSecurityConfigurerAdapter {

    private OIDCAuthenticationFilter filter;

    private OidcTokenRefreshingFilter refreshingFilter;

    @Autowired
    public OIDCSecurityConfig(OIDCAuthenticationFilter filter, OidcTokenRefreshingFilter refreshingFilter) {
        this.filter = filter;
        this.refreshingFilter = refreshingFilter;
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers("/static/**", "/internal/**", "/frontendlogger/**");
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.addFilterAfter(new OAuth2ClientContextFilter(), AbstractPreAuthenticatedProcessingFilter.class)
            .addFilterAfter(filter, OAuth2ClientContextFilter.class)
            .addFilterAfter(refreshingFilter, OIDCAuthenticationFilter.class)
            .authorizeRequests().anyRequest().authenticated()
            .and().csrf().disable()
            .httpBasic().authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/openid_connect_login"));
    }

}