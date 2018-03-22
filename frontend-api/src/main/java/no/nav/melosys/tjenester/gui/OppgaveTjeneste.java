package no.nav.melosys.tjenester.gui;

import java.util.Optional;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.service.Oppgaveplukker;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.PlukkOppgaveInnDto;
import no.nav.melosys.tjenester.gui.dto.PlukketOppgaveDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"oppgaver"})
@Path("/oppgaver")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class OppgaveTjeneste {

    private Oppgaveplukker oppgaveplukker;

    @Autowired
    public OppgaveTjeneste(Oppgaveplukker oppgaveplukker) {
        this.oppgaveplukker = oppgaveplukker;
    }

    @POST
    @Path("{plukk}")
    @ApiOperation(value = "Plukker fra GSAK neste oppgave som saksbehandler skal arbeide med.")
    public Response plukkOppgave(PlukkOppgaveInnDto plukkDto) {
        String ident = SubjectHandler.getInstance().getUserID();

        Optional<Oppgave> plukket = oppgaveplukker.plukkOppgave(ident, plukkDto.getOppgavetype(), plukkDto.getSakstyper(), plukkDto.getBehandlingstyper());

        if (plukket.isPresent()) {
            Oppgave oppgave = plukket.get();

            PlukketOppgaveDto dto = new PlukketOppgaveDto();
            dto.setOppgaveId(oppgave.getOppgaveId());
            dto.setOppgavetype(oppgave.getOppgavetype().name());
            dto.setSaksnummer(oppgave.getSaksnummer());
            dto.setJournalpostId(oppgave.getDokumentId());

            return Response.ok(dto).build();
        } else {
            return Response.ok().build();
        }

    }

}
