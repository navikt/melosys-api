package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import no.nav.melosys.Application
import no.nav.melosys.service.vedtak.IkkeYrkesaktivVedtaksmetadataFiksService.Companion.PATCH_MARKØR
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.sql.Timestamp

@ActiveProfiles("test")
@SpringBootTest(
    classes = [Application::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@EmbeddedKafka
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
@EnableMockOAuth2Server
@AutoConfigureMockMvc
class IkkeYrkesaktivVedtaksmetadataFiksIT(
    @Autowired val mockMvc: MockMvc,
    @Autowired val mockOAuth2Server: MockOAuth2Server,
    @Autowired val jdbcTemplate: JdbcTemplate
) : OracleTestContainerBase() {

    private val fiksUrl = "/admin/vedtaksmetadata-fiks/ikke-yrkesaktive"
    private val angreUrl = "$fiksUrl/angre"

    @Test
    fun `forhåndsvisning teller per gruppe og lister vedtakene som holdes utenfor, uten å skrive`() {
        seedFlytvedtak("MEL-1", "EU_EOS", "FØRSTEGANG", "2024-10-23 09:54:00")
        seedFlytvedtak("MEL-2", "EU_EOS", "FØRSTEGANG", "2024-11-01 10:00:00")
        seedFlytvedtak("MEL-3", "TRYGDEAVTALE", "NY_VURDERING", "2025-02-01 10:00:00")
        val utenProsessinstans = seedFlytvedtak("MEL-4", "EU_EOS", "NY_VURDERING", "2025-03-01 10:00:00", prosessinstans = null)

        kall(fiksUrl, """{}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.skarp").value(false))
            .andExpect(jsonPath("$.antallFunnet").value(3))
            .andExpect(jsonPath("$.antallInnsatt").value(0))
            .andExpect(jsonPath("$.antallIgjen").value(3))
            .andExpect(jsonPath("$.grupper.length()").value(3))
            .andExpect(jsonPath("$.grupper[0].gruppe").value("FIKSES"))
            .andExpect(jsonPath("$.grupper[0].sakstype").value("EU_EOS"))
            .andExpect(jsonPath("$.grupper[0].behandlingstype").value("FØRSTEGANG"))
            .andExpect(jsonPath("$.grupper[0].antall").value(2))
            .andExpect(jsonPath("$.grupper[0].eldsteEndretDato").value("2024-10-23 09:54:00"))
            .andExpect(jsonPath("$.grupper[1].sakstype").value("TRYGDEAVTALE"))
            .andExpect(jsonPath("$.grupper[1].antall").value(1))
            .andExpect(jsonPath("$.grupper[2].gruppe").value("UTEN_PROSESSINSTANS"))
            .andExpect(jsonPath("$.holdesUtenfor.length()").value(1))
            .andExpect(jsonPath("$.holdesUtenfor[0].behandlingId").value(utenProsessinstans))
            .andExpect(jsonPath("$.holdesUtenfor[0].saksnummer").value("MEL-4"))
            .andExpect(jsonPath("$.holdesUtenfor[0].prosessinstansDato").doesNotExist())

        antallMetadata() shouldBe 0
    }

    @Test
    fun `skarp gir flytvedtaket vedtaksdato, klagefrist, vedtakstype og merke, og er idempotent`() {
        val førstegang = seedFlytvedtak("MEL-10", "EU_EOS", "FØRSTEGANG", "2024-10-23 09:54:00")
        val nyVurdering = seedFlytvedtak("MEL-11", "TRYGDEAVTALE", "NY_VURDERING", "2025-02-01 23:30:00")

        kall(fiksUrl, """{"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.skarp").value(true))
            .andExpect(jsonPath("$.antallFunnet").value(2))
            .andExpect(jsonPath("$.antallInnsatt").value(2))
            .andExpect(jsonPath("$.antallIgjen").value(0))
            .andExpect(jsonPath("$.avvik").value(false))
            .andExpect(jsonPath("$.grupper").isEmpty)

        rad(førstegang) shouldBe Rad("2024-10-23 09:54:00", "2024-12-04", "FØRSTEGANGSVEDTAK", PATCH_MARKØR, PATCH_MARKØR)
        rad(nyVurdering) shouldBe Rad("2025-02-01 23:30:00", "2025-03-15", "ENDRINGSVEDTAK", PATCH_MARKØR, PATCH_MARKØR)

        kall(fiksUrl, """{"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallFunnet").value(0))
            .andExpect(jsonPath("$.antallInnsatt").value(0))

        antallMetadata() shouldBe 2
    }

    @Test
    fun `vedtak utenfor utvalget røres ikke`() {
        val fikses = seedFlytvedtak("MEL-20", "EU_EOS", "FØRSTEGANG", "2024-10-23 09:54:00")
        val medRadFraFør = seedFlytvedtak("MEL-21", "EU_EOS", "FØRSTEGANG", "2024-10-23 09:54:00")
        jdbcTemplate.update(
            """INSERT INTO vedtak_metadata (behandlingsresultat_id, vedtak_dato, vedtak_klagefrist, vedtak_type,
               registrert_dato, endret_dato, registrert_av, endret_av)
               VALUES (?, TIMESTAMP '2024-10-23 09:53:58', DATE '2024-12-04', 'FØRSTEGANGSVEDTAK', SYSTIMESTAMP, SYSTIMESTAMP, 'Z990000', 'Z990000')""",
            medRadFraFør
        )
        seedFlytvedtak("MEL-22", "EU_EOS", "FØRSTEGANG", "2024-10-23 09:54:00", fastsattAvLand = null)
        seedFlytvedtak("MEL-23", "EU_EOS", "FØRSTEGANG", "2024-10-23 09:54:00", behTema = "YRKESAKTIV")
        seedFlytvedtak("MEL-24", "FTRL", "FØRSTEGANG", "2024-10-23 09:54:00")
        seedFlytvedtak("MEL-25", "EU_EOS", "FØRSTEGANG", "2024-10-23 09:54:00", behStatus = "UNDER_BEHANDLING")
        seedFlytvedtak("MEL-26", "EU_EOS", "FØRSTEGANG", "2024-10-23 09:54:00", resultatType = "HENLEGGELSE")
        seedFlytvedtak("MEL-27", "EU_EOS", "FØRSTEGANG", "2024-10-23 09:54:00", prosessType = "IVERKSETT_VEDTAK_EOS")
        seedFlytvedtak("MEL-28", "EU_EOS", "NY_VURDERING", "2024-10-23 09:54:00", prosessinstans = null)
        seedFlytvedtak("MEL-29", "EU_EOS", "FØRSTEGANG", "2026-05-07 10:00:00", prosessinstans = "2026-02-04 10:00:00")
        seedFlytvedtak("MEL-30", "EU_EOS", "KLAGE", "2024-10-23 09:54:00")

        kall(fiksUrl, """{}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallFunnet").value(1))
            .andExpect(jsonPath("$.holdesUtenfor[*].saksnummer").value(
                org.hamcrest.Matchers.containsInAnyOrder("MEL-27", "MEL-28", "MEL-29", "MEL-30")
            ))
            .andExpect(jsonPath("$.holdesUtenfor[?(@.saksnummer == 'MEL-29')].gruppe").value("AVVIK_OVER_3_TIMER"))
            .andExpect(jsonPath("$.holdesUtenfor[?(@.saksnummer == 'MEL-30')].gruppe").value("ANNEN_BEHANDLINGSTYPE"))

        kall(fiksUrl, """{"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallInnsatt").value(1))

        patchedeIder() shouldContainExactly listOf(fikses)
        rad(medRadFraFør).registrertAv shouldBe "Z990000"
    }

    @Test
    fun `medlem i folketrygden fikses, lovvalg fastsatt av et annet land røres ikke`() {
        val medlem = seedFlytvedtak("MEL-31", "EU_EOS", "FØRSTEGANG", "2024-10-23 09:54:00", resultatType = "MEDLEM_I_FOLKETRYGDEN")
        seedFlytvedtak("MEL-32", "EU_EOS", "FØRSTEGANG", "2024-10-23 09:54:00", fastsattAvLand = "SE")

        kall(fiksUrl, """{"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallInnsatt").value(1))
            .andExpect(jsonPath("$.grupper").isEmpty)

        patchedeIder() shouldContainExactly listOf(medlem)
    }

    @Test
    fun `avvik på inntil 3 timer mot eldste prosessinstans fikses, over 3 timer holdes utenfor`() {
        // Prosessinstansen lå 1–2 timer feil april 2024 – august 2025, derfor tåles 3 timer
        val toTimerFør = seedFlytvedtak("MEL-40", "EU_EOS", "FØRSTEGANG", "2024-10-23 11:54:00", prosessinstans = "2024-10-23 09:54:00")
        val treTimerEtter = seedFlytvedtak("MEL-41", "EU_EOS", "FØRSTEGANG", "2024-10-23 06:54:00", prosessinstans = "2024-10-23 09:54:00")
        seedFlytvedtak("MEL-42", "EU_EOS", "FØRSTEGANG", "2024-10-23 12:55:00", prosessinstans = "2024-10-23 09:54:00")
        // En senere prosessinstans nær endret_dato redder ikke vedtaket: avviket måles mot den eldste
        val toProsessinstanser = seedFlytvedtak("MEL-43", "EU_EOS", "FØRSTEGANG", "2025-01-10 10:00:00", prosessinstans = "2024-12-01 10:00:00")
        seedProsessinstans(toProsessinstanser, "IVERKSETT_VEDTAK_IKKE_YRKESAKTIV", "2025-01-10 09:59:00")

        kall(fiksUrl, """{"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallInnsatt").value(2))
            .andExpect(jsonPath("$.holdesUtenfor.length()").value(2))
            .andExpect(jsonPath("$.holdesUtenfor[0].gruppe").value("AVVIK_OVER_3_TIMER"))
            .andExpect(jsonPath("$.holdesUtenfor[1].gruppe").value("AVVIK_OVER_3_TIMER"))

        patchedeIder() shouldContainExactly listOf(toTimerFør, treTimerEtter).sorted()
    }

    @Test
    fun `maksAntallRader begrenser hvert kall, eldste først, og antallIgjen teller ned til 0`() {
        val eldst = seedFlytvedtak("MEL-50", "EU_EOS", "FØRSTEGANG", "2023-01-01 10:00:00")
        val mellom = seedFlytvedtak("MEL-51", "TRYGDEAVTALE", "FØRSTEGANG", "2024-01-01 10:00:00")
        val nyest = seedFlytvedtak("MEL-52", "EU_EOS", "NY_VURDERING", "2025-01-01 10:00:00")

        kall(fiksUrl, """{"skarp":true,"maksAntallRader":2}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallFunnet").value(3))
            .andExpect(jsonPath("$.antallInnsatt").value(2))
            .andExpect(jsonPath("$.antallIgjen").value(1))
            .andExpect(jsonPath("$.avvik").value(false))
        patchedeIder() shouldContainExactly listOf(eldst, mellom)

        kall(fiksUrl, """{"skarp":true,"maksAntallRader":2}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallInnsatt").value(1))
            .andExpect(jsonPath("$.antallIgjen").value(0))
        patchedeIder() shouldContainExactly listOf(eldst, mellom, nyest)
    }

    @Test
    fun `maksAntallRader utenfor 1 til 500 avvises før noe skrives`() {
        seedFlytvedtak("MEL-60", "EU_EOS", "FØRSTEGANG", "2024-10-23 09:54:00")

        kall(fiksUrl, """{"skarp":true,"maksAntallRader":0}""").andExpect(status().isBadRequest)
        kall(fiksUrl, """{"skarp":true,"maksAntallRader":501}""").andExpect(status().isBadRequest)
        antallMetadata() shouldBe 0

        kall(fiksUrl, """{"skarp":true,"maksAntallRader":500}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallInnsatt").value(1))
    }

    @Test
    fun `angre sletter bare urørte rader fra datafiksen`() {
        val urørt = seedFlytvedtak("MEL-70", "EU_EOS", "FØRSTEGANG", "2024-10-23 09:54:00")
        val endretEtterpå = seedFlytvedtak("MEL-71", "EU_EOS", "FØRSTEGANG", "2024-11-23 09:54:00")
        val tømtEndretAv = seedFlytvedtak("MEL-72", "EU_EOS", "FØRSTEGANG", "2024-12-23 09:54:00")
        kall(fiksUrl, """{"skarp":true}""").andExpect(status().isOk)
        jdbcTemplate.update("UPDATE vedtak_metadata SET endret_av = 'Z994321' WHERE behandlingsresultat_id = ?", endretEtterpå)
        jdbcTemplate.update("UPDATE vedtak_metadata SET endret_av = NULL WHERE behandlingsresultat_id = ?", tømtEndretAv)

        val annenDatafiks = seedFlytvedtak("MEL-73", "EU_EOS", "FØRSTEGANG", "2025-01-23 09:54:00", prosessinstans = null)
        jdbcTemplate.update(
            """INSERT INTO vedtak_metadata (behandlingsresultat_id, vedtak_dato, vedtak_klagefrist, vedtak_type,
               registrert_dato, endret_dato, registrert_av, endret_av)
               VALUES (?, SYSTIMESTAMP, SYSDATE, 'FØRSTEGANGSVEDTAK', SYSTIMESTAMP, SYSTIMESTAMP, 'MELOSYS-8174-PATCH', 'MELOSYS-8174-PATCH')""",
            annenDatafiks
        )

        kall(angreUrl, """{}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.skarp").value(false))
            .andExpect(jsonPath("$.antallFunnet").value(1))
            .andExpect(jsonPath("$.antallSlettet").value(0))
            .andExpect(jsonPath("$.antallSomIkkeKanAngres").value(2))
        antallMetadata() shouldBe 4

        kall(angreUrl, """{"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallSlettet").value(1))
            .andExpect(jsonPath("$.antallSomIkkeKanAngres").value(2))
            .andExpect(jsonPath("$.avvik").value(false))

        harRad(urørt) shouldBe false
        harRad(endretEtterpå) shouldBe true
        harRad(tømtEndretAv) shouldBe true
        harRad(annenDatafiks) shouldBe true
    }

    @Test
    fun `angre uten body er en forhåndsvisning`() {
        seedFlytvedtak("MEL-80", "EU_EOS", "FØRSTEGANG", "2024-10-23 09:54:00")
        kall(fiksUrl, """{"skarp":true}""").andExpect(status().isOk)

        mockMvc.perform(post(angreUrl).header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken()}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.skarp").value(false))
            .andExpect(jsonPath("$.antallSlettet").value(0))

        antallMetadata() shouldBe 1
    }

    @Test
    fun `endepunktene krever bearer token med driftsgruppe`() {
        seedFlytvedtak("MEL-90", "EU_EOS", "FØRSTEGANG", "2024-10-23 09:54:00")

        listOf(fiksUrl, angreUrl).forEach { url ->
            mockMvc.perform(
                post(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken(grupper = emptyList())}")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content("""{"skarp":true}""")
            ).andExpect(status().isForbidden)

            mockMvc.perform(
                post(url)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content("""{"skarp":true}""")
            ).andExpect(status().isUnauthorized)
        }

        antallMetadata() shouldBe 0
    }

    private fun kall(url: String, body: String) = mockMvc.perform(
        post(url)
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken()}")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(body)
    )

    private fun hentBearerToken(
        grupper: List<String> = listOf(AdminControllerTilgangsstyringIT.DRIFTSGRUPPE_ID)
    ): String = mockOAuth2Server.issueToken(
        "issuer1",
        AdminControllerTilgangsstyringIT.CONSOLE_KLIENT_ID,
        DefaultOAuth2TokenCallback(
            issuerId = "issuer1",
            subject = "testbruker",
            audience = listOf("dumbdumb"),
            claims = mapOf(
                "oid" to "test-oid",
                "NAVident" to "test123",
                "groups" to grupper
            )
        )
    ).serialize()

    /** Avsluttet ikke-yrkesaktiv behandling med prosessinstans samtidig med `endret_dato`, med mindre noe annet er oppgitt. */
    private fun seedFlytvedtak(
        saksnummer: String,
        sakstype: String,
        behType: String,
        endretDato: String,
        prosessinstans: String? = endretDato,
        prosessType: String = "IVERKSETT_VEDTAK_IKKE_YRKESAKTIV",
        behTema: String = "IKKE_YRKESAKTIV",
        behStatus: String = "AVSLUTTET",
        fastsattAvLand: String? = "NO",
        resultatType: String = "FASTSATT_LOVVALGSLAND",
    ): Long {
        jdbcTemplate.update(
            """INSERT INTO fagsak (saksnummer, fagsak_type, status, tema, registrert_dato, endret_dato, registrert_av, endret_av)
               VALUES (?, ?, 'LOVVALG_AVKLART', 'MEDLEMSKAP_LOVVALG', SYSTIMESTAMP, SYSTIMESTAMP, 'IT', 'IT')""",
            saksnummer, sakstype
        )
        jdbcTemplate.update(
            """INSERT INTO behandling (saksnummer, status, beh_type, beh_tema, behandlingsfrist,
               registrert_dato, endret_dato, registrert_av, endret_av)
               VALUES (?, ?, ?, ?, DATE '2024-02-14', SYSTIMESTAMP, SYSTIMESTAMP, 'IT', 'IT')""",
            saksnummer, behStatus, behType, behTema
        )
        val behandlingId = jdbcTemplate.queryForObject(
            "SELECT MAX(id) FROM behandling WHERE saksnummer = ?", Long::class.java, saksnummer
        )!!
        jdbcTemplate.update(
            """INSERT INTO behandlingsresultat (behandling_id, resultat_type, behandlingsmaate, fastsatt_av_land,
               registrert_dato, endret_dato, registrert_av, endret_av)
               VALUES (?, ?, 'MANUELT', ?, ?, ?, 'IT', 'IT')""",
            behandlingId, resultatType, fastsattAvLand, Timestamp.valueOf(endretDato), Timestamp.valueOf(endretDato)
        )
        if (prosessinstans != null) seedProsessinstans(behandlingId, prosessType, prosessinstans)
        return behandlingId
    }

    private fun seedProsessinstans(behandlingId: Long, prosessType: String, registrertDato: String) {
        jdbcTemplate.update(
            """INSERT INTO prosessinstans (uuid, prosess_type, behandling_id, status, registrert_dato, endret_dato)
               VALUES (SYS_GUID(), ?, ?, 'FERDIG', ?, ?)""",
            prosessType, behandlingId, Timestamp.valueOf(registrertDato), Timestamp.valueOf(registrertDato)
        )
    }

    private data class Rad(
        val vedtakDato: String?,
        val klagefrist: String?,
        val vedtakType: String?,
        val registrertAv: String?,
        val endretAv: String?,
    )

    private fun rad(behandlingsresultatId: Long): Rad = jdbcTemplate.queryForObject(
        """SELECT TO_CHAR(vedtak_dato, 'YYYY-MM-DD HH24:MI:SS'), TO_CHAR(vedtak_klagefrist, 'YYYY-MM-DD'),
                  vedtak_type, registrert_av, endret_av
           FROM vedtak_metadata WHERE behandlingsresultat_id = ?""",
        { rs, _ -> Rad(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)) },
        behandlingsresultatId
    )

    private fun harRad(behandlingsresultatId: Long): Boolean = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM vedtak_metadata WHERE behandlingsresultat_id = ?", Int::class.java, behandlingsresultatId
    ) == 1

    private fun antallMetadata(): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vedtak_metadata", Int::class.java)!!

    private fun patchedeIder(): List<Long> = jdbcTemplate.queryForList(
        "SELECT behandlingsresultat_id FROM vedtak_metadata WHERE registrert_av = ? ORDER BY behandlingsresultat_id",
        Long::class.java, PATCH_MARKØR
    ).filterNotNull()
}
