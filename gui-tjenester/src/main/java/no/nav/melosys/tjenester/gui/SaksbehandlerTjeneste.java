package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.tjenester.gui.dto.InnloggetBrukerDto;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Api(tags = {"saksbehandler"})
@Path("/saksbehandler")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class SaksbehandlerTjeneste {

    @GET
    @ApiOperation(value = "Returnerer fullt navn for ident",
            notes = ("Ident hentes fra sikkerhetskonteksten som er tilgjengelig etter innlogging."))
    public InnloggetBrukerDto innloggetBruker() {
        // TODO Implementere LDAP oppslag
        String ident = SpringSubjectHandler.getUserID();

       return new InnloggetBrukerDto(ident, "_TODO_");
    }

}