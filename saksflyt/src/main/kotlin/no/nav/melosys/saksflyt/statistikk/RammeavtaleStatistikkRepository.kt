package no.nav.melosys.saksflyt.statistikk

import no.nav.melosys.saksflytapi.domain.Prosessinstans
import org.springframework.data.jpa.repository.NativeQuery
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface RammeavtaleStatistikkRepository : Repository<Prosessinstans, UUID> {

    /**
     * Teller ferdigbehandlede behandlinger per vedtaksår, der prosessdataen på anmodningen inneholder en gitt
     * nøkkel=verdi.
     *
     * Prosessdataen lagres som java.util.Properties-tekst (key=value per linje) i CLOB-kolonnen `data`, derfor
     * matches det med LIKE mot et `"<kode>=<verdi>"`-mønster. Brukes til å hente ut antall behandlinger der
     * rammeavtale om fjernarbeid (TWFA) er huket av.
     *
     * Behandlingen regnes som ferdigbehandlet når lovvalget er fastsatt, dvs. `resultat_type = :resultatType`
     * (FASTSATT_LOVVALGSLAND) og det finnes en vedtaksdato. Både innvilgelse og avslag på anmodningen får denne
     * resultattypen — utfallet ligger på lovvalgsperioden — så begge telles med. Vi filtrerer bevisst ikke på
     * `behandling.status = 'AVSLUTTET'`, fordi artikkel 13-saker ender i `MIDLERTIDIG_LOVVALGSBESLUTNING`
     * selv om vedtak er fattet.
     *
     * NB: `resultat_type` kan endres i ettertid (ANNULLERT via AnnullerSakService, HENLEGGELSE via
     * HenleggelseService) uten at vedtaksdatoen fjernes. Tallene er derfor ikke stabile over tid — en behandling
     * som annulleres senere forsvinner fra vedtaksåret sitt.
     *
     * Året er året vedtaket ble fattet (`vedtak_metadata.vedtak_dato`), ikke da anmodningen ble registrert.
     * Det telles distinkte behandlinger, slik at flere anmodningsprosesser på samme behandling kun teller én gang.
     *
     * Hver rad er `[aar (String), antall (Number)]`. `fom`/`tom` er valgfrie (null = ingen grense) og gjelder
     * vedtaksdatoen.
     */
    @NativeQuery(
        """
        SELECT TO_CHAR(vm.vedtak_dato, 'YYYY') AS aar, COUNT(DISTINCT br.behandling_id) AS antall
        FROM prosessinstans p
        -- behandlingsresultat har behandling_id som PK, og vedtak_metadata deler den via @MapsId
        JOIN behandlingsresultat br ON br.behandling_id = p.behandling_id
        JOIN vedtak_metadata vm ON vm.behandlingsresultat_id = br.behandling_id
        WHERE p.prosess_type = :prosessType
          AND p.data LIKE :dataLikePattern
          AND br.resultat_type = :resultatType
          AND vm.vedtak_dato IS NOT NULL
          AND (:fom IS NULL OR vm.vedtak_dato >= :fom)
          AND (:tom IS NULL OR vm.vedtak_dato < :tom)
        GROUP BY TO_CHAR(vm.vedtak_dato, 'YYYY')
        ORDER BY 1
        """,
    )
    fun tellFerdigbehandledePerVedtaksaarMedDataLike(
        @Param("prosessType") prosessType: String,
        @Param("dataLikePattern") dataLikePattern: String,
        @Param("resultatType") resultatType: String,
        @Param("fom") fom: LocalDateTime?,
        @Param("tom") tom: LocalDateTime?,
    ): List<Array<Any>>
}
