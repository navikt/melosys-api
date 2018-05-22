package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.ldap.LdapBruker;
import no.nav.melosys.integrasjon.ldap.LdapBrukeroppslag;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.InnloggetBrukerDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"saksbehandler"})
@Path("/saksbehandler")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class SaksbehandlerTjeneste extends RestTjeneste {

    private static final Logger log = LoggerFactory.getLogger(SaksbehandlerTjeneste.class);

    @GET
    @ApiOperation(value = "Returnerer fullt navn for ident",
            notes = ("Ident hentes fra sikkerhetskonteksten som er tilgjengelig etter innlogging."))
    public InnloggetBrukerDto innloggetBruker() {
        String ident = SubjectHandler.getInstance().getUserID();

        LdapBruker ldapBruker = null;
        try {
            ldapBruker = new LdapBrukeroppslag().hentBrukerinformasjon(ident);

            // FIXME Midlertidig tilgangskontroll
            Tilgangskontroll.sjekk(ldapBruker);

        } catch (IntegrasjonException | TekniskException e) {
            log.warn("Det oppstod en feil under henting av LDAP-profil for bruker {}: ", ident, e);
        }

        String navn = "FEIL";
        if (ldapBruker != null) {
            navn = ldapBruker.getDisplayName();
        }

       return new InnloggetBrukerDto(ident, navn);
    }

}