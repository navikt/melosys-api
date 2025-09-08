package no.nav.melosys.service.helseutgiftdekkesperiode

import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
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
        fomDato: LocalDate,
        tomDato: LocalDate,
        bostedLandkode: Land_iso2
    ) : HelseutgiftDekkesPeriode {
        val eksisterendePeriode = helseutgiftDekkesPeriodeRepository.findByBehandlingsresultatId(behandlingID)
            ?: throw IkkeFunnetException("Finner ingen helseutgift-periode med behandlingID: $behandlingID")

        eksisterendePeriode.fomDato = fomDato
        eksisterendePeriode.tomDato = tomDato
        eksisterendePeriode.bostedLandkode = bostedLandkode

        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)

        if(!behandlingsresultat.behandling.erNyVurdering()) {
            eksisterendePeriode.clearTrygdeavgiftsperioder()
        }

        return helseutgiftDekkesPeriodeRepository.save(eksisterendePeriode)
    }

    @Transactional(readOnly = true)
    fun finnHelseutgiftDekkesPeriode(behandlingID: Long): HelseutgiftDekkesPeriode? {
        return helseutgiftDekkesPeriodeRepository.findByBehandlingsresultatId(behandlingID)
    }

    @Transactional(readOnly = true)
    fun hentHelseutgiftDekkesPeriode(behandlingID: Long): HelseutgiftDekkesPeriode {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)

        return behandlingsresultat.helseutgiftDekkesPeriode
    }

    @Transactional
    fun slettHelseutgiftDekkesPeriode(behandlingsresultatID: Long) {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)

        behandlingsresultat.helseutgiftDekkesPeriode?: return

        behandlingsresultat.helseutgiftDekkesPeriode.clearTrygdeavgiftsperioder()
        behandlingsresultatService.lagreOgFlush(behandlingsresultat)

        behandlingsresultat.helseutgiftDekkesPeriode = null
    }

}
