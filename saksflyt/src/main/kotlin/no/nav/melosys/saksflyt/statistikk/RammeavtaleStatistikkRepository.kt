package no.nav.melosys.saksflyt.statistikk

import no.nav.melosys.saksflytapi.domain.Prosessinstans
import org.springframework.data.jpa.repository.NativeQuery
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface RammeavtaleStatistikkRepository : Repository<Prosessinstans, UUID> {

    /**
     * Teller prosessinstanser per år (basert på registrert_dato) der prosessdataen inneholder en gitt nøkkel=verdi.
     * Prosessdataen lagres som java.util.Properties-tekst (key=value per linje) i CLOB-kolonnen `data`, derfor
     * matches det med LIKE mot et `"<kode>=<verdi>"`-mønster. Brukes til å hente ut antall anmodning-om-unntak-
     * behandlinger der rammeavtale om fjernarbeid (TWFA) er huket av.
     *
     * Hver rad er `[aar (String), antall (Number)]`. `fom`/`tom` er valgfrie (null = ingen grense).
     */
    @NativeQuery(
        """
        SELECT TO_CHAR(p.registrert_dato, 'YYYY') AS aar, COUNT(*) AS antall
        FROM prosessinstans p
        WHERE p.prosess_type = :prosessType
          AND p.data LIKE :dataMonster
          AND (:fom IS NULL OR p.registrert_dato >= :fom)
          AND (:tom IS NULL OR p.registrert_dato < :tom)
        GROUP BY TO_CHAR(p.registrert_dato, 'YYYY')
        ORDER BY 1
        """,
    )
    fun tellPerAarMedDataLike(
        @Param("prosessType") prosessType: String,
        @Param("dataMonster") dataMonster: String,
        @Param("fom") fom: LocalDateTime?,
        @Param("tom") tom: LocalDateTime?,
    ): List<Array<Any>>
}
