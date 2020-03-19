package no.nav.melosys.integrasjon.ldap;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import no.nav.melosys.exception.TekniskException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
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

    public LdapBruker hentBrukerinformasjon(String ident) throws TekniskException {
        validerIdent(ident);
        return ldapTemplate.searchForObject(query().where("cn").is(ident), new LdapBrukerMapper());
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

    static class LdapBrukerMapper implements ContextMapper<LdapBruker> {

        public LdapBruker mapFromContext(Object ctx) throws NamingException {
            DirContextAdapter context = (DirContextAdapter) ctx;
            String navn = context.getAttributes().get("displayName").get().toString();
            List<String> grupper = new ArrayList<>();
            Attribute memberOf = context.getAttributes().get("memberOf");

            NamingEnumeration<?> all = memberOf.getAll();
            while (all.hasMoreElements()) {
                Object group = all.nextElement();
                String dnValue = group.toString();
                String cnValue = LdapUtils.filterDNtoCNvalue(dnValue);
                grupper.add(cnValue);
            }

            return new LdapBruker(navn, grupper);
        }
    }
}