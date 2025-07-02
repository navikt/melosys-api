package no.nav.melosys.tjenester.gui.helseutgiftdekkesperiode

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@Protected
@RestController
@RequestMapping("/behandlinger/{behandlingID}/helseutgift-dekkes-perioder")
@Tags(
    Tag(name = "helseutgift_dekkes_periode"),
)
class HelseutgiftDekkesPeriodeController(
    private val helseutgiftDekkesPeriodeService: HelseutgiftDekkesPeriodeService,
    private val aksesskontroll: Aksesskontroll,
) {

    @PostMapping
    fun opprettNyHelseutgiftDekkesPeriode(
        @PathVariable("behandlingID") behandlingID: Long,
        @RequestBody helseutgiftDekkesPeriodeDto: HelseutgiftDekkesPeriodeDto
    ): ResponseEntity<HelseutgiftDekkesPeriodeDto> {
        aksesskontroll.autoriserSkriv(behandlingID)

        if (!erGyldigLand(helseutgiftDekkesPeriodeDto.bostedLandkode)) {
            throw FunksjonellException("Landkode er ikke gyldig")
        }

        val helseutgiftDekkesPeriode = helseutgiftDekkesPeriodeService.opprettHelseutgiftDekkesPeriode(
            behandlingID,
            helseutgiftDekkesPeriodeDto.fomDato,
            helseutgiftDekkesPeriodeDto.tomDato,
            Land_iso2.valueOf(helseutgiftDekkesPeriodeDto.bostedLandkode)
        )
        return ResponseEntity.ok(lagHelseutgiftDekkesPeriodeResponse(helseutgiftDekkesPeriode))
    }

    @PutMapping
    fun oppdaterHelseutgiftDekkesPeriode(
        @PathVariable("behandlingID") behandlingID: Long,
        @RequestBody helseutgiftDekkesPeriodeDto: HelseutgiftDekkesPeriodeDto
    ): ResponseEntity<HelseutgiftDekkesPeriodeDto> {
        aksesskontroll.autoriserSkriv(behandlingID)

        if (!erGyldigLand(helseutgiftDekkesPeriodeDto.bostedLandkode)) {
            throw FunksjonellException("Landkode er ikke gyldig")
        }

        val helseutgiftDekkesPeriode = helseutgiftDekkesPeriodeService.oppdaterHelseutgiftDekkesPeriode(
            behandlingID,
            helseutgiftDekkesPeriodeDto.fomDato,
            helseutgiftDekkesPeriodeDto.tomDato,
            Land_iso2.valueOf(helseutgiftDekkesPeriodeDto.bostedLandkode)
        )

        return ResponseEntity.ok(lagHelseutgiftDekkesPeriodeResponse(helseutgiftDekkesPeriode))
    }

    @GetMapping
    fun hentHelseutgiftDekkesPeriode(
        @PathVariable("behandlingID") behandlingID: Long
    ): ResponseEntity<HelseutgiftDekkesPeriodeDto> {
        aksesskontroll.autoriser(behandlingID)

        val helseutgiftDekkesPeriode = helseutgiftDekkesPeriodeService.hentHelseutgiftDekkesPeriode(behandlingID)

        return ResponseEntity.ok(lagHelseutgiftDekkesPeriodeResponse(helseutgiftDekkesPeriode))
    }

    private fun lagHelseutgiftDekkesPeriodeResponse(helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode) : HelseutgiftDekkesPeriodeDto {
        return HelseutgiftDekkesPeriodeDto.av(helseutgiftDekkesPeriode)
    }


    private fun erGyldigLand(land: String): Boolean {
        return Land_iso2.values().any { it.kode == land }
    }
}


data class HelseutgiftDekkesPeriodeDto(
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    val bostedLandkode: String,
) {
    companion object {
        fun av(helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode): HelseutgiftDekkesPeriodeDto {
            return HelseutgiftDekkesPeriodeDto(
                fomDato = helseutgiftDekkesPeriode.fomDato,
                tomDato = helseutgiftDekkesPeriode.tomDato,
                bostedLandkode = helseutgiftDekkesPeriode.bostedLandkode.kode
            )
        }
    }
}
