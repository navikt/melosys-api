package no.nav.melosys.repository

import no.nav.melosys.domain.avgift.Aarsavregning
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface AarsavregningRepository : JpaRepository<Aarsavregning, Long> {

    fun findByIdAndAar(id: Long, aar: Int): Optional<Aarsavregning>

}
