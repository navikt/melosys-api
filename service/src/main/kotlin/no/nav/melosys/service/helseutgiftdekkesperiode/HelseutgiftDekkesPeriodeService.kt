package no.nav.melosys.service.helseutgiftdekkesperiode

import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.PeriodeKilde
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
        ).apply {
            val harEksisterendePeriode = behandlingsresultat.helseutgiftDekkesPerioder.isNotEmpty()
            kilde = if (harEksisterendePeriode) PeriodeKilde.AVGIFT_SYSTEMET else PeriodeKilde.MELOSYS
        }

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
            behandlingsresultat.clearTrygdeavgiftPåHelseutgiftDekkesPerioder()
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
        val behandlingsresultat = periode.behandlingsresultat

        // Må fjerne trygdeavgiftsperioder på ALLE helseutgift-perioder for denne behandlingen,
        // fordi de kan dele inntektsperioder/skatteforhold via CascadeType.ALL.
        // Ellers får vi FK-brudd (ORA-02292) når orphanRemoval prøver å slette delte rader.
        behandlingsresultat.clearTrygdeavgiftPåHelseutgiftDekkesPerioder()

        // Fjerner fra samlingen — orphanRemoval på Behandlingsresultat.helseutgiftDekkesPerioder
        // sørger for at Hibernate sletter perioden fra databasen
        behandlingsresultat.helseutgiftDekkesPerioder.remove(periode)
        behandlingsresultatService.lagreOgFlush(behandlingsresultat)
    }

    @Transactional
    fun slettAlleHelseutgiftDekkesPerioder(behandlingID: Long) {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        if (behandlingsresultat.helseutgiftDekkesPerioder.isEmpty()) return
        behandlingsresultat.clearHelseutgiftDekkesPerioder()
        behandlingsresultatService.lagreOgFlush(behandlingsresultat)
    }

    @Transactional
    fun slettHelseutgiftDekkesPeriodeMedKilde(behandlingID: Long, kilde: PeriodeKilde) {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        val perioderMedKilde = behandlingsresultat.helseutgiftDekkesPerioder.filter { it.kilde == kilde }
        if (perioderMedKilde.isEmpty()) return

        behandlingsresultat.clearTrygdeavgiftPåHelseutgiftDekkesPerioder()
        perioderMedKilde.forEach { behandlingsresultat.helseutgiftDekkesPerioder.remove(it) }
        behandlingsresultatService.lagreOgFlush(behandlingsresultat)
    }

    private fun hentOgValiderPeriode(periodeId: Long, behandlingID: Long): HelseutgiftDekkesPeriode {
        val ikkeFunnetMelding = "Finner ingen helseutgift-periode med id: $periodeId"

        val periode = helseutgiftDekkesPeriodeRepository.findById(periodeId)
            .orElseThrow { IkkeFunnetException(ikkeFunnetMelding) }

        if (periode.behandlingsresultat.hentId() != behandlingID) {
            throw IkkeFunnetException(ikkeFunnetMelding)
        }

        return periode
    }
}
