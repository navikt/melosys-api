package no.nav.melosys.tjenester.gui;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.vedtak.VedtakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = { "vedtak" })
@Path("/vedtak")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class VedtakTjeneste extends RestTjeneste {

    private final VedtakService vedtakService;

    private final Tilgang tilgang;

    @Autowired
    public VedtakTjeneste(VedtakService vedtakService, Tilgang tilgang) {
        this.vedtakService = vedtakService;
        this.tilgang = tilgang;
    }

    @POST
    @Path("{behandlingID}")
    public Response fattVedtak(@PathParam("behandlingID") long behandlingID) {
        try {
            tilgang.sjekk(behandlingID);
            vedtakService.fattVedtak(behandlingID);
            return Response.ok().build();
        } catch (IkkeFunnetException e) {
            throw new NotFoundException(e);
        } catch (SikkerhetsbegrensningException e) {
            throw new ForbiddenException(e);
        } catch (TekniskException e) {
            throw new InternalServerErrorException(e);
        }
    }
}
