package no.nav.melosys.tjenester.gui;

import java.util.*;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"avklartefakta"})
@Path("/avklartefakta")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class AvklartefaktaTjeneste extends RestTjeneste {

    private AvklartefaktaService avklartefaktaService;

    private final Tilgang tilgang;

    @Autowired
    public AvklartefaktaTjeneste(AvklartefaktaService avklartefaktaService, Tilgang tilgang) {
        this.avklartefaktaService = avklartefaktaService;
        this.tilgang = tilgang;
    }

    @GET
    @Path("{behandlingID}")
    @ApiOperation(value = "Henter avklartefakta for en gitt behandling")
    public Set<AvklartefaktaDto> hentAvklarteFakta(@PathParam("behandlingID") long behandlingID) {

        Set<AvklartefaktaDto> avklartefaktaDtoer;
        try {
            tilgang.sjekk(behandlingID);
            avklartefaktaDtoer = avklartefaktaService.hentAvklarteFakta(behandlingID);
        } catch (IkkeFunnetException e) {
            throw new NotFoundException(e);
        } catch (SikkerhetsbegrensningException e) {
            throw new ForbiddenException(e);
        } catch (TekniskException e) {
            throw new InternalServerErrorException(e);
        }

        return avklartefaktaDtoer;
    }

    @POST
    @Path("{behandlingID}")
    @ApiOperation(value = "Lagre avklartefakta")
    public Response lagraAvklarteFakta(@PathParam("behandlingID") long behandlingID,
                                       @ApiParam("AvklartefaktaData") Set<AvklartefaktaDto> avklartefaktaDtoer) {
        try {
            tilgang.sjekk(behandlingID);
            avklartefaktaService.lagreAvklarteFakta(behandlingID, avklartefaktaDtoer);
        } catch (IkkeFunnetException e) {
            throw new NotFoundException(e);
        } catch (SikkerhetsbegrensningException e) {
            throw new ForbiddenException(e);
        } catch (TekniskException e) {
            throw new InternalServerErrorException(e);
        }

        return Response.ok().build();
    }
}
