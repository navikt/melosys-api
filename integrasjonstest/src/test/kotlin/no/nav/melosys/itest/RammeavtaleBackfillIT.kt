package no.nav.melosys.itest

import io.kotest.assertions.withClue
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
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
        // f.eks. etter at saksbehandleren endret avhukingen. Vakten filtrerer ingenting ved førstegangskjøringen
        // (kolonnen er tom da, og V170 og V171 kjører i samme Flyway-run); verdien ligger i re-kjøringer, enten
        // etter utrullingen for å ta deploy-gapet, eller manuelt etter en feilet V171.
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
    fun `V170 legger kun til kolonnen, og V171 inneholder kun backfill`() {
        // Splitten er selve poenget: ALTER-en er en dictionary-oppdatering, mens backfillen skanner
        // prosessinstans.data. Slås de sammen igjen, kan en treg backfill holde oppstarten så lenge at
        // liveness-proben dreper podden, og da re-kjører Flyway ALTER-en også — ORA-01430.
        //
        // Assertene er bevisst *hvitelister*, ikke svartelister. En tidligere variant spurte «inneholder V170
        // ingen UPDATE?», og et slikt fravær kan bare måles med en SQL-lekser. Fire runder med review fant fire
        // ulike leksikalske hull i den lekseren — semikolon i kommentar, blokkkommentar, sitert identifikator og
        // q-quoting — og hvert hull gjorde assertet stille grønt mens migreringen inneholdt backfillen. En
        // hviteliste snur feilretningen. Flyway-filer er dessuten checksum-frosne etter release, så eksakt
        // innhold er ikke sprøtt.
        //
        // Hvitelisten er likevel ikke nok alene, og [enLinjeErEnLinje] er den andre halvdelen — les den først.
        enLinjeErEnLinje(MIGRERING_KOLONNE)
        enLinjeErEnLinje(MIGRERING_BACKFILL)

        withClue("$MIGRERING_KOLONNE skal inneholde nøyaktig én setning: ALTER-en") {
            kjørbareLinjer(MIGRERING_KOLONNE) shouldBe listOf(
                "ALTER TABLE anmodningsperiode ADD er_fjernarbeid_twfa NUMBER(1);",
            )
        }

        val backfill = backfillSetninger()
        withClue("$MIGRERING_BACKFILL skal inneholde $ANTALL_BACKFILL_UPDATES setninger") {
            backfill shouldHaveSize ANTALL_BACKFILL_UPDATES
        }
        withClue("hver setning i $MIGRERING_BACKFILL skal være en UPDATE — DDL, PL/SQL-blokker og EXECUTE IMMEDIATE hører ikke hjemme her") {
            backfill.forAll { it shouldStartWith "UPDATE" }
        }
    }

    @Test
    fun `kolonnen er nullbar og uten default, slik tri-staten krever`() {
        // NULL = ikke besvart, 0 = nei, 1 = ja. En DEFAULT 0 ville lest alle historiske rader som et registrert
        // nei og ødelagt både uttrekket (WHERE = 1) og EessiService sin null-sjekk. Dette må sjekkes mot skjemaet,
        // ikke mot teksten i migreringsfila: en assert på antall ALTER-setninger ser ikke forskjell på med og uten
        // DEFAULT, og entiteten skriver kolonnen eksplisitt i hver INSERT, så testdata avslører den heller ikke.
        val kolonne = jdbcTemplate.queryForMap(
            """
            SELECT nullable, data_default
            FROM user_tab_columns
            WHERE table_name = 'ANMODNINGSPERIODE' AND column_name = 'ER_FJERNARBEID_TWFA'
            """.trimIndent(),
        )

        withClue("kolonnen må være nullbar for at NULL skal kunne bety «ikke besvart»") {
            kolonne["NULLABLE"] shouldBe "Y"
        }
        withClue("kolonnen må være uten DEFAULT — ellers blir hver historiske rad et registrert nei") {
            kolonne["DATA_DEFAULT"].shouldBeNull()
        }
    }

    /**
     * Kjører UPDATE-setningene fra backfill-migreringen. Kolonnen finnes allerede fordi Flyway kjørte `V170` ved
     * oppstart. `IS NULL`-vakten gjør kjøringen strengt additiv, så en ekstra kjøring er ufarlig.
     */
    private fun kjørBackfillFraMigreringsfil() {
        val oppdateringer = backfillSetninger()

        withClue("$MIGRERING_BACKFILL skal inneholde $ANTALL_BACKFILL_UPDATES UPDATE-setninger (false og true); endres den, må denne testen leses på nytt") {
            oppdateringer shouldHaveSize ANTALL_BACKFILL_UPDATES
        }
        oppdateringer.forEach { jdbcTemplate.execute(it) }
    }

    /**
     * Setningene i backfill-migreringen, delt på semikolon.
     *
     * Splittingen er med vilje naiv. Den trenger ikke være korrekt for vilkårlig SQL, fordi alt den brukes til er
     * hvitelister: setningene sammenlignes mot en forventet form, og de kjøres mot Oracle. Skulle et semikolon i
     * en streng-literal noen gang dele feil, ryker enten formkravet eller `jdbcTemplate` — begge høylytt. Den
     * strengbevisste lekseren som stod her før forsøkte i stedet å bevise et *fravær*, og der er en parse-feil
     * stille. Det er skillet som gjorde tre runder med lekserfunn mulige.
     */
    private fun backfillSetninger(): List<String> =
        kjørbareLinjer(MIGRERING_BACKFILL).joinToString("\n")
            .split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /**
     * Krever at ingen konstruksjon i migreringsfila kan spenne over et linjeskift.
     *
     * [kjørbareLinjer] kaster linjer som *starter* med `--`. Oracle gjør ikke det hvis en streng-literal eller
     * blokkkommentar aapnet paa en tidligere linje fortsatt staar aapen: da kan den forkastede linja lukke
     * literalen og deretter kjøre vilkaarlig SQL, usynlig for hvitelistene under. Verifisert — en `ALTER TABLE`
     * gjemt slik ble faktisk kjørt av Flyway mens splitt-testen var grønn.
     *
     * Svaret er ikke å parse slike konstruksjoner riktig; det var lekseren, og den tok fire runder uten å bli
     * ferdig. Svaret er å *forby forutsetningen*: er apostrofene balansert linje for linje, og finnes verken
     * doble anførselstegn eller blokkkommentarer, kan ingen konstruksjon krysse et linjeskift, og den naive
     * linjebaserte lesingen er da beviselig enig med Oracle. Begge filene overholder dette i dag. Trenger en
     * framtidig migrering noe av det, ryker denne — og da må testen leses på nytt, ikke lempes på.
     */
    private fun enLinjeErEnLinje(migrering: String) {
        kjørbareLinjer(migrering).forAll { linje ->
            withClue("$migrering: apostrofene må være balansert på hver linje — «$linje»") {
                linje.count { it == '\'' } % 2 shouldBe 0
            }
            withClue("$migrering: doble anførselstegn og blokkkommentarer er ikke støttet her — «$linje»") {
                linje.contains('"') shouldBe false
                linje.contains("/*") shouldBe false
            }
        }
    }

    /** Linjene i en migreringsfil, uten linjekommentarer og tomme linjer. */
    private fun kjørbareLinjer(migrering: String): List<String> =
        checkNotNull(
            javaClass.getResource("/db/migration/melosysDB/$migrering")?.readText(),
        ) { "Fant ikke $migrering på classpath — er migreringen fjernet eller omdøpt?" }
            .lines()
            .map { it.trimEnd() }
            .filterNot { it.isBlank() || it.trimStart().startsWith("--") }

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
