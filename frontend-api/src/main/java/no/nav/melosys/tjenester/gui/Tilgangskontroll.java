package no.nav.melosys.tjenester.gui;

import java.util.Collection;

import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.ldap.LdapBruker;
import no.nav.melosys.integrasjon.ldap.LdapBrukeroppslag;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static no.nav.melosys.tjenester.gui.config.Grupper.MELOSYS_GRUPPE;

// FIXME Midlertidig tilgangskontroll. Tilgangskontroll skal egentlig implementeres med ABAC.
public class Tilgangskontroll {

    private static final Logger log = LoggerFactory.getLogger(Tilgangskontroll.class);

    private Tilgangskontroll() {
    }

    public static void sjekk() throws SikkerhetsbegrensningException, TekniskException {
        String ident = SubjectHandler.getInstance().getUserID();

        LdapBruker ldapBruker;
        ldapBruker = new LdapBrukeroppslag().hentBrukerinformasjon(ident);
        Tilgangskontroll.sjekk(ldapBruker);
    }

    public static void sjekk(LdapBruker ldapBruker) throws SikkerhetsbegrensningException {
        Collection<String> groups = ldapBruker.getGroups();
        if (!(groups.stream().anyMatch(g -> g.equalsIgnoreCase(MELOSYS_GRUPPE)))) {
            throw new SikkerhetsbegrensningException(String.format("Bruker %1 er ikke medlem av %2.", ldapBruker.getDisplayName(), MELOSYS_GRUPPE));
        }
    }

}
