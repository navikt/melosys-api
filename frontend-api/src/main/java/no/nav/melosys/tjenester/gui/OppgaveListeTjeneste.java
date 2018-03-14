package no.nav.melosys.tjenester.gui;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.aggregate.OppgaveAG;
import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.integrasjon.gsak.GsakService;
import no.nav.melosys.tjenester.gui.dto.OppgaveDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"oppgavelister"})
@Path("/oppgavelister")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class OppgaveListeTjeneste extends RestTjeneste  {

    private GsakService gsakService;

    @Autowired
    public OppgaveListeTjeneste(GsakService gsakService) {
        this.gsakService = gsakService;

    }

    @GET
    @Path("/{ansvarligEnhetId}/{ansvarligId}/")
    @ApiOperation(value = "Henter oppgaver som hører til en gitt Enhet", notes = (""))
    public Response hentOppgaveLister(@PathParam("ansvarligEnhetId") String ansvarligEnhetId,@PathParam("ansvarligId") String ansvarligId) {

        try {
            System.out.println("ansvarligEnhetId:"+ansvarligEnhetId);
            List<Oppgave> oppgaver = gsakService.finnOppgaveListe(ansvarligEnhetId,ansvarligId,null,null,null,null);
            return Response.ok(oppgaver).build();
        } catch (NotFoundException notFoundException) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
/*

    @POST
    @Path("{behandlingID}")
    @ApiOperation(value = "Tjeneste for å registrere opplysninger fra papirsøknaden manuelt.")
    public Response registrerSøknad(@PathParam("behandlingID") long behandlingID, @ApiParam("Søknadsdata") SoeknadDokument soeknadDokument) {
        valideringService.validerOpplysninger(soeknadDokument);
        SoeknadDokument soeknad = soeknadService.registrerSøknad(behandlingID, soeknadDokument);

        SoeknadDto soeknadDto = new SoeknadDto(behandlingID, soeknad);
        return Response.ok(soeknadDto).build();
    }
*/

}
