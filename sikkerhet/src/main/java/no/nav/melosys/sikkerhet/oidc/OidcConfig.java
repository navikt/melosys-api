package no.nav.melosys.sikkerhet.oidc;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.http.callback.NoParameterCallbackUrlResolver;
import org.pac4j.core.http.url.DefaultUrlResolver;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.springframework.security.web.CallbackFilter;
import org.pac4j.springframework.security.web.SecurityFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class OidcConfig {
    private static final String OIDC_CLIENT_NAME = "OidcClient";
    private static final String CALLBACKURI = "/openid_connect_login";

    @Bean
    public Config config(OidcProperties oidcProperties) {
        OidcConfiguration oidcConfiguration = new OidcConfiguration();
        oidcConfiguration.setClientId(oidcProperties.getUsername());
        oidcConfiguration.setSecret(oidcProperties.getPassword());
        oidcConfiguration.setUseNonce(true);
        oidcConfiguration.setWithState(true);
        oidcConfiguration.setDiscoveryURI(oidcProperties.getDiscoveryUrl());
        oidcConfiguration.setScope("openid");
        oidcConfiguration.setClientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
        oidcConfiguration.setMaxClockSkew(30);
        oidcConfiguration.setPreferredJwsAlgorithm(JWSAlgorithm.RS256);
        oidcConfiguration.setExpireSessionWithToken(true);
        //brukes som "fattigmanns refresh", dvs. session går ut 5 min før token går ut,
        //det vil da gå en redirect til openam hvor sesjonen er gyldig og brukeren får nytt token uten å
        //måtte logge inn.
        oidcConfiguration.setTokenExpirationAdvance(60 * 5);

        OidcClient<OidcProfile, OidcConfiguration> oidcClient = new OidcClient<>(oidcConfiguration);
        oidcClient.setCallbackUrlResolver(new NoParameterCallbackUrlResolver());
        oidcClient.setName(OIDC_CLIENT_NAME);
        oidcClient.setUrlResolver(new DefaultUrlResolver(true));
        oidcClient.setAuthorizationGenerator((ctx, profile) -> profile);

        Clients clients = new Clients(CALLBACKURI, oidcClient);
        oidcClient.setCallbackUrl(CALLBACKURI);
        return new Config(clients);
    }

    @Configuration
    public static class OidcWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        @Autowired
        private Config config;

        @Override
        public void configure(WebSecurity web) {
            web.ignoring().antMatchers("/static/**", "/internal/**", "/frontendlogger/**");
        }

        protected void configure(final HttpSecurity http) throws Exception {

            final SecurityFilter securityFilter = new SecurityFilter(config, OIDC_CLIENT_NAME);
            final CallbackFilter callbackFilter = new CallbackFilter(config);
            callbackFilter.setSuffix(CALLBACKURI);
            http.cors().and()
                .authorizeRequests().antMatchers(CALLBACKURI).permitAll()
                .and()
                .addFilterBefore(callbackFilter, BasicAuthenticationFilter.class)
                .addFilterBefore(securityFilter, BasicAuthenticationFilter.class)
                .authorizeRequests()
                .anyRequest().authenticated()
                .and()
                .csrf().disable()
                .logout()
                .logoutSuccessUrl("/");
        }
    }
}
