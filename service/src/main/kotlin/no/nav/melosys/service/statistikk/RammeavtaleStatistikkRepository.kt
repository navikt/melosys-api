package no.nav.melosys.service.statistikk

import no.nav.melosys.domain.Anmodningsperiode
import org.springframework.data.jpa.repository.NativeQuery
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

/**
 * Uttrekk over behandlinger der rammeavtale om fjernarbeid i EØS (TWFA) er huket av, til Medlemskap og avgift.
 *
 * Kilden er kolonnen `anmodningsperiode.er_fjernarbeid_twfa` (V170), som henger på `behandlingsresultat` — den
 * durable saksmodellen. Frem til august 2026 ble tallene hentet ut av CLOB-en `prosessinstans.data` med `LIKE`;
 * `prosessinstans` er en arbeidstabell som er ment å være kortlevd, og tallene ville forsvunnet stille den dagen
 * en slettejobb ble implementert. Se `README.md` i denne pakken.
 */
interface RammeavtaleStatistikkRepository : Repository<Anmodningsperiode, Long> {

    /**
     * Henter én rad per ferdigbehandlet behandling der rammeavtale om fjernarbeid er huket av på anmodningen,
     * med saksnummer (MEL-nr) og vedtaksdato. Brukes både til tellingen per vedtaksår og til listen over saker.
     *
     * `er_fjernarbeid_twfa` er tri-state: 1 = ja, 0 = nei, NULL = ikke besvart (anmodninger fra før flagget fantes,
     * og anmodninger der saksbehandler lot avhukingen stå tom). Kun 1 telles.
     *
     * Behandlingen regnes som ferdigbehandlet når lovvalget er fastsatt, dvs. `resultat_type = :resultatType`
     * (FASTSATT_LOVVALGSLAND) og det finnes en vedtaksdato. Både innvilgelse og avslag på anmodningen får denne
     * resultattypen — utfallet ligger på lovvalgsperioden — så begge telles med. Vi filtrerer bevisst ikke på
     * `behandling.status = 'AVSLUTTET'`, fordi artikkel 13-saker ender i `MIDLERTIDIG_LOVVALGSBESLUTNING`
     * selv om vedtak er fattet.
     *
     * NB: `resultat_type` kan endres i ettertid (ANNULLERT via AnnullerSakService, HENLEGGELSE via
     * HenleggelseService) uten at vedtaksdatoen fjernes. Tallene er derfor ikke stabile over tid — en behandling
     * som annulleres senere forsvinner fra vedtaksåret sitt. Det løses ikke av kildebyttet; det krever DVH-strøm
     * med `funksjonell_tid` eller en snapshot-tabell.
     *
     * Datoen som gjelder er vedtaksdatoen (`vedtak_metadata.vedtak_dato`), ikke da anmodningen ble registrert.
     * Vedtaksåret utledes av kaller fra denne datoen.
     *
     * `DISTINCT` gjør at flere anmodningsperioder på samme behandling kun gir én rad. Saksflyten tillater i praksis
     * kun én (`AnmodningsperiodeService.hentFørsteAnmodningsperiode` kaster ved != 1), men skjemaet gjør det ikke,
     * og et uttrekk til offisiell rapportering skal ikke dobbelttelle om en rad blir liggende igjen.
     * `br.behandling_id` **må** stå i select-lista: uten den ville to ulike behandlinger på samme sak med samme
     * vedtaksdato blitt slått sammen til én rad, og antallet ville endret seg. Motsatt vei gir én fagsak med to
     * TWFA-behandlinger to rader med samme saksnummer, hvilket er tilsiktet — antallet teller behandlinger, ikke saker.
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
        FROM anmodningsperiode ap
        -- behandlingsresultat har behandling_id som PK, og vedtak_metadata deler den via @MapsId
        JOIN behandlingsresultat br ON br.behandling_id = ap.beh_resultat_id
        JOIN behandling b ON b.id = br.behandling_id
        JOIN vedtak_metadata vm ON vm.behandlingsresultat_id = br.behandling_id
        WHERE ap.er_fjernarbeid_twfa = 1
          AND br.resultat_type = :resultatType
          AND vm.vedtak_dato IS NOT NULL
          AND (:fom IS NULL OR vm.vedtak_dato >= :fom)
          AND (:tom IS NULL OR vm.vedtak_dato < :tom)
        -- ISO-formatert dato sorterer identisk med timestampen. behandling_id er med som tiebreaker slik at to
        -- behandlinger på samme sak med samme vedtaksdato får en stabil rekkefølge mellom to uttrekk
        ORDER BY vedtaksdato, saksnummer, behandling_id
        """,
    )
    fun finnFerdigbehandledeMedFjernarbeid(
        @Param("resultatType") resultatType: String,
        @Param("fom") fom: LocalDateTime?,
        @Param("tom") tom: LocalDateTime?,
    ): List<Array<Any>>
}
