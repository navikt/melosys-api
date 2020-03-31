package no.nav.melosys.tjenester.gui;

import java.util.List;
import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.tjenester.gui.dto.eessi.BucBestillingDto;
import no.nav.melosys.tjenester.gui.dto.eessi.BucInformasjonDto;
import no.nav.melosys.tjenester.gui.dto.eessi.BucerTilknyttetBehandlingDto;
import no.nav.melosys.tjenester.gui.dto.eessi.OpprettBucSvarDto;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/eessi")
@Api(tags = {"eessi"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class EessiTjeneste {
    private static final Logger log = LoggerFactory.getLogger(EessiTjeneste.class);

    private final EessiService eessiService;
    private final BehandlingService behandlingService;

    @Autowired
    public EessiTjeneste(EessiService eessiService, BehandlingService behandlingService) {
        this.eessiService = eessiService;
        this.behandlingService = behandlingService;
    }

    @GetMapping("/mottakerinstitusjoner/{bucType}")
    @ApiOperation(
        value = "Henter mottakerinstitusjoner for alle land for den oppgitte BUC-typen.",
        response = Institusjon.class,
        responseContainer = "List"
    )
    public ResponseEntity hentMottakerinstitusjoner(@PathVariable("bucType") String bucType,
                                                    @RequestParam(value = "landkode", required = false) String landkode) throws MelosysException {
        log.info("Henter mottakerinstitusjoner for BUC {}", bucType);
        return ResponseEntity.ok(eessiService.hentEessiMottakerinstitusjoner(bucType, landkode));
    }

    @PostMapping("/bucer/{behandlingID}/opprett")
    @ApiOperation(
        value = "Oppretter en sak i RINA og sakens første tilgjengelige SED. Returnerer en URL til saken i RINA.",
        response = String.class
    )
    public ResponseEntity opprettBuc(@RequestBody BucBestillingDto nyBucDto, @PathVariable("behandlingID") long behandlingID) throws MelosysException {
        Behandling behandling = behandlingService.hentBehandling(behandlingID);
        OpprettBucSvarDto opprettBucSvarDto = new OpprettBucSvarDto(
            eessiService.opprettBucOgSed(behandling, nyBucDto.getBucType(), List.of(nyBucDto.getMottakerId()))
        );

        return ResponseEntity.ok(opprettBucSvarDto);
    }

    @GetMapping("/bucer/{behandlingID}")
    @ApiOperation(
        value = "Returnerer en liste av bucer for gjeldende behandling.",
        response = BucerTilknyttetBehandlingDto.class
    )
    public ResponseEntity hentBucer(@PathVariable("behandlingID") long behandlingID,
                              @RequestParam(value = "statuser", required = false) List<String> statuser) throws MelosysException {

        Behandling behandling = behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID);
        long gsakSaksnummer = behandling.getFagsak().getGsakSaksnummer();

        log.info("Henter tilknyttede bucer for sakID {}", gsakSaksnummer);
        BucerTilknyttetBehandlingDto bucerDto = new BucerTilknyttetBehandlingDto(
            eessiService.hentTilknyttedeBucer(gsakSaksnummer, statuser).stream()
                .map(BucInformasjonDto::av).collect(Collectors.toList())
        );
        return ResponseEntity.ok(bucerDto);
    }
}
