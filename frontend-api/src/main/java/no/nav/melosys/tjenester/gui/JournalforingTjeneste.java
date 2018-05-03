package no.nav.melosys.tjenester.gui;

import java.time.LocalDate;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.DokumentTittel;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.service.journalforing.JournalforingService;
import no.nav.melosys.tjenester.gui.dto.journalforing.AktoerDto;
import no.nav.melosys.tjenester.gui.dto.journalforing.DokumentDto;
import no.nav.melosys.tjenester.gui.dto.journalforing.JournalforingDto;
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
    public JournalpostDto hentJournalpostOpplysninger(@PathParam("journalpostID") String journalpostID) {
        // TODO
        journalføringService.hentJournalpost(journalpostID);

        // FIXME Mocking inntil videre
        JournalpostDto dto = new JournalpostDto();
        dto.setBruker(new AktoerDto("FJERNET", "LILLA HEST"));
        dto.setAvsender(new AktoerDto("FJERNET", "LILLA HEST"));
        dto.setErBrukerAvsender(true);
        dto.setSakstype(FagsakType.EU_EØS);
        DokumentDto dokumentDto = new DokumentDto();
        dokumentDto.setDokumentID("Dok_ID");
        dokumentDto.setMottattDato(LocalDate.now());
        dokumentDto.setTittel(DokumentTittel.SØKNAD_MEDLEMSSKAP.getBeskrivelse());
        dto.setDokument(dokumentDto);
        return dto;
    }

    @POST
    public void journalfør(JournalforingDto journalforingDto) {
        // TODO
    }
}
