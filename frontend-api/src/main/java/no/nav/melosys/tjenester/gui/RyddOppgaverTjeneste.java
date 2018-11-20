package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"rydde-oppgaver"})
@Profile("test")
@Path("/oppgaver/rydd")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class RyddOppgaverTjeneste extends RestTjeneste {

    private final OppgaveService oppgaveService;

    @Autowired
    public RyddOppgaverTjeneste(OppgaveService oppgaveService) {
        this.oppgaveService = oppgaveService;
    }

    @GET
    @ApiOperation(
        value = "Henter alle oppgaver som er tildelt en gitt saksbehandler og oppdater status på oppgaven som ferdigstilt."
    )
    public Response ryddOppgaver() throws TekniskException, FunksjonellException {
        String ident = SubjectHandler.getInstance().getUserID();
        oppgaveService.ferdigstillOppgaverforAnsvarlig(ident);
        return Response.ok().build();
    }
}
