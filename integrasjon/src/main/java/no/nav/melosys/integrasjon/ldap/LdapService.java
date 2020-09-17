package no.nav.melosys.integrasjon.ldap;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import no.nav.melosys.exception.TekniskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

@Component
public class LdapService {
    private static final Logger log = LoggerFactory.getLogger(LdapService.class);

    private final LdapTemplate ldapTemplate;

    private static final Pattern IDENT_PATTERN = Pattern.compile("^[a-åA-Å]\\d{6}$");

    @Autowired
    public LdapService(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }

    public Optional<LdapBruker> finnBrukerinformasjon(String ident) throws TekniskException {
        if (ident == null || ident.isEmpty()) {
            throw new TekniskException("Kan ikke slå opp brukernavn uten å ha ident");
        }

        return defaultHvisUgyldig(ident, ident).or(() ->
            ldapTemplate.search(query().where("cn").is(ident), new LdapBrukerMapper()).stream().findFirst());
    }

    private Optional<LdapBruker> defaultHvisUgyldig(String ident, String defaultIdent) {
        Matcher matcher = IDENT_PATTERN.matcher(ident);
        if (!matcher.matches()) {
            return Optional.of(new LdapBruker(defaultIdent, Collections.emptyList()));
        }

        return Optional.empty();
    }

    static class LdapBrukerMapper implements AttributesMapper<LdapBruker> {

        private static final String DISPLAY_NAME = "displayName";
        private static final String MEMBER_OF = "memberOf";

        @Override
        public LdapBruker mapFromAttributes(Attributes attributes) throws NamingException {
            return new LdapBruker(
                hentDisplayName(attributes),
                hentMemberOf(attributes)
            );
        }

        private String hentDisplayName(Attributes attributes) throws NamingException {
            return attributes.get(DISPLAY_NAME).get().toString();
        }

        private Collection<String> hentMemberOf(Attributes attributes) throws NamingException {
            List<String> grupper = new ArrayList<>();
            Attribute memberOf = attributes.get(MEMBER_OF);

            NamingEnumeration<?> all = memberOf.getAll();
            while (all.hasMoreElements()) {
                Object group = all.nextElement();
                grupper.add(filterDNtoCNvalue(group.toString()));
            }

            return grupper;
        }

        private String filterDNtoCNvalue(String value) {
            if (value.toLowerCase(Locale.ROOT).contains("cn=")) {
                try {
                    LdapName ldapname = new LdapName(value); //NOSONAR, only used locally
                    for (Rdn rdn : ldapname.getRdns()) {
                        if ("CN".equalsIgnoreCase(rdn.getType())) {
                            String cn = rdn.getValue().toString();
                            log.debug("uid on DN form. Filtered from {} to {}", value, cn); //NOSONAR trusted source, validated SAML-token or LDAP memberOf
                            return cn;
                        }
                    }
                } catch (InvalidNameException e) { //NOSONAR
                    log.debug("value not on DN form. Skipping filter. {}", e.getExplanation()); //NOSONAR trusted source
                }
            }
            return value;
        }
    }
}