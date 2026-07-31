package no.nav.melosys.repository.tekstblokk

import java.util.Optional

import no.nav.melosys.domain.brev.tekstblokk.Tekstblokk
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkOversikt
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkType
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TekstblokkRepository : JpaRepository<Tekstblokk, Long> {

    @Query(
        """
        SELECT new no.nav.melosys.domain.brev.tekstblokk.TekstblokkOversikt(t.id, t.tittel, t.innhold, t.type, t.endretDato, t.endretAv, t.endretAvNavn)
        FROM Tekstblokk t
        WHERE (:type IS NULL OR t.type = :type)
          AND t.slettetDato IS NULL
        ORDER BY t.tittel ASC
        """,
    )
    fun finnOversikt(@Param("type") type: TekstblokkType?): List<TekstblokkOversikt>

    @Query("SELECT t.id, tag FROM Tekstblokk t JOIN t.tags tag WHERE t.id IN :ids")
    fun finnTagsForIds(@Param("ids") ids: Collection<Long>): List<Array<Any>>

    @Query("SELECT t.id, sakstype FROM Tekstblokk t JOIN t.sakstyper sakstype WHERE t.id IN :ids")
    fun finnSakstyperForIds(@Param("ids") ids: Collection<Long>): List<Array<Any>>

    @Query("SELECT t.id, behandlingstema FROM Tekstblokk t JOIN t.behandlingstemaer behandlingstema WHERE t.id IN :ids")
    fun finnBehandlingstemaerForIds(@Param("ids") ids: Collection<Long>): List<Array<Any>>

    // Kun tags i grafen: flere @ElementCollection i samme join gir kartesisk produkt.
    // Sakstyper og behandlingstemaer lastes lazy innenfor transaksjonen i servicen.
    @EntityGraph(attributePaths = ["tags"])
    fun findByIdAndSlettetDatoIsNull(id: Long): Optional<Tekstblokk>
}
