package no.nav.melosys.tjenester.gui

import io.getunleash.Unleash
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkType
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.exception.SikkerhetsbegrensningException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.service.tekstblokk.TekstblokkService
import no.nav.melosys.tjenester.gui.dto.tekstblokk.TekstblokkDto
import no.nav.melosys.tjenester.gui.dto.tekstblokk.TekstblokkOversiktDto
import no.nav.melosys.tjenester.gui.dto.tekstblokk.TekstblokkRequestDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
@RequestMapping("/tekstblokker")
@Tag(name = "tekstblokker")
class TekstblokkController(
    private val tekstblokkService: TekstblokkService,
    private val unleash: Unleash,
) {

    @GetMapping
    @Operation(summary = "Henter oversikt over tekstblokker og brevmaler (uten innhold)")
    fun hentAlle(@RequestParam(value = "type", required = false) type: TekstblokkType?): ResponseEntity<List<TekstblokkOversiktDto>> {
        sjekkLesetilgang()
        return ResponseEntity.ok(tekstblokkService.hentAlleOversikter(type).map(TekstblokkOversiktDto::av))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Henter én tekstblokk med fullt innhold")
    fun hent(@PathVariable("id") id: Long): ResponseEntity<TekstblokkDto> {
        sjekkLesetilgang()
        return ResponseEntity.ok(TekstblokkDto.av(tekstblokkService.hent(id)))
    }

    @PostMapping
    @Operation(summary = "Oppretter en ny tekstblokk eller brevmal")
    fun opprett(@Valid @RequestBody request: TekstblokkRequestDto): ResponseEntity<TekstblokkDto> {
        sjekkAdministrasjon()
        return ResponseEntity.ok(TekstblokkDto.av(tekstblokkService.opprett(request.tilInput())))
    }

    @PutMapping("/{id}")
    @Operation(summary = "Oppdaterer en eksisterende tekstblokk")
    fun oppdater(@PathVariable("id") id: Long, @Valid @RequestBody request: TekstblokkRequestDto): ResponseEntity<TekstblokkDto> {
        sjekkAdministrasjon()
        return ResponseEntity.ok(TekstblokkDto.av(tekstblokkService.oppdater(id, request.tilInput())))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Sletter en tekstblokk")
    fun slett(@PathVariable("id") id: Long): ResponseEntity<Void> {
        sjekkAdministrasjon()
        tekstblokkService.slett(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Lesetilgang krever kun melosys.tekstblokker – brukes i Send brev-popoveren.
     * Returnerer 404 når feature er av: endepunktet finnes ikke for denne brukeren.
     */
    private fun sjekkLesetilgang() {
        if (!unleash.isEnabled(ToggleName.MELOSYS_TEKSTBLOKKER)) {
            throw IkkeFunnetException("Tekstblokker-funksjonalitet er ikke aktivert")
        }
    }

    /**
     * Administrasjon (opprett/endre/slett) krever i tillegg melosys.administrasjon.
     * Slik kan vi rulle ut popoveren bredt mens kun et utvalg får full admin-tilgang.
     * Returnerer 403 når toggle er av: brukeren kan lese, men ikke administrere.
     */
    private fun sjekkAdministrasjon() {
        sjekkLesetilgang()
        if (!unleash.isEnabled(ToggleName.MELOSYS_ADMINISTRASJON)) {
            throw SikkerhetsbegrensningException("Du har ikke tilgang til å administrere tekstblokker")
        }
    }

}
