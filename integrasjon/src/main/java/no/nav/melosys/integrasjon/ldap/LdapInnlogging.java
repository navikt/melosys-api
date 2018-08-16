package no.nav.melosys.integrasjon.ldap;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;

public class LdapInnlogging {

    private LdapInnlogging() {
    }

    public static LdapContext lagLdapContext() throws TekniskException {
        String url = getRequiredProperty("ldap.url");
        String user = getRequiredProperty("ldap.username") + "@" + getRequiredProperty("ldap.domain");

        Hashtable<String, Object> environment = new Hashtable<>(); //NOSONAR //metodeparameter krever Hashtable
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        environment.put(Context.SECURITY_AUTHENTICATION, "simple");
        environment.put(Context.SECURITY_CREDENTIALS, getRequiredProperty("ldap.password"));
        environment.put(Context.SECURITY_PRINCIPAL, user);
        environment.put(Context.PROVIDER_URL, url);
        try {
            return new InitialLdapContext(environment, null);
        } catch (NamingException e) {
            throw new IntegrasjonException("Klarte ikke å koble til LDAP på URL " + url);
        }
    }

    static String getRequiredProperty(String navn) throws TekniskException {
        String verdi = System.getProperty(navn);
        if (verdi == null || verdi.isEmpty()) {
            throw new TekniskException("Klarte ikke koble til LDAP da påkrevd system property " + navn + " ikke er satt");
        }
        return verdi;

    }
}
