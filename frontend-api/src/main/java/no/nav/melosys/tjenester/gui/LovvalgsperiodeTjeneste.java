package no.nav.melosys.tjenester.gui;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.*;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.tjenester.gui.dto.LovvalgsperiodeDto;
import no.nav.melosys.tjenester.gui.dto.PeriodeDto;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = { "lovvalgsperioder" })
@Service
@Path("/")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class LovvalgsperiodeTjeneste extends RestTjeneste {

    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final Tilgang tilgang;

    public LovvalgsperiodeTjeneste(LovvalgsperiodeService lovvalgsperiodeService, Tilgang tilgang) {
        super();
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.tilgang = tilgang;
    }

    @GET
    @Path("/lovvalgsperioder/{behandlingsid}")
    @ApiOperation(value = "Henter en lovvalgsperiode for en gitt behandling", response = LovvalgsperiodeDto.class)
    @ApiResponses({ @ApiResponse(code = 404, message = "Dersom behandlingsid-en ikke fins.") })
    public Response hentLovvalgsperioder(@PathParam("behandlingsid") long behandlingsid) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        tilgang.sjekk(behandlingsid);
        Collection<LovvalgsperiodeDto> resultat = lovvalgsperiodeService
                .hentLovvalgsperioder(behandlingsid)
                .stream()
                .map(LovvalgsperiodeDto::av)
                .collect(Collectors.toList());
        return Response.ok(resultat).build();
    }

    @POST
    @Path("/lovvalgsperioder/{behandlingsid}")
    @ApiOperation("Lagrer en lovvalgsperiode for en gitt behandling.")
    @ApiResponses({ @ApiResponse(code = 404, message = "Dersom behandlingsid-en ikke fins.") })
    public Collection<LovvalgsperiodeDto> lagreLovvalgsperioder(@PathParam("behandlingsid") long behandlingsid,
            @ApiParam(value = "En liste av lovvalgsperioder å lagre.") Collection<LovvalgsperiodeDto> lovvalgsperiodeDtoer) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        tilgang.sjekk(behandlingsid);
        List<Lovvalgsperiode> lovvalgsperioder = lovvalgsperiodeDtoer.stream()
                .map(LovvalgsperiodeDto::til)
                .collect(Collectors.toList());
        lovvalgsperiodeService.lagreLovvalgsperioder(behandlingsid, lovvalgsperioder);
        return lovvalgsperiodeDtoer;
    }

    @GET
    @Path("/opprinneligLovvalgsperiode/{behandlingsid}")
    @ApiOperation(value = "Henter den opprinnelig lovvalgsperioden en replikert avsluttet behandling har", response = LovvalgsperiodeDto.class)
    @ApiResponses({ @ApiResponse(code = 404, message = "Dersom behandlingsid-en ikke fins.") })
    public Map<String, PeriodeDto> hentOpprinneligLovvalgsperiode(@PathParam("behandlingsid") long behandlingsid) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        tilgang.sjekk(behandlingsid);
        Lovvalgsperiode lovvalgsperiode = lovvalgsperiodeService.hentOpprinneligLovvalgsperiode(behandlingsid);
        PeriodeDto periodeDto = new PeriodeDto(lovvalgsperiode.getFom(), lovvalgsperiode.getTom());
        return ImmutableMap.of("opprinneligLovvalgsperiode", periodeDto);
    }
}
