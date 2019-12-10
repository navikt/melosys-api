package no.nav.melosys.tjenester.gui;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.tjenester.gui.dto.periode.LovvalgsperiodeDto;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/lovvalgsperioder")
@Api(tags = { "lovvalgsperioder" })
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class LovvalgsperiodeTjeneste {

    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final TilgangService tilgangService;

    public LovvalgsperiodeTjeneste(LovvalgsperiodeService lovvalgsperiodeService, TilgangService tilgangService) {
        super();
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.tilgangService = tilgangService;
    }

    @GetMapping("{behandlingID}")
    @ApiOperation(value = "Henter en lovvalgsperiode for en gitt behandling", response = LovvalgsperiodeDto.class)
    @ApiResponses({ @ApiResponse(code = 404, message = "Dersom behandlingsid-en ikke fins.") })
    public ResponseEntity hentLovvalgsperioder(@PathVariable("behandlingID") long behandlingsid) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        tilgangService.sjekkTilgang(behandlingsid);
        Collection<LovvalgsperiodeDto> resultat = lovvalgsperiodeService
                .hentLovvalgsperioder(behandlingsid)
                .stream()
                .map(LovvalgsperiodeDto::av)
                .collect(Collectors.toList());
        return ResponseEntity.ok(resultat);
    }

    @PostMapping("{behandlingID}")
    @ApiOperation("Lagrer en lovvalgsperiode for en gitt behandling.")
    @ApiResponses({ @ApiResponse(code = 404, message = "Dersom behandlingsid-en ikke fins.") })
    public Collection<LovvalgsperiodeDto> lagreLovvalgsperioder(@PathVariable("behandlingID") long behandlingsid,
            @RequestBody Collection<LovvalgsperiodeDto> lovvalgsperiodeDtoer) throws FunksjonellException, TekniskException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingsid);
        List<Lovvalgsperiode> lovvalgsperioder = lovvalgsperiodeDtoer.stream()
                .map(LovvalgsperiodeDto::til)
                .collect(Collectors.toList());
        lovvalgsperiodeService.lagreLovvalgsperioder(behandlingsid, lovvalgsperioder);
        return lovvalgsperiodeDtoer;
    }

    @GetMapping("{behandlingID}/opprinnelig")
    @ApiOperation(value = "Henter den opprinnelig lovvalgsperioden en replikert avsluttet behandling har", response = LovvalgsperiodeDto.class)
    @ApiResponses({ @ApiResponse(code = 404, message = "Dersom behandlingsid-en ikke fins.") })
    public Map<String, PeriodeDto> hentOpprinneligLovvalgsperiode(@PathVariable("behandlingID") long behandlingsid) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        tilgangService.sjekkTilgang(behandlingsid);
        Lovvalgsperiode lovvalgsperiode = lovvalgsperiodeService.hentOpprinneligLovvalgsperiode(behandlingsid);
        PeriodeDto periodeDto = new PeriodeDto(lovvalgsperiode.getFom(), lovvalgsperiode.getTom());
        return ImmutableMap.of("opprinneligLovvalgsperiode", periodeDto);
    }
}