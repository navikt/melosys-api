package no.nav.melosys.tjenester.gui;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.abac.TilgangService;
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
    private final TilgangService tilgangService;

    @Autowired
    public EessiTjeneste(EessiService eessiService, BehandlingService behandlingService, TilgangService tilgangService) {
        this.eessiService = eessiService;
        this.behandlingService = behandlingService;
        this.tilgangService = tilgangService;
    }

    @GET
    @Path("/mottakerinstitusjoner/{bucType}")
    @ApiOperation(
        value = "Henter mottakerinstitusjoner for alle land for den oppgitte BUC-typen.",
        response = Institusjon.class,
        responseContainer = "List"
    )
    public Response hentMottakerinstitusjoner(@PathParam("bucType") String bucType) throws MelosysException {
        log.info("Henter mottakerinstitusjoner for BUC {}", bucType);
        return Response.ok(eessiService.hentMottakerinstitusjoner(bucType)).build();
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
                              @QueryParam("status") String status) throws MelosysException {

        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        long gsakSaksnummer = behandling.getFagsak().getGsakSaksnummer();

        log.info("Henter tilknyttede bucer for gsak {}", gsakSaksnummer);
        BucerTilknyttetBehandlingDto bucerDto = new BucerTilknyttetBehandlingDto(eessiService.hentTilknyttedeBucer(gsakSaksnummer, status));
        return Response.ok(bucerDto).build();
    }

    @GET
    @Path("/seder/{behandlingID}/pdf/{sedType}")
    @Produces({"application/pdf", MediaType.APPLICATION_JSON + "; charset=UTF-8"})
    @ApiOperation(
        value = "Henter en sed-pdf av gitt type basert på opplysningene i behandlingen.",
        response = Response.class
    )
    public Response hentSedForhåndsvisning(@PathParam("behandlingID") long behandlingID,
                                           @PathParam("sedType") SedType sedType) throws MelosysException {

        tilgangService.sjekkTilgang(behandlingID);
        byte[] dokument = eessiService.hentSedForhåndsvisning(behandlingID, sedType);

        return Response.ok(dokument)
            .header(HttpHeaders.CONTENT_LENGTH, dokument.length)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; attachment; filename=" + sedType.name() + "_utkast.pdf")
            .build();
    }
}
