package no.nav.melosys.integrasjon.ldap;

import java.util.Locale;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LdapUtils {

    private static final Logger logger = LoggerFactory.getLogger(LdapUtils.class);

    private LdapUtils(){}

    public static String filterDNtoCNvalue(String value) {
        if(value.toLowerCase(Locale.ROOT).contains("cn=")) {
            try {
                LdapName ldapname = new LdapName(value); //NOSONAR, only used locally
                for (Rdn rdn : ldapname.getRdns()) {
                    if ("CN".equalsIgnoreCase(rdn.getType())) {
                        String cn = rdn.getValue().toString();
                        logger.debug("uid on DN form. Filtered from {} to {}", value, cn); //NOSONAR trusted source, validated SAML-token or LDAP memberOf
                        return cn;
                    }
                }
            } catch (InvalidNameException e) { //NOSONAR
                logger.debug("value not on DN form. Skipping filter. {}", e.getExplanation()); //NOSONAR trusted source
            }
        }
        return value;
    }
}