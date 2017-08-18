package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.tjenester.gui.dto.InnloggetBrukerDto;
import no.nav.vedtak.felles.integrasjon.ldap.LdapBrukeroppslag;
import no.nav.vedtak.sikkerhet.context.SubjectHandler;

@Api(tags = {"saksbehandler"})
@Path("/saksbehandler")
public class SaksbehandlerTjeneste {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Returnerer fullt navn for ident",
            notes = ("Ident hentes fra sikkerhetskonteksten som er tilgjengelig etter innlogging."))
    public InnloggetBrukerDto innloggetBruker() {
        String ident = SubjectHandler.getSubjectHandler().getUid();
        LdapBrukeroppslag ldap = new LdapBrukeroppslag();
        String navn = ldap.finnNavnPåBruker(ident);
        return new InnloggetBrukerDto(ident, navn);
        //return new InnloggetBrukerDto("Z991001", "F_Z991001 E_Z991001");
    }

}