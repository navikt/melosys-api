package no.nav.melosys.tjenester.gui;

import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.tjenester.gui.dto.eessi.BucBestillingDto;
import no.nav.melosys.tjenester.gui.dto.eessi.BucerTilknyttetBehandlingDto;
import no.nav.melosys.tjenester.gui.dto.eessi.OpprettBucSvarDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"eessi"})
@Path("/eessi")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class EessiTjeneste extends RestTjeneste {
    private static final Logger log = LoggerFactory.getLogger(EessiTjeneste.class);

    private final EessiService eessiService;
    private final BehandlingService behandlingService;

    @Autowired
    public EessiTjeneste(EessiService eessiService, BehandlingService behandlingService) {
        this.eessiService = eessiService;
        this.behandlingService = behandlingService;
    }

    @GET
    @Path("/mottakerinstitusjoner/{bucType}")
    @ApiOperation(
        value = "Henter mottakerinstitusjoner for alle land for den oppgitte BUC-typen.",
        response = Institusjon.class,
        responseContainer = "List"
    )
    public Response hentMottakerinstitusjoner(@PathParam("bucType") String bucType, @QueryParam("landkode") String landkode) throws MelosysException {
        log.info("Henter mottakerinstitusjoner for BUC {}", bucType);
        return Response.ok(eessiService.hentEessiMottakerinstitusjoner(bucType, landkode)).build();
    }

    @POST
    @Path("/bucer/{behandlingID}/opprett")
    @ApiOperation(
        value = "Oppretter en sak i RINA og sakens første tilgjengelige SED. Returnerer en URL til saken i RINA.",
        response = String.class
    )
    public Response opprettBuc(@ApiParam BucBestillingDto nyBucDto, @PathParam("behandlingID") long behandlingID) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        OpprettBucSvarDto opprettBucSvarDto = new OpprettBucSvarDto(
            eessiService.opprettBucOgSed(behandling, nyBucDto.getBucType(), nyBucDto.getMottakerLand(), nyBucDto.getMottakerId())
        );

        return Response.ok(opprettBucSvarDto).build();
    }

    @GET
    @Path("/bucer/{behandlingID}")
    @ApiOperation(
        value = "Returnerer en liste av bucer for gjeldende behandling.",
        response = BucerTilknyttetBehandlingDto.class
    )
    public Response hentBucer(@PathParam("behandlingID") long behandlingID,
                              @QueryParam("statuser") List<String> statuser) throws MelosysException {

        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        long gsakSaksnummer = behandling.getFagsak().getGsakSaksnummer();

        log.info("Henter tilknyttede bucer for gsak {}", gsakSaksnummer);
        BucerTilknyttetBehandlingDto bucerDto = new BucerTilknyttetBehandlingDto(eessiService.hentTilknyttedeBucer(gsakSaksnummer, statuser));
        return Response.ok(bucerDto).build();
    }
}
