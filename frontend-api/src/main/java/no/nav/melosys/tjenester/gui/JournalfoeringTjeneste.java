package no.nav.melosys.tjenester.gui;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.journalforing.JournalfoeringService;
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto;
import no.nav.melosys.tjenester.gui.dto.dokument.DokumentDto;
import no.nav.melosys.tjenester.gui.dto.journalforing.JournalpostDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"journalforing"})
@Path("/journalforing")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class JournalfoeringTjeneste extends RestTjeneste {

    private static Logger log = LoggerFactory.getLogger(JournalfoeringTjeneste.class);
    
    private JournalfoeringService journalføringService;

    @Autowired
    public JournalfoeringTjeneste(JournalfoeringService journalføringService) {
        this.journalføringService = journalføringService;
    }

    @GET
    @Path("{journalpostID}")
    @ApiOperation(value = "Hent journalpost opplysninger.", response = JournalpostDto.class)
    public Response hentJournalpostOpplysninger(@ApiParam @PathParam("journalpostID") String journalpostID) {
        log.debug("Journalpost med ID {} hentes.", journalpostID);
        Journalpost journalpost;
        try {
            journalpost = journalføringService.hentJournalpost(journalpostID);
        } catch (SikkerhetsbegrensningException e) {
            throw new ForbiddenException(e.getMessage());
        } catch (IkkeFunnetException e) {
            log.warn("IkkeFunnetException: {}", e.getMessage());
            throw new NotFoundException(e.getMessage());
        } catch (TekniskException e) {
            log.error("TekniskException", e);
            throw new InternalServerErrorException("Teknisk feil: " + e.getMessage());
        } catch (FunksjonellException e) {
            log.error("FunksjonellException", e);
            throw new BadRequestException("Funksjonell feil: " + e.getMessage());
        }
        JournalpostDto dto = new JournalpostDto();
        dto.setMottattDato(journalpost.getForsendelseMottatt());
        String brukerID = journalpost.getBrukerId();
        dto.setBrukerID(brukerID);
        String avsenderID = journalpost.getAvsenderId();
        dto.setAvsenderID(avsenderID);
        dto.setErBrukerAvsender(brukerID != null && brukerID.equalsIgnoreCase(avsenderID));
        DokumentDto dokumentDto = new DokumentDto(journalpost.getHoveddokument().getDokumentId(), journalpost.getHoveddokument().getTittel());
        dto.setHoveddokument(dokumentDto);
        return Response.ok(dto).build();
    }

    @POST
    @Path("opprett")
    @ApiOperation(value = "Opprett sak og journalfør.")
    public void opprettSakOgJournalfør(@ApiParam JournalfoeringOpprettDto journalfoeringDto) {
        try {
            journalføringService.opprettSakOgJournalfør(journalfoeringDto);
        } catch (SikkerhetsbegrensningException e) {
            throw new ForbiddenException("Ikke tilgang");
        } catch (FunksjonellException e) {
            log.info("Funksjonell feil: {}", e.getMessage());
            throw new BadRequestException(e);
        }
    }

    @POST
    @Path("tilordne")
    @ApiOperation(value = "Tilordne sak og journalfør.")
    public void tilordneSakOgJournalfør(@ApiParam JournalfoeringTilordneDto journalfoeringDto) {
        try {
            journalføringService.tilordneSakOgJournalfør(journalfoeringDto);
        } catch (SikkerhetsbegrensningException e) {
            throw new ForbiddenException("Ikke tilgang");
        } catch (FunksjonellException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
