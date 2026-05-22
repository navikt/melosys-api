package no.nav.melosys.tjenester.gui;

import java.util.List;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import no.nav.melosys.domain.tekstblokk.Tekstblokk;
import no.nav.melosys.domain.tekstblokk.TekstblokkType;
import no.nav.melosys.exception.IkkeFunnetException;
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
        sjekkLesetilgang();
        return ResponseEntity.ok(tekstblokkService.hentAlle(type).stream().map(this::tilOversiktDto).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Henter én tekstblokk med fullt innhold")
    public ResponseEntity<TekstblokkDto> hent(@PathVariable("id") Long id) {
        sjekkLesetilgang();
        return ResponseEntity.ok(tilDto(tekstblokkService.hent(id)));
    }

    @PostMapping
    @Operation(summary = "Oppretter en ny tekstblokk eller brevmal")
    public ResponseEntity<TekstblokkDto> opprett(@Valid @RequestBody TekstblokkRequestDto request) {
        sjekkAdministrasjon();
        return ResponseEntity.ok(tilDto(tekstblokkService.opprett(tilInput(request))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Oppdaterer en eksisterende tekstblokk")
    public ResponseEntity<TekstblokkDto> oppdater(@PathVariable("id") Long id, @Valid @RequestBody TekstblokkRequestDto request) {
        sjekkAdministrasjon();
        return ResponseEntity.ok(tilDto(tekstblokkService.oppdater(id, tilInput(request))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Sletter en tekstblokk")
    public ResponseEntity<Void> slett(@PathVariable("id") Long id) {
        sjekkAdministrasjon();
        tekstblokkService.slett(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lesetilgang krever kun melosys.tekstblokker – brukes i Send brev-popoveren.
     * Returnerer 404 når feature er av: endepunktet finnes ikke for denne brukeren.
     */
    private void sjekkLesetilgang() {
        if (!unleash.isEnabled(ToggleName.MELOSYS_TEKSTBLOKKER)) {
            throw new IkkeFunnetException("Tekstblokker-funksjonalitet er ikke aktivert");
        }
    }

    /**
     * Administrasjon (opprett/endre/slett) krever i tillegg melosys.administrasjon.
     * Slik kan vi rulle ut popoveren bredt mens kun et utvalg får full admin-tilgang.
     * Returnerer 403 når toggle er av: brukeren kan lese, men ikke administrere.
     */
    private void sjekkAdministrasjon() {
        sjekkLesetilgang();
        if (!unleash.isEnabled(ToggleName.MELOSYS_ADMINISTRASJON)) {
            throw new SikkerhetsbegrensningException("Du har ikke tilgang til å administrere tekstblokker");
        }
    }

    private TekstblokkService.Input tilInput(TekstblokkRequestDto request) {
        return new TekstblokkService.Input(request.tittel(), request.innhold(), request.type(), request.tags());
    }

    private TekstblokkOversiktDto tilOversiktDto(Tekstblokk t) {
        return new TekstblokkOversiktDto(t.getId(), t.getTittel(), t.getType(), sorterteTags(t), t.getEndretDato(), t.getEndretAv());
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
        return t.getTags().stream().sorted().toList();
    }
}
