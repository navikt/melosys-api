package no.nav.melosys.sikkerhet.oidc;

import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.model.RegisteredClient;
import org.mitre.openid.connect.client.NamedAdminAuthoritiesMapper;
import org.mitre.openid.connect.client.OIDCAuthenticationFilter;
import org.mitre.openid.connect.client.OIDCAuthenticationProvider;
import org.mitre.openid.connect.client.UserInfoFetcher;
import org.mitre.openid.connect.client.service.ClientConfigurationService;
import org.mitre.openid.connect.client.service.IssuerService;
import org.mitre.openid.connect.client.service.ServerConfigurationService;
import org.mitre.openid.connect.client.service.impl.PlainAuthRequestUrlBuilder;
import org.mitre.openid.connect.client.service.impl.StaticClientConfigurationService;
import org.mitre.openid.connect.client.service.impl.StaticServerConfigurationService;
import org.mitre.openid.connect.client.service.impl.StaticSingleIssuerService;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The configuration class creates spring filter with OpenID Connect server and client services, and a manager
 * through the MITREid library.
 */
@Configuration
public class MITREidConfig {

    @Bean
    public OIDCAuthenticationProvider oidcAuthenticationProvider() {
        OIDCAuthenticationProvider provider = new OIDCAuthenticationProvider();
        provider.setAuthoritiesMapper(new NamedAdminAuthoritiesMapper());
        provider.setUserInfoFetcher(new UserInfoFetcher());
        return provider;
    }

    @Bean
    public OIDCAuthenticationFilter oidcAuthenticationFilter(AuthenticationManager authManager, IssuerService issuerService,
                                                             ServerConfigurationService scs, ClientConfigurationService ccs) {
        OIDCAuthenticationFilter filter = new OIDCAuthenticationFilter();

        filter.setAuthenticationManager(authManager);
        filter.setIssuerService(issuerService);
        filter.setServerConfigurationService(scs);
        filter.setClientConfigurationService(ccs);
        filter.setAuthRequestUrlBuilder(new PlainAuthRequestUrlBuilder());
        return filter;
    }

    @Bean
    public AuthenticationManager authManager() {
        return new ProviderManager(Collections.singletonList(oidcAuthenticationProvider()));
    }

    @Bean
    public IssuerService issuerService(Environment env) {
        StaticSingleIssuerService is = new StaticSingleIssuerService();
        is.setIssuer(env.getRequiredProperty("OpenIdConnect.issoIssuer"));
        //is.setIssuer(env.getRequiredProperty("isso-issuer"));
        return is;
    }

    @Bean
    public ServerConfigurationService scs(Environment env) {
        StaticServerConfigurationService sscs = new StaticServerConfigurationService();

        Map<String, ServerConfiguration> servers = new HashMap<>();
        ServerConfiguration serverConfig = new ServerConfiguration();
        serverConfig.setIssuer(env.getRequiredProperty("OpenIdConnect.issoIssuer"));
        serverConfig.setAuthorizationEndpointUri(env.getRequiredProperty("OpenIdConnect.issoHost") + "/authorize");
        serverConfig.setTokenEndpointUri(env.getRequiredProperty("OpenIdConnect.issoHost") + "/access_token");
        serverConfig.setUserInfoUri(env.getRequiredProperty("OpenIdConnect.issoHost") + "/userinfo");
        serverConfig.setJwksUri(env.getRequiredProperty("OpenIdConnect.issoJwks"));

        servers.put(env.getRequiredProperty("OpenIdConnect.issoIssuer"), serverConfig);

        sscs.setServers(servers);
        return sscs;
    }

    @Bean
    public ClientConfigurationService ccs(Environment env) {
        StaticClientConfigurationService sccs = new StaticClientConfigurationService();

        Map<String, RegisteredClient> clients = new HashMap<>();
        RegisteredClient client = new RegisteredClient();
        client.setClientId(env.getRequiredProperty("OpenIdConnect.username"));
        client.setClientSecret(env.getRequiredProperty("OpenIdConnect.password"));
        client.setScope(Collections.singleton("openid"));
        client.setTokenEndpointAuthMethod(ClientDetailsEntity.AuthMethod.SECRET_BASIC);

        clients.put(env.getRequiredProperty("OpenIdConnect.issoIssuer"), client);
        sccs.setClients(clients);
        return sccs;
    }
}
