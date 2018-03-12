package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.service.Oppgaveplukker;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"oppgaver"})
@Path("/oppgaver")
@Service
@Scope(value = WebApplicationContext.SCOPE_APPLICATION)
public class OppgaveTjeneste {

    private Oppgaveplukker oppgaveplukker;

    @Autowired
    public OppgaveTjeneste(Oppgaveplukker oppgaveplukker) {
        this.oppgaveplukker = oppgaveplukker;
    }

    @GET
    @Path("{plukk}")
    @ApiOperation(value = "Plukker fra GSAK neste oppgave som saksbehandler skal arbeide med.")
    public Response plukkOppgave() { //FIXME avklare parametre.
        String ident = SpringSubjectHandler.getUserID();

        List<String> fagområdeKodeListe = new ArrayList<>();
        fagområdeKodeListe.add("MED");
        fagområdeKodeListe.add("UFM");

        oppgaveplukker.plukkOppgave(ident, fagområdeKodeListe, null, null);

        return Response.ok().build();
    }

}
