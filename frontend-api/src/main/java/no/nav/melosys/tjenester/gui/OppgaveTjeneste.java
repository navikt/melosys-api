package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.*;
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
import no.nav.melosys.service.oppgave.dto.*;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.OppgaveOversiktDto;
import no.nav.melosys.tjenester.gui.dto.PlukketOppgaveDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(value = "/oppgaver", tags = "oppgaver")
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
    @ApiOperation(value = "Plukker fra GSAK neste oppgave som saksbehandler skal arbeide med.", response = PlukketOppgaveDto.class)
    public Response plukkOppgave(@ApiParam PlukkOppgaveInnDto plukkDto) {
        String ident = SubjectHandler.getInstance().getUserID();

        try {
            Optional<Oppgave> plukket = oppgaveplukker.plukkOppgave(ident, plukkDto);

            if (plukket.isPresent()) {
                Oppgave oppgave = plukket.get();
                PlukketOppgaveDto dto = new PlukketOppgaveDto();

                dto.setOppgaveID(oppgave.getOppgaveId());
                if (oppgave.erBehandling()) {
                    dto.setOppgavetype(Oppgavetype.BEH_SAK.getKode());
                    dto.setSaksnummer(oppgave.getSaksnummer());
                } else if (oppgave.erJournalFøring()) {
                    dto.setOppgavetype(Oppgavetype.JFR.getKode());
                }
                dto.setJournalpostID(oppgave.getJournalpostId());

                return Response.ok(dto).build();
            } else {
                return Response.ok().build();
            }

        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (IkkeFunnetException e) {
            log.info("Ingen oppgaver funnet for ident {}. Feilmelding: ", ident, e);
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (FunksjonellException e) {
            log.info("Funksjonell feil: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (TekniskException e) {
            log.error("Uventet teknisk Feil", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/tilbakelegge")
    @ApiOperation(value = "Legger tilbake oppgave knyttet til gitt behandlingID i GSAK.")
    public Response leggTilbakeOppgave(@ApiParam TilbakeleggingDto tilbakelegging) {
        String ident = SubjectHandler.getInstance().getUserID();

        try {
            oppgaveplukker.leggTilbakeOppgave(ident, tilbakelegging);
            return Response.ok().build();
        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (IkkeFunnetException e) {
            log.error("Ingen oppgaver funnet for ident {}. Feilmelding: ", ident, e);
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (FunksjonellException e) {
            log.info("Funksjonell feil: {}", e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (TekniskException e) {
            log.error("Uventet teknisk Feil", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/oversikt")
    @ApiOperation(
        value = "Henter alle oppgaver som er tildelt en gitt saksbehandler.",
        response = OppgaveOversiktDto.class)
    public Response mineOppgaver() {
        String ident = SubjectHandler.getInstance().getUserID();
        List<OppgaveDto> oppgaveDtoListe;
        try {
            oppgaveDtoListe = oppgaveService.hentOppgaverMedAnsvarlig(ident);
        } catch (SikkerhetsbegrensningException e) {
            throw new ForbiddenException(e.getMessage());
        } catch (IkkeFunnetException e) {
            log.info("IkkeFunnetException: {}", e.getMessage());
            throw new NotFoundException(e.getMessage());
        } catch (TekniskException e) {
            log.error("TekniskException", e);
            throw new InternalServerErrorException("Teknisk feil: " + e.getMessage());
        } catch (FunksjonellException e) {
            log.error("FunksjonellException", e);
            throw new BadRequestException("Funksjonell feil: " + e.getMessage());
        }

        OppgaveOversiktDto oversiktDto = new OppgaveOversiktDto();
        List<JournalfoeringsoppgaveDto> journalføring = new ArrayList<>();
        List<BehandlingsoppgaveDto> saksbehandling = new ArrayList<>();

        for (OppgaveDto oppgaveDto : oppgaveDtoListe) {
            if (oppgaveDto instanceof JournalfoeringsoppgaveDto) {
                journalføring.add((JournalfoeringsoppgaveDto) oppgaveDto);
            } else if (oppgaveDto instanceof BehandlingsoppgaveDto) {
                saksbehandling.add((BehandlingsoppgaveDto) oppgaveDto);
            } else {
                log.warn("Ukjent oppgavetype {}: ", oppgaveDto.getClass().getSimpleName());
            }
        }
        oversiktDto.setJournalforing(journalføring);
        oversiktDto.setSaksbehandling(saksbehandling);
        return Response.ok(oversiktDto).build();
    }

    @GET
    @Path("/sok")
    @ApiOperation(
        value = "Henter alle oppgaver knyttet til en gitt bruker.",
        response = OppgaveDto.class,
        responseContainer = "List")
    public Response hentOppgaver(@QueryParam("fnr") @ApiParam("Fødselsnummer eller D-nummer.")  String fnr) {
        try {
            List<OppgaveDto> oppgaver = oppgaveService.hentOppgaverMedBruker(fnr);
            return Response.ok(oppgaver).build();
        } catch (SikkerhetsbegrensningException e) {
            throw new ForbiddenException(e.getMessage());
        } catch (IkkeFunnetException e) {
            log.info("IkkeFunnetException: {}", e.getMessage());
            throw new NotFoundException(e.getMessage());
        } catch (TekniskException e) {
            log.error("TekniskException", e);
            throw new InternalServerErrorException("Teknisk feil: " + e.getMessage());
        } catch (FunksjonellException e) {
            log.error("FunksjonellException", e);
            throw new BadRequestException("Funksjonell feil: " + e.getMessage());
        }
    }
}
