package no.nav.melosys.tjenester.gui;

import java.util.Collection;
import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.medlemskapsperiode.MedlemskapsperiodeService;
import no.nav.melosys.tjenester.gui.dto.MedlemskapsperiodeDto;
import no.nav.melosys.tjenester.gui.dto.MedlemskapsperiodeOppdatering;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/behandlinger/{behandlingID}/medlemskapsperioder")
@Api(tags = {"medlemskapsperioder"})
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class MedlemskapsperiodeTjeneste {

    private final MedlemskapsperiodeService medlemskapsperiodeService;
    private final TilgangService tilgangService;

    public MedlemskapsperiodeTjeneste(MedlemskapsperiodeService medlemskapsperiodeService, TilgangService tilgangService) {
        this.medlemskapsperiodeService = medlemskapsperiodeService;
        this.tilgangService = tilgangService;
    }

    @GetMapping
    public ResponseEntity<Collection<MedlemskapsperiodeDto>> hentMedlemskapsperioder(@PathVariable("behandlingID") long behandlingID) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        tilgangService.sjekkTilgang(behandlingID);
        return ResponseEntity.ok(
            medlemskapsperiodeService.hentMedlemskapsperioder(behandlingID)
                .stream()
                .map(MedlemskapsperiodeDto::av)
                .collect(Collectors.toSet())
        );
    }

    @PostMapping
    public ResponseEntity<MedlemskapsperiodeDto> opprettMedlemskapsperiode(@PathVariable("behandlingID") long behandlingID,
                                                                           @RequestBody MedlemskapsperiodeOppdatering medlemskapsperiodeOppdatering) throws FunksjonellException, TekniskException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        return ResponseEntity.ok(
            MedlemskapsperiodeDto.av(
                medlemskapsperiodeService.opprettMedlemskapsperiode(
                    behandlingID,
                    medlemskapsperiodeOppdatering.getFomDato(),
                    medlemskapsperiodeOppdatering.getTomDato(),
                    medlemskapsperiodeOppdatering.getInnvilgelsesResultat(),
                    medlemskapsperiodeOppdatering.getTrygdedekning())
            )
        );
    }

    @PutMapping("/{medlemskapsperiodeID}")
    public ResponseEntity<MedlemskapsperiodeDto> oppdaterMedlemskapsperiode(@PathVariable("behandlingID") long behandlingID,
                                                                            @PathVariable("medlemskapsperiodeID") long medlemskapsperiodeID,
                                                                            @RequestBody MedlemskapsperiodeOppdatering medlemskapsperiodeOppdatering) throws FunksjonellException, TekniskException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        return ResponseEntity.ok(
            MedlemskapsperiodeDto.av(
                medlemskapsperiodeService.oppdaterMedlemskapsperiode(
                    behandlingID,
                    medlemskapsperiodeID,
                    medlemskapsperiodeOppdatering.getFomDato(),
                    medlemskapsperiodeOppdatering.getTomDato(),
                    medlemskapsperiodeOppdatering.getInnvilgelsesResultat(),
                    medlemskapsperiodeOppdatering.getTrygdedekning()
                )
            )
        );
    }

    @DeleteMapping("/{medlemskapsperiodeID}")
    public ResponseEntity<Void> slettMedlemskapsperiode(@PathVariable("behandlingID") long behandlingID,
                                                        @PathVariable("medlemskapsperiodeID") long medlemskapsperiodeID) throws FunksjonellException, TekniskException {
        tilgangService.sjekkRedigerbarOgTilgang(behandlingID);
        medlemskapsperiodeService.slettMedlemskapsperiode(behandlingID, medlemskapsperiodeID);
        return ResponseEntity.ok().build();
    }
}
