package no.nav.melosys.tjenester.gui;

import java.util.List;
import java.util.Optional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.Oppgavetype;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.oppgave.Oppgaveplukker;
import no.nav.melosys.service.oppgave.dto.OppgaveDto;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.PlukkOppgaveInnDto;
import no.nav.melosys.tjenester.gui.dto.PlukketOppgaveDto;
import no.nav.melosys.tjenester.gui.dto.TilbakeleggingDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"oppgaver"})
@Path("/oppgaver")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class OppgaveTjeneste extends RestTjeneste {
    private Oppgaveplukker oppgaveplukker;
    private OppgaveService oppgaveService;

    @Autowired
    public OppgaveTjeneste(Oppgaveplukker oppgaveplukker, OppgaveService oppgaveService) {
        this.oppgaveplukker = oppgaveplukker;
        this.oppgaveService = oppgaveService;
    }

    @POST
    @Path("/plukk")
    @ApiOperation(value = "Plukker fra GSAK neste oppgave som saksbehandler skal arbeide med.")
    public Response plukkOppgave(PlukkOppgaveInnDto plukkDto) {
        String ident = SubjectHandler.getInstance().getUserID();

        Oppgavetype oppgavetype = Oppgavetype.valueOf(plukkDto.getOppgavetype());

        Optional<Oppgave> plukket = oppgaveplukker.plukkOppgave(ident, oppgavetype, plukkDto.getSakstyper(), plukkDto.getBehandlingstyper());

        if (plukket.isPresent()) {
            Oppgave oppgave = plukket.get();

            PlukketOppgaveDto dto = new PlukketOppgaveDto();
            dto.setOppgaveID(oppgave.getOppgaveId());
            dto.setOppgavetype(oppgave.getOppgavetype().name());
            dto.setSaksnummer(oppgave.getGsakSaksnummer());
            dto.setJournalpostID(oppgave.getDokumentId());

            return Response.ok(dto).build();
        } else {
            return Response.ok().build();
        }

    }

    @POST
    @ApiOperation(value = "Legger tilbake oppgaven med gitt oppgaveId i GSAK")
    public Response leggTilbakeOppgave(@ApiParam("Tilbakeleggingsinformasjon") TilbakeleggingDto tilbakelegging) {
        String ident = SubjectHandler.getInstance().getUserID();

        oppgaveplukker.leggTilbakeOppgave(tilbakelegging.getOppgaveId(), ident, tilbakelegging.getBegrunnelse());

        return Response.ok().build();
    }

    @GET
    @Path("/oversikt")
    @ApiOperation(value = "Henter alle oppgaver som er tildelt en gitt saksbehandler.")
    public Response mineOppgaver() {
        String ident = SubjectHandler.getInstance().getUserID();
        List<OppgaveDto> oppgaveDtoListe = oppgaveService.hentOppgaver(ident);
        return Response.ok(oppgaveDtoListe).build();
    }
}
