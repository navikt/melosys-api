package no.nav.melosys.tjenester.gui;

import java.util.Collection;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.melosys.domain.Utpekingsperiode;
import no.nav.melosys.service.tilgang.Aksesskontroll;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.tjenester.gui.dto.utpeking.UtpekingsperioderDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@Tag(name = "utpekingsperioder")
@RequestMapping("/utpekingsperioder")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class UtpekingsperiodeController {

    private final UtpekingService utpekingService;
    private final Aksesskontroll aksesskontroll;

    public UtpekingsperiodeController(UtpekingService utpekingService, Aksesskontroll aksesskontroll) {
        this.utpekingService = utpekingService;
        this.aksesskontroll = aksesskontroll;
    }

    @GetMapping("{behandlingID}")
    @Operation(summary = "Henter utpekingsperioder for en gitt behandling")
    public UtpekingsperioderDto hentUtpekingsperioder(@PathVariable("behandlingID") long behandlingID) {

        aksesskontroll.autoriser(behandlingID);

        Collection<Utpekingsperiode> utpekingsperioder = utpekingService.hentUtpekingsperioder(behandlingID);

        return UtpekingsperioderDto.av(utpekingsperioder);
    }

    @PostMapping("{behandlingID}")
    @Operation(summary = "Lagrer utpekingssperioder for en gitt behandling.")
    public UtpekingsperioderDto lagreUtpekingsperioder(@PathVariable("behandlingID") long behandlingID,
                                                       @RequestBody UtpekingsperioderDto utpekingsperioderDto) {

        aksesskontroll.autoriserSkriv(behandlingID);

        Collection<Utpekingsperiode> utpekingsperioder = UtpekingsperioderDto.tilDomene(utpekingsperioderDto);
        utpekingsperioder = utpekingService.lagreUtpekingsperioder(behandlingID, utpekingsperioder);

        return UtpekingsperioderDto.av(utpekingsperioder);
    }
}
