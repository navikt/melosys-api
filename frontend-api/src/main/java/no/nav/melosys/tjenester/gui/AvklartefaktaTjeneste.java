package no.nav.melosys.tjenester.gui;

import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = { "avklartefakta" })
@Path("/avklartefakta")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
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
    @ApiOperation(value = "Henter avklartefakta for en gitt behandling",
                  response = Avklartefakta.class,
                  responseContainer = "Set")
    public Set<AvklartefaktaDto> hentAvklarteFakta(@ApiParam("BehandlingsID") @PathParam("behandlingID") long behandlingID) throws TekniskException, SikkerhetsbegrensningException, IkkeFunnetException {
        tilgang.sjekk(behandlingID);
        return avklartefaktaService.hentAlleAvklarteFakta(behandlingID);
    }

    @POST
    @Path("{behandlingID}")
    @ApiOperation(value = "Lagre avklartefakta")
    public Set<AvklartefaktaDto> lagreAvklarteFakta(@PathParam("behandlingID") long behandlingID,
                                                    @ApiParam("AvklartefaktaData") Set<AvklartefaktaDto> avklartefaktaDtoer) throws TekniskException, FunksjonellException {
        tilgang.sjekkRedigerbar(behandlingID);

        avklartefaktaService.lagreAvklarteFakta(behandlingID, avklartefaktaDtoer);
        return avklartefaktaService.hentAlleAvklarteFakta(behandlingID);
    }
}
