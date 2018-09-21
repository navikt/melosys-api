package no.nav.melosys.tjenester.gui;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.Journalpost;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.abac.JournalTilgang;
import no.nav.melosys.service.journalforing.JournalfoeringService;
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto;
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
public class JournalfoeringTjeneste extends RestTjeneste {

    private JournalfoeringService journalføringService;

    private final JournalTilgang journalTilgang;

    @Autowired
    public JournalfoeringTjeneste(JournalfoeringService journalføringService, JournalTilgang journalTilgang) {
        this.journalføringService = journalføringService;
        this.journalTilgang = journalTilgang;
    }

    @GET
    @Path("{journalpostID}")
    public Response hentJournalpostOpplysninger(@PathParam("journalpostID") String journalpostID) {
        Journalpost journalpost;
        try {
            journalpost = journalføringService.hentJournalpost(journalpostID);
            journalTilgang.sjekk(journalpost);
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
    public void opprettSakOgJournalfør(JournalfoeringOpprettDto journalfoeringDto) {
        try {
            journalTilgang.sjekk(journalfoeringDto);
            journalføringService.opprettSakOgJournalfør(journalfoeringDto);
        } catch (FunksjonellException e) {
            throw new BadRequestException(e.getMessage());
        } catch (SikkerhetsbegrensningException e) {
            throw new BadRequestException("Ikke tilgang");
        }
    }

    @POST
    @Path("tilordne")
    public void tilordneSakOgJournalfør(JournalfoeringTilordneDto journalfoeringDto) {
        try {
            journalTilgang.sjekk(journalfoeringDto);
            journalføringService.tilordneSakOgJournalfør(journalfoeringDto);
        } catch (FunksjonellException e) {
            throw new BadRequestException(e.getMessage());
        } catch (SikkerhetsbegrensningException e) {
            throw new BadRequestException("Ikke tilgang");
        }
    }
}
