package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.lovvalgsperiode.OpprettLovvalgsperiodeService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.dto.OpprettLovvalgsperiodeDto;
import no.nav.melosys.tjenester.gui.dto.periode.LovvalgsperiodeDto;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Protected
@RestController
@RequestMapping()
@Api(tags = {"lovvalgsperioder"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class LovvalgsperiodeTjeneste {

    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final OpprettLovvalgsperiodeService opprettLovvalgsperiodeService;
    private final Aksesskontroll aksesskontroll;

    public LovvalgsperiodeTjeneste(LovvalgsperiodeService lovvalgsperiodeService,
                                   OpprettLovvalgsperiodeService opprettLovvalgsperiodeService,
                                   Aksesskontroll aksesskontroll) {
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.opprettLovvalgsperiodeService = opprettLovvalgsperiodeService;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping("/lovvalgsperioder/{behandlingID}")
    @ApiOperation(value = "Henter en lovvalgsperiode for en gitt behandling", response = LovvalgsperiodeDto.class)
    @ApiResponses({@ApiResponse(code = 404, message = "Dersom behandlingsid-en ikke fins.")})
    public ResponseEntity<Collection<LovvalgsperiodeDto>> hentLovvalgsperioder(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriser(behandlingID);
        Collection<LovvalgsperiodeDto> resultat = lovvalgsperiodeService
            .hentLovvalgsperioder(behandlingID)
            .stream()
            .map(LovvalgsperiodeDto::av)
            .toList();
        return ResponseEntity.ok(resultat);
    }

    @PostMapping("/behandlinger/{behandlingID}/lovvalgsperioder")
    @ApiOperation("Oppretter en lovvalgsperiode")
    @ApiResponses({@ApiResponse(code = 404, message = "Dersom behandlingsid-en ikke fins.")})
    public ResponseEntity<Collection<LovvalgsperiodeDto>> opprettLovvalgsperiode(@PathVariable("behandlingID") long behandlingsid,
                                                                                 @RequestBody OpprettLovvalgsperiodeDto opprettLovvalgsperiodeDto) {
        aksesskontroll.autoriserSkriv(behandlingsid);

        return ResponseEntity.ok(List.of(LovvalgsperiodeDto.av(
            opprettLovvalgsperiodeService.opprettLovvalgsperiode(behandlingsid, opprettLovvalgsperiodeDto.tilRequest()))));
    }

    @PutMapping("/behandlinger/{behandlingID}/lovvalgsperioder/{lovvalgsperiodeId}")
    @ApiOperation("Oppdaterer en lovvalgsperiode")
    @ApiResponses({@ApiResponse(code = 404, message = "Dersom lovvalgsperiode-en ikke fins.")})
    public ResponseEntity<LovvalgsperiodeDto> oppdaterLovvalgsperiode(@PathVariable("behandlingID") long behandlingsid,
                                                                      @PathVariable("lovvalgsperiodeId") long lovvalgsperiodeId,
                                                                      @RequestBody LovvalgsperiodeDto lovvalgsperiodeDto) {
        aksesskontroll.autoriserSkriv(behandlingsid);

        return ResponseEntity.ok(LovvalgsperiodeDto.av(
            lovvalgsperiodeService.oppdaterLovvalgsperiode(lovvalgsperiodeId, lovvalgsperiodeDto.til())));
    }

    @DeleteMapping("/behandlinger/{behandlingID}/lovvalgsperioder/{lovvalgsperiodeId}")
    @ApiOperation("Sletter en lovvalgsperiode")
    public ResponseEntity<LovvalgsperiodeDto> slettLovvalgsperiode(@PathVariable("behandlingID") long behandlingsid,
                                                                      @PathVariable("lovvalgsperiodeId") long lovvalgsperiodeId) {
        aksesskontroll.autoriserSkriv(behandlingsid);

        lovvalgsperiodeService.slettLovvalgsperiode(lovvalgsperiodeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/lovvalgsperioder/{behandlingID}")
    @ApiOperation("Lagrer en lovvalgsperiode for en gitt behandling.")
    @ApiResponses({@ApiResponse(code = 404, message = "Dersom behandlingsid-en ikke fins.")})
    public ResponseEntity<Collection<LovvalgsperiodeDto>> lagreLovvalgsperioder(@PathVariable("behandlingID") long behandlingsid,
                                                                                @RequestBody Collection<LovvalgsperiodeDto> lovvalgsperiodeDtoer) {
        aksesskontroll.autoriserSkriv(behandlingsid);
        List<Lovvalgsperiode> lovvalgsperioder = lovvalgsperiodeDtoer.stream()
            .map(LovvalgsperiodeDto::til)
            .toList();
        lovvalgsperiodeService.lagreLovvalgsperioder(behandlingsid, lovvalgsperioder);
        return ResponseEntity.ok(lovvalgsperiodeDtoer);
    }

    @GetMapping("/lovvalgsperioder/{behandlingID}/opprinnelig")
    @ApiOperation(value = "Henter den opprinnelig lovvalgsperioden en replikert avsluttet behandling har")
    @ApiResponses({@ApiResponse(code = 404, message = "Dersom behandlingsid-en ikke fins.")})
    public ResponseEntity<Map<String, PeriodeDto>> hentOpprinneligLovvalgsperiode(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriser(behandlingID);
        var lovvalgsperiode = lovvalgsperiodeService.hentOpprinneligLovvalgsperiode(behandlingID);
        var periodeDto = new PeriodeDto(lovvalgsperiode.getFom(), lovvalgsperiode.getTom());
        return ResponseEntity.ok(Map.of("opprinneligLovvalgsperiode", periodeDto));
    }
}
