package no.nav.melosys.repository.tekstblokk

import java.util.Optional

import no.nav.melosys.domain.tekstblokk.Tekstblokk
import no.nav.melosys.domain.tekstblokk.TekstblokkOversikt
import no.nav.melosys.domain.tekstblokk.TekstblokkType
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TekstblokkRepository : JpaRepository<Tekstblokk, Long> {

    @Query(
        """
        SELECT new no.nav.melosys.domain.tekstblokk.TekstblokkOversikt(t.id, t.tittel, t.type, t.endretDato, t.endretAv)
        FROM Tekstblokk t
        WHERE (:type IS NULL OR t.type = :type)
        ORDER BY t.tittel ASC
        """,
    )
    fun finnOversikt(@Param("type") type: TekstblokkType?): List<TekstblokkOversikt>

    @Query("SELECT t.id, tag FROM Tekstblokk t JOIN t.tags tag WHERE t.id IN :ids")
    fun finnTagsForIds(@Param("ids") ids: Collection<Long>): List<Array<Any>>

    @EntityGraph(attributePaths = ["tags"])
    override fun findById(id: Long): Optional<Tekstblokk>
}
