package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.tjenester.gui.dto.InnloggetBrukerDto;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Api(tags = {"saksbehandler"})
@Path("/saksbehandler")
public class SaksbehandlerTjeneste {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Returnerer fullt navn for ident",
            notes = ("Ident hentes fra sikkerhetskonteksten som er tilgjengelig etter innlogging."))
    public InnloggetBrukerDto innloggetBruker() {
        // TODO Implementere LDAP oppslag
        String ident = SpringSubjectHandler.getUserID();

       return new InnloggetBrukerDto(ident, "_TODO_");
    }

}