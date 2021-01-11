package no.nav.melosys.integrasjon.ldap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

@Configuration
public class LdapConfig {

    @Bean
    public LdapContextSource ldapContextSource(LdapCredentials ldapCredentials) {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapCredentials.getUrl());

        //For å koble til lokal ldap-server uten autentisering
        if (ldapCredentials.getUsername() != null && ldapCredentials.getPassword() != null) {
            contextSource.setUserDn(ldapCredentials.getUsername() + "@" + ldapCredentials.getDomain());
            contextSource.setPassword(ldapCredentials.getPassword());
        }
        contextSource.setBase(ldapCredentials.getUserbasedn());

        return contextSource;
    }

    @Bean
    public LdapTemplate ldapTemplate(LdapContextSource ldapContextSource) {
        return new LdapTemplate(ldapContextSource);
    }
}
