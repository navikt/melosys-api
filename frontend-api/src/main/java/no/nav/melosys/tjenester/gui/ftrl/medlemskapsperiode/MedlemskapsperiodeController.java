package no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode;

import java.util.Collection;
import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService;
import no.nav.melosys.service.ftrl.medlemskapsperiode.OpprettForslagMedlemskapsperiodeService;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode.dto.BestemmelseDto;
import no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode.dto.MedlemskapsperiodeDto;
import no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode.dto.MedlemskapsperiodeOppdateringDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@Api(tags = {"medlemskapsperioder"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class MedlemskapsperiodeController {

    private final MedlemskapsperiodeService medlemskapsperiodeService;
    private final OpprettForslagMedlemskapsperiodeService opprettForslagMedlemskapsperiodeService;
    private final Aksesskontroll aksesskontroll;

    public MedlemskapsperiodeController(MedlemskapsperiodeService medlemskapsperiodeService,
                                      OpprettForslagMedlemskapsperiodeService opprettForslagMedlemskapsperiodeService,
                                      Aksesskontroll aksesskontroll) {
        this.medlemskapsperiodeService = medlemskapsperiodeService;
        this.opprettForslagMedlemskapsperiodeService = opprettForslagMedlemskapsperiodeService;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping("/behandlinger/{behandlingID}/medlemskapsperioder")
    public ResponseEntity<Collection<MedlemskapsperiodeDto>> hentMedlemskapsperioder(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriser(behandlingID);
        return ResponseEntity.ok(
            medlemskapsperiodeService.hentMedlemskapsperioder(behandlingID)
                .stream()
                .map(MedlemskapsperiodeDto::av)
                .collect(Collectors.toSet())
        );
    }

    @PostMapping("/behandlinger/{behandlingID}/medlemskapsperioder")
    public ResponseEntity<MedlemskapsperiodeDto> opprettMedlemskapsperiode(@PathVariable("behandlingID") long behandlingID,
                                                                           @RequestBody MedlemskapsperiodeOppdateringDto medlemskapsperiodeOppdateringDto) {
        aksesskontroll.autoriserSkriv(behandlingID);
        return ResponseEntity.ok(
            MedlemskapsperiodeDto.av(
                medlemskapsperiodeService.opprettMedlemskapsperiode(
                    behandlingID,
                    medlemskapsperiodeOppdateringDto.fomDato(),
                    medlemskapsperiodeOppdateringDto.tomDato(),
                    medlemskapsperiodeOppdateringDto.innvilgelsesResultat(),
                    medlemskapsperiodeOppdateringDto.trygdedekning(),
                    medlemskapsperiodeOppdateringDto.bestemmelse())
            )
        );
    }

    @PutMapping("/behandlinger/{behandlingID}/medlemskapsperioder/{medlemskapsperiodeID}")
    public ResponseEntity<MedlemskapsperiodeDto> oppdaterMedlemskapsperiode(@PathVariable("behandlingID") long behandlingID,
                                                                            @PathVariable("medlemskapsperiodeID") long medlemskapsperiodeID,
                                                                            @RequestBody MedlemskapsperiodeOppdateringDto medlemskapsperiodeOppdateringDto) {
        aksesskontroll.autoriserSkriv(behandlingID);
        return ResponseEntity.ok(
            MedlemskapsperiodeDto.av(
                medlemskapsperiodeService.oppdaterMedlemskapsperiode(
                    behandlingID,
                    medlemskapsperiodeID,
                    medlemskapsperiodeOppdateringDto.fomDato(),
                    medlemskapsperiodeOppdateringDto.tomDato(),
                    medlemskapsperiodeOppdateringDto.innvilgelsesResultat(),
                    medlemskapsperiodeOppdateringDto.trygdedekning(),
                    medlemskapsperiodeOppdateringDto.bestemmelse()
                )
            )
        );
    }

    @DeleteMapping("/behandlinger/{behandlingID}/medlemskapsperioder/{medlemskapsperiodeID}")
    public ResponseEntity<Void> slettMedlemskapsperiode(@PathVariable("behandlingID") long behandlingID,
                                                        @PathVariable("medlemskapsperiodeID") long medlemskapsperiodeID) {
        aksesskontroll.autoriserSkriv(behandlingID);
        medlemskapsperiodeService.slettMedlemskapsperiode(behandlingID, medlemskapsperiodeID);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/behandlinger/{behandlingID}/medlemskapsperioder")
    public ResponseEntity<Void> slettMedlemskapsperiode(@PathVariable("behandlingID") long behandlingID) {
        aksesskontroll.autoriserSkriv(behandlingID);
        medlemskapsperiodeService.slettMedlemskapsperioder(behandlingID);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/behandlinger/{behandlingID}/medlemskapsperioder/forslag")
    public ResponseEntity<Collection<MedlemskapsperiodeDto>> opprettForslagPåMedlemskapsperioder(@PathVariable("behandlingID") long behandlingID,
                                                                                                 @RequestBody BestemmelseDto bestemmelseDto) {
        aksesskontroll.autoriserSkriv(behandlingID);

        return ResponseEntity.ok(
            opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(behandlingID, bestemmelseDto.bestemmelse())
                .stream()
                .map(MedlemskapsperiodeDto::av)
                .collect(Collectors.toSet())
        );
    }
}
