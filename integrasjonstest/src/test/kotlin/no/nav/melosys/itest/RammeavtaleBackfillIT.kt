package no.nav.melosys.itest

import io.kotest.assertions.withClue
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
 * Verifiserer backfill-en i `V170__anmodningsperiode_fjernarbeid_twfa.sql` mot ekte Oracle.
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

    /**
     * Kjører UPDATE-setningene fra migreringsfila. ALTER TABLE hoppes over — kolonnen finnes allerede fordi
     * Flyway kjørte migreringen ved oppstart. Backfill-en er idempotent, så en ekstra kjøring er ufarlig.
     */
    private fun kjørBackfillFraMigreringsfil() {
        val sql = checkNotNull(
            javaClass.getResource("/db/migration/melosysDB/$MIGRERING")?.readText(),
        ) { "Fant ikke $MIGRERING på classpath — er migreringen fjernet eller omdøpt?" }

        val oppdateringer = sql.split(";")
            .map { it.lines().filterNot { linje -> linje.trimStart().startsWith("--") }.joinToString("\n").trim() }
            .filter { it.startsWith("UPDATE", ignoreCase = true) }

        withClue("Migreringen skal inneholde to UPDATE-setninger (false og true); endres den, må denne testen leses på nytt") {
            oppdateringer shouldHaveSize 2
        }
        oppdateringer.forEach { jdbcTemplate.execute(it) }
    }

    private fun flagget(behandlingId: Long): Int? = jdbcTemplate.queryForObject(
        "SELECT er_fjernarbeid_twfa FROM anmodningsperiode WHERE beh_resultat_id = ? AND fom_dato = ?",
        Int::class.javaObjectType,
        behandlingId,
        java.sql.Date.valueOf(FØRSTE_FOM),
    )

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
        private const val MIGRERING = "V170__anmodningsperiode_fjernarbeid_twfa.sql"
        private val FØRSTE_FOM: LocalDate = LocalDate.of(2023, 1, 1)
    }
}
