package no.nav.melosys.tjenester.gui;

import java.util.List;
import java.util.Optional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.Oppgavetype;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.oppgave.Oppgaveplukker;
import no.nav.melosys.service.oppgave.dto.OppgaveDto;
import no.nav.melosys.service.oppgave.dto.PlukkOppgaveInnDto;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.MockOppgaveDto;
import no.nav.melosys.tjenester.gui.dto.PlukketOppgaveDto;
import no.nav.melosys.tjenester.gui.dto.TilbakeleggingDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"oppgaver"})
@Path("/oppgaver")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class OppgaveTjeneste extends RestTjeneste {
    private static final Logger log = LoggerFactory.getLogger(OppgaveTjeneste.class);
    private final Oppgaveplukker oppgaveplukker;
    private final OppgaveService oppgaveService;

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
        PlukketOppgaveDto dto = new PlukketOppgaveDto();

        try {
            Optional<Oppgave> plukket = oppgaveplukker.plukkOppgave(ident, plukkDto);

            if (plukket.isPresent()) {
                Oppgave oppgave = plukket.get();

                dto.setOppgaveID(oppgave.getOppgaveId());
                if (oppgave.erBehandling()) {
                    dto.setOppgavetype(Oppgavetype.BEH_SAK.getKode());
                    dto.setSaksnummer(oppgave.getSaksnummer());
                } else if (oppgave.erJournalFøring()) {
                    dto.setOppgavetype(Oppgavetype.JFR.getKode());
                }
                dto.setJournalpostID(oppgave.getJournalpostId());
            }

        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (IkkeFunnetException e) {
            log.error("Ingen oppgaver funnet for ident {}. Feilmelding: {}", ident, e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (FunksjonellException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (TekniskException e) {
            log.error("Uventet teknisk Feil {} ", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        return Response.ok(dto).build();
    }

    @POST
    @Path("/tilbakelegge")
    @ApiOperation(value = "Legger tilbake oppgaven med gitt oppgaveId i GSAK")
    public Response leggTilbakeOppgave(@ApiParam("Tilbakeleggingsinformasjon") TilbakeleggingDto tilbakelegging) {
        String ident = SubjectHandler.getInstance().getUserID();

        try {
            oppgaveplukker.leggTilbakeOppgave(tilbakelegging.getOppgaveId(), ident, tilbakelegging.getBegrunnelse());
            return Response.ok().build();

        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (IkkeFunnetException e) {
            log.error("Ingen oppgaver funnet for ident {}. Feilmelding: {}", ident, e.getMessage());
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (FunksjonellException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (TekniskException e) {
            log.error("Uventet teknisk Feil {} ", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/oversikt")
    @ApiOperation(value = "Henter alle oppgaver som er tildelt en gitt saksbehandler.")
    public Response mineOppgaver() {
        String ident = SubjectHandler.getInstance().getUserID();
        List<OppgaveDto> oppgaveDtoListe;
        try {
            oppgaveDtoListe = oppgaveService.hentOppgaverMedAnsvarlig(ident);
        } catch (TekniskException e) {
            log.error("Uventet teknisk Feil {} ", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        return Response.ok(oppgaveDtoListe).build();
    }

    // FIXME Dette er for å hjelpe testing av oppgavehåndtering.
    @POST
    @Path("/opprett")
    @ApiOperation(value = "Oppretter en mock oppgave")
    public Response opprett(MockOppgaveDto oppgaveDto) {
        String oppgaveID = oppgaveService.opprettOppgave(oppgaveDto.getAnsvarligID(), oppgaveDto.getOppgavetype(), oppgaveDto.getFnr(), oppgaveDto.getJournalpostID(), oppgaveDto.getSaksnummer());

        return Response.ok(oppgaveID).build();
    }


    @GET
    @Path("/sok")
    @ApiOperation(value = "Henter alle oppgaver knyttet til en gitt bruker.")
    public Response hentOppgaver(@QueryParam("fnr") @ApiParam("Fødselsnummer eller D-nummer.")  String fnr) {
        List<OppgaveDto> oppgaver;
        try {
            oppgaver = oppgaveService.hentOppgaverMedBruker(fnr);
        } catch (IkkeFunnetException e) {
            log.error("Finner ingen aktørId for ident {}: {}", fnr, e.getMessage(), e);
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (TekniskException e) {
            log.error("Uventet teknisk Feil {} ", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        return Response.ok(oppgaver).build();
    }
}
