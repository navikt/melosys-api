package no.nav.melosys.tjenester.gui;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"rydde-oppgaver"})
@Profile("test")
@Path("/rydde-oppgaver")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class RyddOppgaverTjeneste extends RestTjeneste {

    private static final Logger log = LoggerFactory.getLogger(RyddOppgaverTjeneste.class);

    private final OppgaveService oppgaveService;

    @Autowired
    public RyddOppgaverTjeneste(OppgaveService oppgaveService) {
        this.oppgaveService = oppgaveService;
    }

    @GET
    @Path("/alle")
    @ApiOperation(
        value = "Henter alle oppgaver som er tildelt en gitt saksbehandler og oppdater status på oppgaven som ferdigstilt."
    )
    public Response ryddOppgaver() {
        String ident = SubjectHandler.getInstance().getUserID();
        try {
            oppgaveService.ferdigstillOppgaverforAnsvarlig(ident);
            return Response.ok().build();
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
