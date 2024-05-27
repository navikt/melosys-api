package no.nav.melosys.repository

import no.nav.melosys.domain.avgift.Aarsavregning
import org.springframework.data.jpa.repository.JpaRepository

interface AarsavregningRepository : JpaRepository<Aarsavregning, Long>
