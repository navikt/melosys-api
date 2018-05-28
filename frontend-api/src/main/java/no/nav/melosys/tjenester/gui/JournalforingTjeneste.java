package no.nav.melosys.tjenester.gui;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.Journalpost;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.journalforing.JournalforingService;
import no.nav.melosys.service.journalforing.dto.JournalforingDto;
import no.nav.melosys.tjenester.gui.dto.journalforing.DokumentDto;
import no.nav.melosys.tjenester.gui.dto.journalforing.JournalpostDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"journalforing"})
@Path("/journalforing")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class JournalforingTjeneste extends RestTjeneste {

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
            throw new BadRequestException(e.getMessage());
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
