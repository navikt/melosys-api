package no.nav.melosys.tjenester.gui;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.Journalpost;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.journalforing.JournalforingService;
import no.nav.melosys.service.journalforing.dto.JournalforingDto;
import no.nav.melosys.tjenester.gui.dto.journalforing.DokumentDto;
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
public class JournalforingTjeneste extends RestTjeneste {

    private static Logger log = LoggerFactory.getLogger(JournalforingTjeneste.class);
    
    private JournalforingService journalføringService;

    @Autowired
    public JournalforingTjeneste(JournalforingService journalføringService) {
        this.journalføringService = journalføringService;
    }

    @GET
    @Path("{journalpostID}")
    public Response hentJournalpostOpplysninger(@PathParam("journalpostID") String journalpostID) {
        Journalpost journalpost;
        try {
            journalpost = journalføringService.hentJournalpost(journalpostID);
        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (TekniskException e) {
            log.error("TekniskException", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        JournalpostDto dto = new JournalpostDto();
        String brukerID = journalpost.getBrukerId();
        dto.setBrukerID(brukerID);
        String avsenderID = journalpost.getAvsenderId();
        dto.setAvsenderID(avsenderID);
        dto.setErBrukerAvsender(brukerID != null && brukerID.equalsIgnoreCase(avsenderID));
        DokumentDto dokumentDto = new DokumentDto();
        dokumentDto.setID(journalpost.getHoveddokumentId());
        dokumentDto.setMottattDato(journalpost.getForsendelseMottatt());
        dokumentDto.setTittel(journalpost.getHoveddokumentTittel());
        dto.setDokument(dokumentDto);
        return Response.ok(dto).build();
    }

    @POST
    @Path("opprett")
    public void opprettSakOgJournalfør(JournalforingDto journalforingDto) {
        try {
            journalføringService.opprettSakOgJournalfør(journalforingDto);
        } catch (FunksjonellException e) {
            throw new BadRequestException(e);
        } catch (TekniskException e) {
            log.error("TekniskException", e);
            throw new InternalServerErrorException(e);
        }
    }

    @POST
    @Path("tilordne")
    public void tilordneSakOgJournalfør(JournalforingDto journalforingDto) {
        try {
            journalføringService.tilordneSakOgJournalfør(journalforingDto);
        } catch (FunksjonellException e) {
            throw new BadRequestException(e.getMessage());
        }
    }
}
