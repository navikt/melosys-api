package no.nav.melosys.tjenester.gui;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.integrasjon.eessi.dto.OpprettSedDto;
import no.nav.melosys.integrasjon.eessi.dto.SedinfoDto;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.dokument.sed.SedService;
import no.nav.melosys.tjenester.gui.dto.sed.NyBucDto;
import no.nav.melosys.tjenester.gui.dto.sed.SedUnderArbeidDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"sed"})
@Path("/sed")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@Transactional // TODO: Liker ikke å ha denne her, men trengs for å hente vilkaarsresultat
public class SedTjeneste extends RestTjeneste {

    private final SedService sedService;

    private final BehandlingRepository behandlingRepository;

    @Autowired
    public SedTjeneste(SedService sedService, BehandlingRepository behandlingRepository) {
        this.sedService = sedService;
        this.behandlingRepository = behandlingRepository;
    }

    @GET
    @Path("/informasjonombuc")
    @ApiOperation(
        value = "Henter land, mottakerinstitusjoner og første SED for hver av de oppgitte BUCene.",
        // notes = "",
        response = Object.class
    )
    public Response hentBucerMedLandOgMottakerinstitusjoner() {
        // Hent mottakerinstitusjoner
        // Hent buc og første sed
        return null;
    }

    @POST
    @Path("/opprettbuc/{behandlingID}")
    @ApiOperation(
        value = "Oppretter en sak i RINA og sakens første tilgjengelige SED. Returnerer rinaUrl.",
        response = String.class
    )
    public Response opprettBuc(@ApiParam NyBucDto nyBucDto, @PathParam("behandlingID") long behandlingID) {
        Behandling behandling = behandlingRepository.findWithSaksopplysningerById(behandlingID);

        OpprettSedDto opprettSedDto =
            sedService.opprettBucOgSed(behandling, nyBucDto.getBucType(), nyBucDto.getMottakerLand(), nyBucDto.getMottakerId());

        // TODO: nullsjekk + error obj
        return Response.ok(opprettSedDto.getRinaUrl()).build();
    }

    @GET
    @Path("/sedunderarbeid/{behandlingID}")
    @ApiOperation(
        value = "Returnerer en liste av seder som er under arbeid for gjeldende sak.",
        response = SedUnderArbeidDto.class,
        responseContainer = "List"
    )
    public Response hentSederUnderArbeid(@PathParam("behandlingID") long behandlingID) {
        Function<SedinfoDto, SedUnderArbeidDto> tilSedUnderArbeidDto = sedinfoDto -> {
            SedUnderArbeidDto sedUnderArbeidDto = new SedUnderArbeidDto();
            sedUnderArbeidDto.setOpprettetDato(sedinfoDto.getOpprettetDato());
            sedUnderArbeidDto.setRinaUrl(sedinfoDto.getRinaUrl());
            sedUnderArbeidDto.setSedType(sedinfoDto.getSedType());
            sedUnderArbeidDto.setStatus(sedinfoDto.getStatus());
            return sedUnderArbeidDto;
        };

        Behandling behandling = behandlingRepository.findWithSaksopplysningerById(behandlingID);
        List<SedUnderArbeidDto> seder = sedService.hentTilknyttedeSeder(behandling.getFagsak().getGsakSaksnummer()).stream()
            .map(tilSedUnderArbeidDto).collect(Collectors.toList());

        // TODO: nullsjekk + error obj
        return Response.ok(seder).build();
    }
}
