package no.nav.melosys.service.helseutgiftdekkesperiode

import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeKilde
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.repository.HelseutgiftDekkesPeriodeRepository
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class HelseutgiftDekkesPeriodeService(
    private val helseutgiftDekkesPeriodeRepository: HelseutgiftDekkesPeriodeRepository,
    private val behandlingsresultatService: BehandlingsresultatService
) {

    @Transactional
    fun opprettHelseutgiftDekkesPeriode(
        behandlingID: Long,
        fomDato: LocalDate,
        tomDato: LocalDate,
        bostedLandkode: Land_iso2
    ): HelseutgiftDekkesPeriode {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)

        val nyHelseutgiftDekkesPeriode = HelseutgiftDekkesPeriode(
            behandlingsresultat = behandlingsresultat,
            fomDato = fomDato,
            tomDato = tomDato,
            bostedLandkode = bostedLandkode
        )

        return helseutgiftDekkesPeriodeRepository.save(nyHelseutgiftDekkesPeriode)
    }

    @Transactional
    fun oppdaterHelseutgiftDekkesPeriode(
        behandlingID: Long,
        periodeId: Long,
        fomDato: LocalDate,
        tomDato: LocalDate,
        bostedLandkode: Land_iso2
    ): HelseutgiftDekkesPeriode {
        val eksisterendePeriode = hentOgValiderPeriode(periodeId, behandlingID)

        eksisterendePeriode.fomDato = fomDato
        eksisterendePeriode.tomDato = tomDato
        eksisterendePeriode.bostedLandkode = bostedLandkode

        val behandlingsresultat = eksisterendePeriode.behandlingsresultat

        if (!behandlingsresultat.hentBehandling().erNyVurdering()) {
            eksisterendePeriode.clearTrygdeavgiftsperioder()
        }

        return helseutgiftDekkesPeriodeRepository.save(eksisterendePeriode)
    }

    @Transactional(readOnly = true)
    fun finnHelseutgiftDekkesPerioder(behandlingID: Long): List<HelseutgiftDekkesPeriode> {
        return helseutgiftDekkesPeriodeRepository.findByBehandlingsresultatId(behandlingID)
    }

    @Transactional(readOnly = true)
    fun hentHelseutgiftDekkesPerioder(behandlingID: Long): List<HelseutgiftDekkesPeriode> {
        val perioder = helseutgiftDekkesPeriodeRepository.findByBehandlingsresultatId(behandlingID)
        if (perioder.isEmpty()) {
            throw IkkeFunnetException("Fant ikke helseutgift dekkes perioder for behandling $behandlingID.")
        }
        return perioder
    }

    @Transactional
    fun slettHelseutgiftDekkesPeriode(behandlingID: Long, periodeId: Long) {
        val periode = hentOgValiderPeriode(periodeId, behandlingID)
        periode.clearTrygdeavgiftsperioder()
        helseutgiftDekkesPeriodeRepository.delete(periode)
    }

    @Transactional
    fun slettAlleHelseutgiftDekkesPerioder(behandlingID: Long) {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        if (behandlingsresultat.helseutgiftDekkesPerioder.isEmpty()) return
        behandlingsresultat.clearHelseutgiftDekkesPerioder()
        behandlingsresultatService.lagreOgFlush(behandlingsresultat)
    }

    private fun hentOgValiderPeriode(periodeId: Long, behandlingID: Long): HelseutgiftDekkesPeriode {
        val ikkeFunnetMelding = "Finner ingen helseutgift-periode med id: $periodeId"

        val periode = helseutgiftDekkesPeriodeRepository.findById(periodeId)
            .orElseThrow { IkkeFunnetException(ikkeFunnetMelding) }

        if (periode.behandlingsresultat.hentId() != behandlingID) {
            throw IkkeFunnetException(ikkeFunnetMelding)
        }

        if (periode.kilde != HelseutgiftDekkesPeriodeKilde.MELOSYS) {
            throw IkkeFunnetException(ikkeFunnetMelding)
        }

        return periode
    }
}
