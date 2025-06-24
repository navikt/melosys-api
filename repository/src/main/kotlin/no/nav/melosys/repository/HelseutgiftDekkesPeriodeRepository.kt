package no.nav.melosys.repository

import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import org.springframework.data.jpa.repository.JpaRepository

interface HelseutgiftDekkesPeriodeRepository : JpaRepository<HelseutgiftDekkesPeriode, Long>{
    fun findByBehandlingsresultatId(behandlingsresultatId: Long): HelseutgiftDekkesPeriode
}
