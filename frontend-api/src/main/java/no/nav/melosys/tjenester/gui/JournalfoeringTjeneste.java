package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.journalforing.JournalfoeringService;
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto;
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
    public Response hentJournalpostOpplysninger(@PathParam("journalpostID") String journalpostID) throws IntegrasjonException, FunksjonellException {
        log.debug("Journalpost med ID {} hentes.", journalpostID);
        JournalpostDto journalpostDto = JournalpostDto.av(journalføringService.hentJournalpost(journalpostID));
        return Response.ok(journalpostDto).build();
    }

    @POST
    @Path("opprett")
    @ApiOperation(value = "Opprett sak og journalfør.")
    public void opprettSakOgJournalfør(@ApiParam JournalfoeringOpprettDto journalfoeringDto) throws MelosysException {
        journalføringService.journalfoer(journalfoeringDto);
    }

    @POST
    @Path("tilordne")
    @ApiOperation(value = "Tilordne sak og journalfør.")
    public void tilordneSakOgJournalfør(@ApiParam JournalfoeringTilordneDto journalfoeringDto) throws FunksjonellException, TekniskException {
        journalføringService.tilordneSakOgJournalfør(journalfoeringDto);
    }
}
