package no.nav.melosys.itest

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.anmodningsperiode
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate

/**
 * Verifiserer backfill-en i `V171__backfill_anmodningsperiode_fjernarbeid_twfa.sql` mot ekte Oracle.
 * Kolonnen selv legges til av `V170`; de to migreringene er skilt fordi backfillen er den dyre halvdelen.
 *
 * Migreringen kjøres av Flyway ved oppstart av containeren, altså før noen testdata finnes — den kan derfor ikke
 * observeres direkte. Testen leser i stedet UPDATE-setningene ut av selve migreringsfila og kjører dem mot seedet
 * data. Det gjør at testen ikke kan komme i utakt med SQL-en som faktisk deployes: endres migreringen, endres
 * det som testes.
 *
 * Prosessinstans-radene er eneste kilde til historikken (RINA duger ikke, jf. pakke-README-en i
 * `service/.../statistikk`), så en backfill som treffer feil er ikke reversibel.
 */
class RammeavtaleBackfillIT(
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired private val prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val jdbcTemplate: JdbcTemplate,
) : ComponentTestBase() {

    @Test
    fun `backfill setter 1 for true, 0 for eksplisitt false og lar ubesvarte staa som null`() {
        val medTrue = lagSakMedAnmodningsperiode("MEL-BF-A")
        lagProsessinstans(medTrue, erFjernarbeid = true)

        val medFalse = lagSakMedAnmodningsperiode("MEL-BF-B")
        lagProsessinstans(medFalse, erFjernarbeid = false)

        // Prosessinstans finnes, men flagget ble aldri satt (anmodning fra før feltet fantes).
        // ProsessinstansBuilder utelater nøkkelen helt når verdien er null
        val utenFlagg = lagSakMedAnmodningsperiode("MEL-BF-C")
        lagProsessinstans(utenFlagg, erFjernarbeid = null)

        // Ingen prosessinstans i det hele tatt
        val utenProsess = lagSakMedAnmodningsperiode("MEL-BF-D")

        kjørBackfillFraMigreringsfil()

        flagget(medTrue) shouldBe 1
        flagget(medFalse) shouldBe 0
        withClue("Et ubesvart spørsmål skal ikke bli til et registrert nei") {
            flagget(utenFlagg).shouldBeNull()
            flagget(utenProsess).shouldBeNull()
        }
    }

    @Test
    fun `backfill treffer ikke flagg satt paa en annen prosesstype`() {
        // Kun ANMODNING_OM_UNNTAK-prosessen bærer saksbehandlerens avhuking. Uten prosesstype-filteret i
        // migreringen ville flagget kunne plukkes opp fra en vilkårlig annen saga på samme behandling
        val behandlingId = lagSakMedAnmodningsperiode("MEL-BF-E")
        lagProsessinstans(behandlingId, erFjernarbeid = true, prosessType = ProsessType.SEND_BREV)

        kjørBackfillFraMigreringsfil()

        flagget(behandlingId).shouldBeNull()
    }

    @Test
    fun `backfill lar true vinne naar samme behandling har baade true og false`() {
        // Anmodning kan ha blitt forsøkt sendt flere ganger. Rekkefølgen mellom de to UPDATE-ene skal ikke avgjøre
        // resultatet, og en sak som faktisk er behandlet etter rammeavtalen skal ikke falle ut av rapporteringen
        val behandlingId = lagSakMedAnmodningsperiode("MEL-BF-F")
        lagProsessinstans(behandlingId, erFjernarbeid = false)
        lagProsessinstans(behandlingId, erFjernarbeid = true)

        kjørBackfillFraMigreringsfil()

        flagget(behandlingId) shouldBe 1
    }

    @Test
    fun `backfill setter flagget paa alle anmodningsperioder paa behandlingen`() {
        val behandlingId = lagSakMedAnmodningsperiode("MEL-BF-G", ekstraPeriodeFom = LocalDate.of(2022, 1, 1))
        lagProsessinstans(behandlingId, erFjernarbeid = true)

        kjørBackfillFraMigreringsfil()

        val verdier = jdbcTemplate.queryForList(
            "SELECT er_fjernarbeid_twfa FROM anmodningsperiode WHERE beh_resultat_id = ?",
            Int::class.javaObjectType,
            behandlingId,
        )
        verdier shouldHaveSize 2
        verdier.toSet() shouldBe setOf(1)
    }

    @Test
    fun `backfill roerer ikke en rad som allerede har en verdi`() {
        // IS NULL-vakten gjør kjøringen strengt additiv. Uten den ville en ny kjøring av backfillen overskrive
        // verdien ny kode har skrevet, med det prosessinstansen sier — og prosessinstansen kan være utdatert,
        // f.eks. etter at saksbehandleren endret avhukingen. Vakten er også det som gjør at V171 kan kjøres
        // manuelt før deploy uten at Flyway-kjøringen etterpå ødelegger noe.
        val alleredeNei = lagSakMedAnmodningsperiode("MEL-BF-H")
        lagProsessinstans(alleredeNei, erFjernarbeid = true)
        settFlagg(alleredeNei, 0)

        val alleredeJa = lagSakMedAnmodningsperiode("MEL-BF-I")
        lagProsessinstans(alleredeJa, erFjernarbeid = false)
        settFlagg(alleredeJa, 1)

        kjørBackfillFraMigreringsfil()

        withClue("true-setningen skal ikke overskrive et registrert nei") {
            flagget(alleredeNei) shouldBe 0
        }
        withClue("false-setningen skal ikke overskrive et registrert ja") {
            flagget(alleredeJa) shouldBe 1
        }
    }

    @Test
    fun `kolonnen legges til i V170 og backfillen ligger i V171`() {
        // Splitten er selve poenget: ALTER-en er ren metadataendring, mens backfillen skanner prosessinstans.data.
        // Slås de sammen igjen, kan en treg backfill holde oppstarten så lenge at liveness-proben dreper podden
        // før kolonnen finnes. Testen over ville da fortsatt vært grønn — den leser bare V171 — så splitten
        // trenger sin egen assert.
        withClue("$MIGRERING_KOLONNE skal legge til kolonnen") {
            setningerSomStarterMed(MIGRERING_KOLONNE, "ALTER") shouldHaveSize 1
        }
        withClue("$MIGRERING_KOLONNE skal ikke inneholde backfill — den hører hjemme i $MIGRERING_BACKFILL") {
            setningerSomStarterMed(MIGRERING_KOLONNE, "UPDATE").shouldBeEmpty()
        }
        withClue("$MIGRERING_BACKFILL skal ikke endre skjemaet") {
            setningerSomStarterMed(MIGRERING_BACKFILL, "ALTER").shouldBeEmpty()
        }
        withClue("$MIGRERING_BACKFILL skal inneholde backfillen") {
            setningerSomStarterMed(MIGRERING_BACKFILL, "UPDATE") shouldHaveSize ANTALL_BACKFILL_UPDATES
        }
    }

    /**
     * Kjører UPDATE-setningene fra backfill-migreringen. Kolonnen finnes allerede fordi Flyway kjørte `V170` ved
     * oppstart. `IS NULL`-vakten gjør kjøringen strengt additiv, så en ekstra kjøring er ufarlig.
     */
    private fun kjørBackfillFraMigreringsfil() {
        val oppdateringer = setningerSomStarterMed(MIGRERING_BACKFILL, "UPDATE")

        withClue("$MIGRERING_BACKFILL skal inneholde $ANTALL_BACKFILL_UPDATES UPDATE-setninger (false og true); endres den, må denne testen leses på nytt") {
            oppdateringer shouldHaveSize ANTALL_BACKFILL_UPDATES
        }
        oppdateringer.forEach { jdbcTemplate.execute(it) }
    }

    /**
     * Leser setningene ut av en migreringsfil på classpath, uten kommentarlinjer.
     */
    private fun setningerSomStarterMed(migrering: String, nøkkelord: String): List<String> {
        val sql = checkNotNull(
            javaClass.getResource("/db/migration/melosysDB/$migrering")?.readText(),
        ) { "Fant ikke $migrering på classpath — er migreringen fjernet eller omdøpt?" }

        return sql.split(";")
            .map { it.lines().filterNot { linje -> linje.trimStart().startsWith("--") }.joinToString("\n").trim() }
            .filter { it.startsWith(nøkkelord, ignoreCase = true) }
    }

    private fun flagget(behandlingId: Long): Int? = jdbcTemplate.queryForObject(
        "SELECT er_fjernarbeid_twfa FROM anmodningsperiode WHERE beh_resultat_id = ? AND fom_dato = ?",
        Int::class.javaObjectType,
        behandlingId,
        java.sql.Date.valueOf(FØRSTE_FOM),
    )

    /** Speiler det ny kode gjør ved anmodning: kolonnen er allerede fylt når backfillen kjører. */
    private fun settFlagg(behandlingId: Long, verdi: Int) {
        jdbcTemplate.update(
            "UPDATE anmodningsperiode SET er_fjernarbeid_twfa = ? WHERE beh_resultat_id = ?",
            verdi,
            behandlingId,
        )
    }

    private fun lagSakMedAnmodningsperiode(saksnummer: String, ekstraPeriodeFom: LocalDate? = null): Long {
        val fagsak = fagsakRepository.save(
            Fagsak.forTest {
                this.saksnummer = saksnummer
                type = Sakstyper.EU_EOS
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                status = Saksstatuser.LOVVALG_AVKLART
                medBruker()
            },
        )

        val behandlingsresultat = Behandlingsresultat.forTest {
            behandlingsmåte = Behandlingsmaate.MANUELT
            type = Behandlingsresultattyper.ANMODNING_OM_UNNTAK
            fastsattAvLand = Land_iso2.NO
            behandling {
                medFagsak(fagsak)
                type = Behandlingstyper.FØRSTEGANG
                status = Behandlingsstatus.UNDER_BEHANDLING
                tema = Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY
            }
            anmodningsperiode {
                fom = FØRSTE_FOM
                tom = FØRSTE_FOM.plusMonths(6)
            }
            ekstraPeriodeFom?.let { ekstraFom ->
                anmodningsperiode {
                    fom = ekstraFom
                    tom = ekstraFom.plusMonths(6)
                }
            }
        }

        return behandlingsresultatRepository.saveAndFlush(behandlingsresultat).hentBehandling().id
    }

    private fun lagProsessinstans(
        behandlingId: Long,
        erFjernarbeid: Boolean?,
        prosessType: ProsessType = ProsessType.ANMODNING_OM_UNNTAK,
    ) {
        val behandling = behandlingsresultatRepository.findById(behandlingId).orElseThrow().hentBehandling()
        val prosessinstans = Prosessinstans.forTest {
            id = null
            type = prosessType
            medBehandling(behandling)
            // Speiler ProsessinstansBuilder: null utelater nøkkelen helt, false skrives eksplisitt
            erFjernarbeid?.let { medData(ProsessDataKey.ER_FJERNARBEID_TWFA, it) }
        }
        prosessinstansRepository.saveAndFlush(prosessinstans)
    }

    companion object {
        private const val MIGRERING_KOLONNE = "V170__anmodningsperiode_fjernarbeid_twfa.sql"
        private const val MIGRERING_BACKFILL = "V171__backfill_anmodningsperiode_fjernarbeid_twfa.sql"
        private const val ANTALL_BACKFILL_UPDATES = 2
        private val FØRSTE_FOM: LocalDate = LocalDate.of(2023, 1, 1)
    }
}
