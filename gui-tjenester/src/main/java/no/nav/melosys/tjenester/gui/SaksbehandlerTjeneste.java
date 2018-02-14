package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.TekniskException;
import no.nav.melosys.integrasjon.ldap.LdapBruker;
import no.nav.melosys.integrasjon.ldap.LdapBrukeroppslag;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
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
        String ident = SpringSubjectHandler.getUserID();

        LdapBruker ldapBruker;
        try {
            ldapBruker = new LdapBrukeroppslag().hentBrukerinformasjon(ident);
        } catch (IntegrasjonException | TekniskException e) {
            log.warn("", e);
            throw e;
        }

       return new InnloggetBrukerDto(ident, ldapBruker.getDisplayName());
    }

}