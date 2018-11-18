package no.nav.melosys.tjenester.gui;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

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
    @ApiOperation(value = "Fatter et vedtak for en gitt behandling")
    public Response fattVedtak(@ApiParam("behandlingID") @PathParam("behandlingID") long behandlingID) throws SikkerhetsbegrensningException, IkkeFunnetException, TekniskException {
        tilgang.sjekk(behandlingID);
        vedtakService.fattVedtak(behandlingID);
        return Response.ok().build();
    }
}
