package no.nav.melosys.tjenester.gui;

import java.util.Collection;
import javax.ws.rs.ForbiddenException;

import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.TekniskException;
import no.nav.melosys.integrasjon.ldap.LdapBruker;
import no.nav.melosys.integrasjon.ldap.LdapBrukeroppslag;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;

import static no.nav.melosys.tjenester.gui.config.Grupper.MELOSYS_GRUPPE;

// FIXME Midlertidig tilgangskontroll. Tilgangskontroll skal egentlig implementeres med ABAC.
public class Tilgangskontroll {

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
        if (!groups.contains(MELOSYS_GRUPPE)) {
            throw new ForbiddenException();
        }
    }

}
