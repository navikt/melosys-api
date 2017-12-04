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
 * The configuration class uses a spring filter created with the MITREid library and configures it with the required
 * spring options for OpenID Connect through http.
 */
@Configuration
@EnableWebSecurity
@Import(MITREidConfig.class)
public class OIDCSecurityConfig extends WebSecurityConfigurerAdapter {

    private OIDCAuthenticationFilter filter;

    @Autowired
    public void setOIDCAuthenticationFilter(OIDCAuthenticationFilter filter) {
        this.filter = filter;
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers("/webjars/**", "/css/**", "/internal/**");
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http
                .addFilterAfter(new OAuth2ClientContextFilter(), AbstractPreAuthenticatedProcessingFilter.class)
                .addFilterAfter(filter, OAuth2ClientContextFilter.class)
                .authorizeRequests()
                .antMatchers("/isReady", "/isAlive")
                .permitAll()
                .anyRequest()
                .authenticated()
                .and().csrf().disable()
                .httpBasic().authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/openid_connect_login"));
    }

}