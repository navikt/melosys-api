package no.nav.melosys.tjenester.gui;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.tilgang.TilgangService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodeGetDto;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodePostDto;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodeSkrivDto;
import no.nav.melosys.tjenester.gui.dto.anmodning.AnmodningsperiodeSvarDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@Api(tags = {"anmodningsperioder"})
@RequestMapping("/anmodningsperioder")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AnmodningsperiodeTjeneste {
    private final AnmodningsperiodeService anmodningsperiodeService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final TilgangService tilgangService;

    @Autowired
    public AnmodningsperiodeTjeneste(AnmodningsperiodeService anmodningsperiodeService, LovvalgsperiodeService lovvalgsperiodeService, TilgangService tilgangService) {
        super();
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.tilgangService = tilgangService;
    }

    @GetMapping("{behandlingID}")
    @ApiOperation(value = "Henter anmodningsperioder for en gitt behandling", response = AnmodningsperiodeSkrivDto.class)
    @ApiResponses({@ApiResponse(code = 404, message = "Dersom behandlingID-en ikke fins.")})
    public AnmodningsperiodeGetDto hentAnmodningsperioder(@PathVariable("behandlingID") long behandlingID) {
        tilgangService.sjekkTilgang(behandlingID);
        return AnmodningsperiodeGetDto.av(anmodningsperiodeService.hentAnmodningsperioder(behandlingID));
    }

    @PostMapping("{behandlingID}")
    @ApiOperation("Lagrer anmodningsperioder for en gitt behandling.")
    @ApiResponses({@ApiResponse(code = 404, message = "Dersom behandlingID-en ikke fins.")})
    public AnmodningsperiodeGetDto lagreAnmodningsperioder(@PathVariable("behandlingID") long behandlingID,
                                                           @RequestBody AnmodningsperiodePostDto anmodningsperiodePostDto) {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        Collection<Anmodningsperiode> anmodningsperioder = anmodningsperiodeService.lagreAnmodningsperioder(
            behandlingID, anmodningsperiodePostDto.getAnmodningsperioder().stream().map(AnmodningsperiodeSkrivDto::til)
                .collect(Collectors.toList())
        );
        return AnmodningsperiodeGetDto.av(anmodningsperioder);
    }

    @GetMapping("{anmodningsperiodeID}/svar")
    @ApiOperation("Henter svar på en anmodningsperiode.")
    @ApiResponses({@ApiResponse(code = 404, message = "Dersom anmodningsperioden ikke fins.")})
    public AnmodningsperiodeSvarDto hentAnmodningsperiodeSvar(@PathVariable("anmodningsperiodeID") long anmodningsperiodeID) {

        Optional<Anmodningsperiode> anmodningsperiodeOptional = anmodningsperiodeService.finnAnmodningsperiode(anmodningsperiodeID);

        long behandlingID = anmodningsperiodeOptional.map(Anmodningsperiode::getBehandlingsresultat)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke anmodningsperiode med id " + anmodningsperiodeID)).getId();

        tilgangService.sjekkTilgang(behandlingID);

        Optional<AnmodningsperiodeSvar> svar = anmodningsperiodeOptional.map(Anmodningsperiode::getAnmodningsperiodeSvar);

        return svar.map(AnmodningsperiodeSvarDto::av).orElseGet(AnmodningsperiodeSvarDto::tom);
    }

    @PostMapping("{anmodningsperiodeID}/svar")
    @ApiOperation("Lagrer svar på en anmodningsperiode.")
    @ApiResponses({@ApiResponse(code = 404, message = "Dersom anmodningsperioden ikke fins.")})
    public AnmodningsperiodeSvarDto lagreAnmodningsperiodeSvar(@PathVariable("anmodningsperiodeID") long anmodningsperiodeID,
                                                               @RequestBody AnmodningsperiodeSvarDto anmodningsperiodeSvarDto) {

        long behandlingID = anmodningsperiodeService.finnAnmodningsperiode(anmodningsperiodeID)
            .map(Anmodningsperiode::getBehandlingsresultat)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke anmodningsperiode med id " + anmodningsperiodeID)).getId();
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);

        AnmodningsperiodeSvar svar = anmodningsperiodeService.lagreAnmodningsperiodeSvar(anmodningsperiodeID, anmodningsperiodeSvarDto.til());

        Lovvalgsperiode lovvalgsperiode = Lovvalgsperiode.av(svar, Medlemskapstyper.PLIKTIG);
        lovvalgsperiodeService.lagreLovvalgsperioder(behandlingID, Collections.singleton(lovvalgsperiode));

        return AnmodningsperiodeSvarDto.av(svar);
    }
}
