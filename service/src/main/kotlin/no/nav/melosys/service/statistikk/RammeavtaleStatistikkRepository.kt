package no.nav.melosys.service.statistikk

import no.nav.melosys.saksflytapi.domain.Prosessinstans
import org.springframework.data.jpa.repository.NativeQuery
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

/**
 * ⚠️ **Midlertidig løsning — leser fra en arbeidstabell.**
 *
 * Spørringen under henter rapporteringstall ut av `prosessinstans.data`, fordi TWFA-avhukingen i dag ikke finnes
 * noe annet sted. `prosessinstans` er en arbeidstabell som er ment å være kortlevd
 * (`V161__prosessinstans_prioritet.sql`, `V3.0_01__PROSESSINSTANS.sql`, KDoc-en på [Prosessinstans]). Det virker
 * kun fordi ingen slettejobb finnes ennå — ikke fordi det er holdbart.
 *
 * **Ikke kopier dette mønsteret.** Trengs noe fra en prosessinstans varig, skal det lagres et annet sted i
 * databasen. Planlagt fiks og backfill: se `README.md` i denne pakken.
 */
interface RammeavtaleStatistikkRepository : Repository<Prosessinstans, UUID> {

    /**
     * Henter én rad per ferdigbehandlet behandling der prosessdataen på anmodningen inneholder en gitt nøkkel=verdi,
     * med saksnummer (MEL-nr) og vedtaksdato. Brukes til å hente ut behandlinger der rammeavtale om fjernarbeid
     * (TWFA) er huket av, både for tellingen per vedtaksår og for listen over saker.
     *
     * Prosessdataen lagres som java.util.Properties-tekst (key=value per linje) i CLOB-kolonnen `data`, derfor
     * matches det med LIKE mot et `"<kode>=<verdi>"`-mønster. Utypet og uindeksert — se pakke-README-en for
     * hvorfor dette er teknisk gjeld og hva som skal erstatte det.
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
     * Datoen som gjelder er vedtaksdatoen (`vedtak_metadata.vedtak_dato`), ikke da anmodningen ble registrert.
     * Vedtaksåret utledes av kaller fra denne datoen.
     *
     * `DISTINCT` gjør at flere anmodningsprosesser på samme behandling kun gir én rad. `br.behandling_id` **må**
     * derfor stå i select-lista: uten den ville to ulike behandlinger på samme sak med samme vedtaksdato blitt
     * slått sammen til én rad, og antallet ville endret seg. Motsatt vei gir én fagsak med to TWFA-behandlinger
     * to rader med samme saksnummer, hvilket er tilsiktet — antallet teller behandlinger, ikke saker.
     *
     * Hver rad er `[saksnummer (String), behandlingId (Number), vedtaksdato (String, ISO-8601)]`. Datoen
     * formateres i databasen så lesingen ikke konverterer den. NB: skrivingen normaliserer `Instant` til
     * JVM-tidssonen (`hibernate.timezone.default_storage=NORMALIZE`), som i prod pinnes til Europe/Oslo via
     * `JAVA_TOOL_OPTIONS` i Dockerfile. Vedtaksåret følger derfor norsk lokaltid — et vedtak fattet
     * 2024-12-31T23:00Z ligger i 2025. Det har vært slik siden første versjon av uttrekket.
     *
     * `fom`/`tom` er valgfrie (null = ingen grense) og gjelder vedtaksdatoen.
     */
    @NativeQuery(
        """
        SELECT DISTINCT b.saksnummer AS saksnummer,
                        br.behandling_id AS behandling_id,
                        TO_CHAR(vm.vedtak_dato, 'YYYY-MM-DD') AS vedtaksdato
        FROM prosessinstans p
        JOIN behandling b ON b.id = p.behandling_id
        -- behandlingsresultat har behandling_id som PK, og vedtak_metadata deler den via @MapsId
        JOIN behandlingsresultat br ON br.behandling_id = p.behandling_id
        JOIN vedtak_metadata vm ON vm.behandlingsresultat_id = br.behandling_id
        WHERE p.prosess_type = :prosessType
          AND p.data LIKE :dataLikePattern
          AND br.resultat_type = :resultatType
          AND vm.vedtak_dato IS NOT NULL
          AND (:fom IS NULL OR vm.vedtak_dato >= :fom)
          AND (:tom IS NULL OR vm.vedtak_dato < :tom)
        -- ISO-formatert dato sorterer identisk med timestampen. behandling_id er med som tiebreaker slik at to
        -- behandlinger på samme sak med samme vedtaksdato får en stabil rekkefølge mellom to uttrekk
        ORDER BY vedtaksdato, saksnummer, behandling_id
        """,
    )
    fun finnFerdigbehandledeMedDataLike(
        @Param("prosessType") prosessType: String,
        @Param("dataLikePattern") dataLikePattern: String,
        @Param("resultatType") resultatType: String,
        @Param("fom") fom: LocalDateTime?,
        @Param("tom") tom: LocalDateTime?,
    ): List<Array<Any>>
}
