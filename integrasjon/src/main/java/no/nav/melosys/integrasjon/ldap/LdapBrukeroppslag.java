package no.nav.melosys.integrasjon.ldap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import no.nav.melosys.exception.TekniskException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Component;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

@Component
public class LdapBrukeroppslag {

    private final LdapTemplate ldapTemplate;

    private static final Pattern IDENT_PATTERN = Pattern.compile("^\\p{LD}+$");

    @Autowired
    public LdapBrukeroppslag(LdapTemplate ldapTemplate) {
        this.ldapTemplate = ldapTemplate;
    }

    public Optional<LdapBruker> finnBrukerinformasjon(String ident) throws TekniskException {
        validerIdent(ident);
        return ldapTemplate.search(query().where("cn").is(ident), new LdapBrukerMapper()).stream().findFirst();
    }

    private void validerIdent(String ident) throws TekniskException {
        if (ident == null || ident.isEmpty()) {
            throw new TekniskException("Kan ikke slå opp brukernavn uten å ha ident");
        }
        Matcher matcher = IDENT_PATTERN.matcher(ident);
        if (!matcher.matches()) {
            throw new TekniskException("Mulig LDAP-injection forsøk. Søkte med ugyldig ident '" + ident + "'");
        }
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
                grupper.add(LdapUtils.filterDNtoCNvalue(group.toString()));
            }

            return grupper;
        }
    }
}