package no.nav.melosys.sikkerhet.oidc;

import org.mitre.openid.connect.client.OIDCAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring security {@code @Configuration}
 */
@Configuration
@EnableWebSecurity
@Import(MITREidConfig.class)
public class OIDCSecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${config.cors.allowOrigin}")
    private String allowOrigin;

    private OIDCAuthenticationFilter filter;

    private OidcTokenRefreshingFilter refreshingFilter;

    @Autowired
    public OIDCSecurityConfig(OIDCAuthenticationFilter filter, OidcTokenRefreshingFilter refreshingFilter) {
        this.filter = filter;
        this.refreshingFilter = refreshingFilter;
    }

    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers("/internal/health");
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.cors().and()
                .addFilterAfter(new OAuth2ClientContextFilter(), AbstractPreAuthenticatedProcessingFilter.class)
                .addFilterAfter(filter, OAuth2ClientContextFilter.class)
                .addFilterAfter(refreshingFilter, OIDCAuthenticationFilter.class)
                .authorizeRequests().anyRequest().authenticated()
                .and().csrf().disable()
                .httpBasic().authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/openid_connect_login"));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin(allowOrigin);
        config.setAllowedMethods(Arrays.asList("GET", "POST"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        config.addAllowedHeader("Accept");
        config.addAllowedHeader("Accept-Charset");
        config.addAllowedHeader("Authorization");
        config.addAllowedHeader("Content-type");
        config.addAllowedHeader("Origin");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}