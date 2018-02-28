package no.nav.melosys.tjenester.gui;

import java.util.Collection;
import javax.ws.rs.ForbiddenException;

import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.TekniskException;
import no.nav.melosys.integrasjon.ldap.LdapBruker;
import no.nav.melosys.integrasjon.ldap.LdapBrukeroppslag;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.melosys.tjenester.gui.config.Grupper.MELOSYS_GRUPPE;

// FIXME Midlertidig tilgangskontroll. Tilgangskontroll skal egentlig implementeres med ABAC.
public class Tilgangskontroll {

    private static final Logger log = LoggerFactory.getLogger(Tilgangskontroll.class);

    private Tilgangskontroll() {
    }

    public static void sjekk() {
        String ident = SpringSubjectHandler.getUserID();

        LdapBruker ldapBruker;
        try {
            ldapBruker = new LdapBrukeroppslag().hentBrukerinformasjon(ident);
            Tilgangskontroll.sjekk(ldapBruker);

        } catch (IntegrasjonException | TekniskException e) {
            throw e;
        }
    }

    public static void sjekk(LdapBruker ldapBruker) {
        Collection<String> groups = ldapBruker.getGroups();
        if (!(groups.stream().anyMatch(g -> g.equalsIgnoreCase(MELOSYS_GRUPPE)))) {
            log.warn("Bruker {} er ikke medlem av {}.", ldapBruker.getDisplayName(), MELOSYS_GRUPPE);
            throw new ForbiddenException();
        }
    }

}
