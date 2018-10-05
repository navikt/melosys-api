package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.*;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;
import no.nav.melosys.tjenester.gui.util.JsonResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"avklartefakta"})
@Path("/avklartefakta")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class AvklarteFaktaTjeneste extends RestTjeneste {

    private String jsonFaktaAvklaring;

    private AvklartefaktaService avklartefaktaService;

    @Autowired
    public AvklarteFaktaTjeneste(ResourceLoader resourceLoader, AvklartefaktaService avklartefaktaService) throws IOException {
        jsonFaktaAvklaring = JsonResourceLoader.load(resourceLoader, "faktaavklaring.json");
        this.avklartefaktaService = avklartefaktaService;
    }

    @GET
    @Path("{behandlingID}")
    @ApiOperation(value = "Henter faktaavklaring for en gitt søknad")
    public Response hentFaktaavklaring(@PathParam("behandlingID") long behandlingID) {

        Set<AvklartefaktaDto> avklartefaktaDtoer = null;
        try {
            avklartefaktaDtoer = avklartefaktaService.hentAvklarteFakta(behandlingID);
        } catch (IkkeFunnetException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }
        return Response.ok().entity(avklartefaktaDtoer).build();
    }

    @POST
    @Path("{behandlingID}")
    @ApiOperation(value = "Lagre bostedavklaring")
    public Response postAvklaring(@PathParam("behandlingID") long behandlingID,
                                  @ApiParam("AvklartefaktaData") Set<AvklartefaktaDto> avklartefaktaDtoer) {
        try {
            avklartefaktaService.lagreAvklarteFakta(behandlingID, avklartefaktaDtoer);
        } catch (IkkeFunnetException e) {
            return Response.status(Response.Status.NOT_FOUND).entity(e.getMessage()).build();
        }

        return Response.ok(avklartefaktaDtoer).build();
    }
}
