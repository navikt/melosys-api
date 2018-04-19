package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.swagger.annotations.Api;
import no.nav.melosys.service.journalforing.JournalforingService;
import no.nav.melosys.tjenester.gui.dto.journalforing.JournalpostDto;
import no.nav.melosys.tjenester.gui.dto.journalforing.JournalforingDto;
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
        JournalpostDto journalpostDto = new JournalpostDto();
        journalføringService.hentJournalpost(journalpostID);
        return journalpostDto;
    }

    @POST
    public void journalfør(JournalforingDto journalforingDto) {

    }
}
