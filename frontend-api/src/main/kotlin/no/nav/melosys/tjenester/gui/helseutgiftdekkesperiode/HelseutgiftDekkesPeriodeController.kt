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

        val bostedLandkode = validerOgParseLandkode(helseutgiftDekkesPeriodeDto.bostedLandkode)

        val helseutgiftDekkesPeriode = helseutgiftDekkesPeriodeService.opprettHelseutgiftDekkesPeriode(
            behandlingID,
            helseutgiftDekkesPeriodeDto.fomDato,
            helseutgiftDekkesPeriodeDto.tomDato,
            bostedLandkode
        )
        return ResponseEntity.ok(HelseutgiftDekkesPeriodeDto.av(helseutgiftDekkesPeriode))
    }

    @PutMapping("/{periodeId}")
    fun oppdaterHelseutgiftDekkesPeriode(
        @PathVariable("behandlingID") behandlingID: Long,
        @PathVariable("periodeId") periodeId: Long,
        @RequestBody helseutgiftDekkesPeriodeDto: HelseutgiftDekkesPeriodeDto
    ): ResponseEntity<HelseutgiftDekkesPeriodeDto> {
        aksesskontroll.autoriserSkriv(behandlingID)

        val bostedLandkode = validerOgParseLandkode(helseutgiftDekkesPeriodeDto.bostedLandkode)

        val helseutgiftDekkesPeriode = helseutgiftDekkesPeriodeService.oppdaterHelseutgiftDekkesPeriode(
            behandlingID,
            periodeId,
            helseutgiftDekkesPeriodeDto.fomDato,
            helseutgiftDekkesPeriodeDto.tomDato,
            bostedLandkode
        )

        return ResponseEntity.ok(HelseutgiftDekkesPeriodeDto.av(helseutgiftDekkesPeriode))
    }

    @GetMapping
    fun finnHelseutgiftDekkesPerioder(
        @PathVariable("behandlingID") behandlingID: Long
    ): ResponseEntity<List<HelseutgiftDekkesPeriodeDto>> {
        aksesskontroll.autoriser(behandlingID)

        val perioder = helseutgiftDekkesPeriodeService.finnHelseutgiftDekkesPerioder(behandlingID)

        return ResponseEntity.ok(perioder.map { HelseutgiftDekkesPeriodeDto.av(it) })
    }

    @DeleteMapping("/{periodeId}")
    fun slettHelseutgiftDekkesPeriode(
        @PathVariable("behandlingID") behandlingID: Long,
        @PathVariable("periodeId") periodeId: Long
    ): ResponseEntity<Void> {
        aksesskontroll.autoriserSkriv(behandlingID)

        helseutgiftDekkesPeriodeService.slettHelseutgiftDekkesPeriode(behandlingID, periodeId)

        return ResponseEntity.noContent().build()
    }

    private fun validerOgParseLandkode(bostedLandkode: String): Land_iso2 {
        if (bostedLandkode.isBlank()) {
            throw FunksjonellException("Bosted landkode er påkrevd")
        }
        return Land_iso2.values().firstOrNull { it.kode == bostedLandkode }
            ?: throw FunksjonellException("Landkode er ikke gyldig")
    }
}


data class HelseutgiftDekkesPeriodeDto(
    val id: Long? = null,
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    val bostedLandkode: String,
) {
    companion object {
        fun av(helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode): HelseutgiftDekkesPeriodeDto {
            return HelseutgiftDekkesPeriodeDto(
                id = helseutgiftDekkesPeriode.id,
                fomDato = helseutgiftDekkesPeriode.fomDato,
                tomDato = helseutgiftDekkesPeriode.tomDato,
                bostedLandkode = helseutgiftDekkesPeriode.bostedLandkode.kode
            )
        }
    }
}
