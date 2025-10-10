package no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.jpa.konverterTilBestemmelse
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService
import no.nav.melosys.service.ftrl.medlemskapsperiode.OpprettForslagMedlemskapsperiodeService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode.dto.BestemmelseDto
import no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode.dto.FastsettingsperiodeDto
import no.nav.melosys.tjenester.gui.ftrl.medlemskapsperiode.dto.MedlemskapsperiodeOppdateringDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.context.annotation.Scope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.WebApplicationContext

@Protected
@RestController
@Tag(name = "medlemskapsperioder")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
class MedlemskapsperiodeController(
    private val medlemskapsperiodeService: MedlemskapsperiodeService,
    private val opprettForslagMedlemskapsperiodeService: OpprettForslagMedlemskapsperiodeService,
    private val aksesskontroll: Aksesskontroll
) {
    @GetMapping("/behandlinger/{behandlingID}/medlemskapsperioder")
    fun hentMedlemskapsperioder(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<Collection<FastsettingsperiodeDto>> {
        aksesskontroll.autoriser(behandlingID)
        return ResponseEntity.ok(
            medlemskapsperiodeService.hentMedlemskapsperioder(behandlingID)
                .map { it.toDto() }
        )
    }

    @PostMapping("/behandlinger/{behandlingID}/medlemskapsperioder")
    fun opprettMedlemskapsperiode(
        @PathVariable("behandlingID") behandlingID: Long,
        @RequestBody medlemskapsperiodeOppdateringDto: MedlemskapsperiodeOppdateringDto
    ): ResponseEntity<FastsettingsperiodeDto> {
        aksesskontroll.autoriserSkriv(behandlingID)
        return ResponseEntity.ok(
            medlemskapsperiodeService.opprettMedlemskapsperiode(
                behandlingID,
                medlemskapsperiodeOppdateringDto.fomDato,
                medlemskapsperiodeOppdateringDto.tomDato,
                medlemskapsperiodeOppdateringDto.innvilgelsesResultat,
                medlemskapsperiodeOppdateringDto.trygdedekning,
                konverterTilBestemmelse(medlemskapsperiodeOppdateringDto.bestemmelse)
            ).toDto()
        )
    }

    @PutMapping("/behandlinger/{behandlingID}/medlemskapsperioder/{medlemskapsperiodeID}")
    fun oppdaterMedlemskapsperiode(
        @PathVariable("behandlingID") behandlingID: Long,
        @PathVariable("medlemskapsperiodeID") medlemskapsperiodeID: Long,
        @RequestBody medlemskapsperiodeOppdateringDto: MedlemskapsperiodeOppdateringDto
    ): ResponseEntity<FastsettingsperiodeDto> {
        aksesskontroll.autoriserSkriv(behandlingID)
        return ResponseEntity.ok(
            medlemskapsperiodeService.oppdaterMedlemskapsperiode(
                behandlingID,
                medlemskapsperiodeID,
                medlemskapsperiodeOppdateringDto.fomDato,
                medlemskapsperiodeOppdateringDto.tomDato,
                medlemskapsperiodeOppdateringDto.innvilgelsesResultat,
                medlemskapsperiodeOppdateringDto.trygdedekning,
                konverterTilBestemmelse(medlemskapsperiodeOppdateringDto.bestemmelse)
            ).toDto()
        )
    }

    @DeleteMapping("/behandlinger/{behandlingID}/medlemskapsperioder/{medlemskapsperiodeID}")
    fun slettMedlemskapsperiode(
        @PathVariable("behandlingID") behandlingID: Long,
        @PathVariable("medlemskapsperiodeID") medlemskapsperiodeID: Long
    ): ResponseEntity<Unit> {
        aksesskontroll.autoriserSkriv(behandlingID)
        medlemskapsperiodeService.slettMedlemskapsperiode(behandlingID, medlemskapsperiodeID)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/behandlinger/{behandlingID}/medlemskapsperioder")
    fun slettMedlemskapsperiode(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<Unit> {
        aksesskontroll.autoriserSkriv(behandlingID)
        medlemskapsperiodeService.slettMedlemskapsperioder(behandlingID)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/behandlinger/{behandlingID}/medlemskapsperioder/forslag")
    fun opprettForslagPåMedlemskapsperioder(
        @PathVariable("behandlingID") behandlingID: Long,
        @RequestBody bestemmelseDto: BestemmelseDto
    ): ResponseEntity<Collection<FastsettingsperiodeDto>> {
        aksesskontroll.autoriserSkriv(behandlingID)
        return ResponseEntity.ok(
            opprettForslagMedlemskapsperiodeService.opprettForslagPåMedlemskapsperioder(
                behandlingID,
                konverterTilBestemmelse(bestemmelseDto.bestemmelse)
            ).map { it.toDto() }
        )
    }

    private fun Medlemskapsperiode.toDto() =
        FastsettingsperiodeDto(id, fom, tom, bestemmelse, innvilgelsesresultat, trygdedekning, medlemskapstype)

}
