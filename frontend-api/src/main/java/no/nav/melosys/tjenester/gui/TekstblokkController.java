package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.melosys.domain.tekstblokk.Tekstblokk;
import no.nav.melosys.domain.tekstblokk.TekstblokkType;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.featuretoggle.ToggleName;
import no.nav.melosys.service.tekstblokk.TekstblokkService;
import no.nav.melosys.tjenester.gui.dto.tekstblokk.TekstblokkDto;
import no.nav.melosys.tjenester.gui.dto.tekstblokk.TekstblokkOversiktDto;
import no.nav.melosys.tjenester.gui.dto.tekstblokk.TekstblokkRequestDto;
import no.nav.security.token.support.core.api.Protected;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Protected
@RestController
@RequestMapping("/tekstblokker")
@Tag(name = "tekstblokker")
public class TekstblokkController {

    private final TekstblokkService tekstblokkService;
    private final Unleash unleash;

    public TekstblokkController(TekstblokkService tekstblokkService, Unleash unleash) {
        this.tekstblokkService = tekstblokkService;
        this.unleash = unleash;
    }

    @GetMapping
    @Operation(summary = "Henter oversikt over tekstblokker og brevmaler (uten innhold)")
    public ResponseEntity<List<TekstblokkOversiktDto>> hentAlle(@RequestParam(value = "type", required = false) TekstblokkType type) {
        sjekkToggle();
        List<TekstblokkOversiktDto> oversikt = tekstblokkService.hentAlle(type).stream()
            .map(this::tilOversiktDto)
            .toList();
        return ResponseEntity.ok(oversikt);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Henter én tekstblokk med fullt innhold")
    public ResponseEntity<TekstblokkDto> hent(@PathVariable("id") Long id) {
        sjekkToggle();
        return ResponseEntity.ok(tilDto(tekstblokkService.hent(id)));
    }

    @PostMapping
    @Operation(summary = "Oppretter en ny tekstblokk eller brevmal")
    public ResponseEntity<TekstblokkDto> opprett(@RequestBody TekstblokkRequestDto request) {
        sjekkToggle();
        Tekstblokk opprettet = tekstblokkService.opprett(
            request.tittel(),
            request.innhold(),
            request.type(),
            request.tags() == null ? Collections.emptySet() : new java.util.HashSet<>(request.tags())
        );
        return ResponseEntity.ok(tilDto(opprettet));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Oppdaterer en eksisterende tekstblokk")
    public ResponseEntity<TekstblokkDto> oppdater(@PathVariable("id") Long id, @RequestBody TekstblokkRequestDto request) {
        sjekkToggle();
        Tekstblokk oppdatert = tekstblokkService.oppdater(
            id,
            request.tittel(),
            request.innhold(),
            request.type(),
            request.tags() == null ? Collections.emptySet() : new java.util.HashSet<>(request.tags())
        );
        return ResponseEntity.ok(tilDto(oppdatert));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Sletter en tekstblokk")
    public ResponseEntity<Void> slett(@PathVariable("id") Long id) {
        sjekkToggle();
        tekstblokkService.slett(id);
        return ResponseEntity.noContent().build();
    }

    private void sjekkToggle() {
        if (!unleash.isEnabled(ToggleName.MELOSYS_TEKSTBLOKKER)) {
            throw new SikkerhetsbegrensningException("Tekstblokker-funksjonalitet er ikke aktivert");
        }
    }

    private TekstblokkOversiktDto tilOversiktDto(Tekstblokk t) {
        return new TekstblokkOversiktDto(
            t.getId(),
            t.getTittel(),
            t.getType(),
            sorterteTags(t),
            t.getEndretDato(),
            t.getEndretAv()
        );
    }

    private TekstblokkDto tilDto(Tekstblokk t) {
        return new TekstblokkDto(
            t.getId(),
            t.getTittel(),
            t.getInnhold(),
            t.getType(),
            sorterteTags(t),
            t.getRegistrertDato(),
            t.getRegistrertAv(),
            t.getEndretDato(),
            t.getEndretAv()
        );
    }

    private List<String> sorterteTags(Tekstblokk t) {
        List<String> sortert = new ArrayList<>(t.getTags());
        Collections.sort(sortert);
        return sortert;
    }
}
