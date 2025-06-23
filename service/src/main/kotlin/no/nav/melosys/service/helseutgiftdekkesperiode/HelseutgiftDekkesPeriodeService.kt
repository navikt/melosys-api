package no.nav.melosys.service.helseutgiftdekkesperiode

import no.nav.melosys.domain.Bostedsland
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.HelseutgiftDekkesPeriodeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class HelseutgiftDekkesPeriodeService(
    private val helseutgiftDekkesPeriodeRepository: HelseutgiftDekkesPeriodeRepository,
    private val behandlingsresultatRepository: BehandlingsresultatRepository
) {

    @Transactional
    fun opprettHelseutgiftDekkesPeriode(behandlingsresultatID: Long, fomDato: LocalDate, tomDato: LocalDate?, bostedsland: String) {
        val behandlingsresultat = behandlingsresultatRepository.findById(behandlingsresultatID).orElseThrow { IkkeFunnetException("Finner ingen behandlingsresultat for id: $behandlingsresultatID")  }
        val nyHelseutgiftDekkesPeriodePeriode = HelseutgiftDekkesPeriode().apply {
            this.behandlingsresultat = behandlingsresultat
            this.fomDato = fomDato
            this.tomDato = tomDato
            this.bostedsland = bostedsland
        }

        helseutgiftDekkesPeriodeRepository.save(nyHelseutgiftDekkesPeriodePeriode)
    }

    @Transactional(readOnly = true)
    fun hentHelseutgiftDekkesPeriode(behandlingsresultatID: Long): HelseutgiftDekkesPeriode {
        return helseutgiftDekkesPeriodeRepository.findByBehandlingsresultatId(behandlingsresultatID)
    }
}
