package no.nav.melosys.repository

import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeKilde
import org.springframework.data.jpa.repository.JpaRepository

interface HelseutgiftDekkesPeriodeRepository : JpaRepository<HelseutgiftDekkesPeriode, Long>{
    fun findByBehandlingsresultatId(behandlingsresultatId: Long): List<HelseutgiftDekkesPeriode>
    fun findByBehandlingsresultatIdAndKilde(behandlingsresultatId: Long, kilde: HelseutgiftDekkesPeriodeKilde): List<HelseutgiftDekkesPeriode>
}
