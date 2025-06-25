package no.nav.melosys.service.helseutgiftdekkesperiode

import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.exception.FunksjonellException
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
    fun opprettHelseutgiftDekkesPeriode(behandlingID: Long, fomDato: LocalDate, tomDato: LocalDate, bostedLandkode: String) {
        val behandlingsresultat = behandlingsresultatRepository.findById(behandlingID)
            .orElseThrow { IkkeFunnetException("Finner ingen behandlingsresultat for id: $behandlingID") }

        val nyHelseutgiftDekkesPeriode = HelseutgiftDekkesPeriode(
            behandlingsresultat = behandlingsresultat,
            fomDato = fomDato,
            tomDato = tomDato,
            bostedLandkode = Land_iso2.valueOf(bostedLandkode)
        )

        helseutgiftDekkesPeriodeRepository.save(nyHelseutgiftDekkesPeriode)
    }

    @Transactional
    fun oppdaterHelseutgiftDekkesPeriode(
        behandlingID: Long,
        fomDato: LocalDate,
        tomDato: LocalDate,
        bostedLandkode: String
    ) {
        val eksisterendePeriode = helseutgiftDekkesPeriodeRepository.findByBehandlingsresultatId(behandlingID)
            ?: throw IkkeFunnetException("Finner ingen helseutgift-periode med behandlingID: $behandlingID")

        eksisterendePeriode.fomDato = fomDato
        eksisterendePeriode.tomDato = tomDato
        eksisterendePeriode.bostedLandkode = Land_iso2.valueOf(bostedLandkode)

        helseutgiftDekkesPeriodeRepository.save(eksisterendePeriode)
    }

    @Transactional(readOnly = true)
    fun hentHelseutgiftDekkesPeriode(behandlingID: Long): HelseutgiftDekkesPeriode? {
        return helseutgiftDekkesPeriodeRepository.findByBehandlingsresultatId(behandlingID) ?: throw IkkeFunnetException("Finner ingen helseutgift-periode med behandlingID: $behandlingID")
    }

}
