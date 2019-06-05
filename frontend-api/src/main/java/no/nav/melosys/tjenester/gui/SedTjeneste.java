package no.nav.melosys.tjenester.gui;

import java.util.Collections;
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
import no.nav.melosys.integrasjon.eessi.dto.InstitusjonDto;
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
public class SedTjeneste extends RestTjeneste {

    private final SedService sedService;

    private final BehandlingRepository behandlingRepository;

    @Autowired
    public SedTjeneste(SedService sedService, BehandlingRepository behandlingRepository) {
        this.sedService = sedService;
        this.behandlingRepository = behandlingRepository;
    }

    @GET
    @Path("/mottakerinstitusjoner/{bucType}")
    @ApiOperation(
        value = "Henter mottakerinstitusjoner for alle land for den oppgitte BUCen.",
        response = InstitusjonDto.class,
        responseContainer = "List"
    )
    public Response hentMottakerinstitusjoner(@PathParam("bucType") String bucType) {
        List<InstitusjonDto> mottakerinstitusjoner = sedService.hentMottakerinstitusjoner(bucType);
        return Response.ok(mottakerinstitusjoner).build();
    }

    @POST
    @Path("/opprettbuc/{behandlingID}")
    @ApiOperation(
        value = "Oppretter en sak i RINA og sakens første tilgjengelige SED. Returnerer en URL til saken i RINA.",
        response = String.class
    )
    public Response opprettBuc(@ApiParam NyBucDto nyBucDto, @PathParam("behandlingID") long behandlingID) {
        Behandling behandling = behandlingRepository.findWithSaksopplysningerById(behandlingID);

        OpprettSedDto opprettSedDto =
            sedService.opprettBucOgSed(behandling, nyBucDto.getBucType(), nyBucDto.getMottakerLand(), nyBucDto.getMottakerId());

        if (opprettSedDto == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.ok(opprettSedDto.getRinaUrl()).build();
    }

    @GET
    @Path("/sedunderarbeid/{behandlingID}")
    @ApiOperation(
        value = "Returnerer en liste av seder som er under arbeid for gjeldende sak.",
        response = SedUnderArbeidDto.class,
        responseContainer = "List"
    )
    @Transactional
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

        if (behandling == null) {
            return Response.ok(Collections.emptyList()).build();
        }

        List<SedUnderArbeidDto> seder = sedService.hentTilknyttedeSeder(behandling.getFagsak().getGsakSaksnummer()).stream()
            .map(tilSedUnderArbeidDto).collect(Collectors.toList());

        return Response.ok(seder).build();
    }
}
