package no.nav.melosys.itest

import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContainExactly
import no.nav.melosys.Application
import no.nav.melosys.service.avgift.aarsavregning.skattepliktig.VedtaksmetadataFiksService.Companion.PATCH_MARKOER
import no.nav.security.mock.oauth2.MockOAuth2Server
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

/**
 * Dekker datafiksen i MELOSYS-8174 (Q4a/Q4b): endepunktet skriver rett i `vedtak_metadata` i prod,
 * så garantiene her er de som avgjør om en feilkjøring kan gjøre skade.
 *
 * Testene fastholder særlig funnene fra kodereviewen 20.08:
 *  - skarp kjøring kan ikke skje uten eksplisitt scope, over taket, eller på en behandlingstype
 *    vi ikke kan utlede vedtakstype for,
 *  - angre rører kun rader som fortsatt er urørt patch (markør i BÅDE registrert_av og endret_av).
 */
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
class VedtaksmetadataFiksIT(
    @Autowired val mockMvc: MockMvc,
    @Autowired val mockOAuth2Server: MockOAuth2Server,
    @Autowired val jdbcTemplate: JdbcTemplate
) : OracleTestContainerBase() {

    private val fiksUrl = "/admin/aarsavregninger/saker/skattepliktige/vedtaksmetadata-fiks"
    private val angreUrl = "$fiksUrl/angre"

    @Test
    fun `preview viser radene som ville blitt satt inn, uten å skrive noe`() {
        val brId = seedDefektBehandling("MEL-901", "NY_VURDERING", "2024-10-23 09:54:00")

        kall(fiksUrl, """{"saksnummer":["MEL-901"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderFunnet").value(1))
            .andExpect(jsonPath("$.antallRaderInnsatt").value(0))
            .andExpect(jsonPath("$.rader[0].behandlingsresultatId").value(brId))
            .andExpect(jsonPath("$.rader[0].blirVedtakDato").value("2024-10-23 09:54:00"))
            // klagefrist = vedtaksdato + 42 dager, jf. Flyway-patchen V7.6_04
            .andExpect(jsonPath("$.rader[0].blirKlagefrist").value("2024-12-04"))
            .andExpect(jsonPath("$.rader[0].blirVedtakType").value("ENDRINGSVEDTAK"))
            .andExpect(jsonPath("$.utenMetadataPerSak['MEL-901']").value(1))

        antallMetadata() shouldBe 0
    }

    @Test
    fun `skarp setter inn radene med markør og proxy-dato, og er idempotent`() {
        val foerstegang = seedDefektBehandling("MEL-902", "FØRSTEGANG", "2023-11-23 08:00:00")
        val nyVurdering = seedDefektBehandling("MEL-902", "NY_VURDERING", "2024-10-23 09:54:00")

        kall(fiksUrl, """{"saksnummer":["MEL-902"],"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderInnsatt").value(2))
            .andExpect(jsonPath("$.avvik").value(false))
            .andExpect(jsonPath("$.utenMetadataPerSak").isEmpty)

        vedtakstype(foerstegang) shouldBe "FØRSTEGANGSVEDTAK"
        vedtakstype(nyVurdering) shouldBe "ENDRINGSVEDTAK"
        // vedtak_dato er proxyen behandlingsresultat.endret_dato, ikke tidspunktet fiksen kjørte
        vedtaksdato(nyVurdering) shouldBe "2024-10-23 09:54:00"
        registrertAv(nyVurdering) shouldBe PATCH_MARKOER

        kall(fiksUrl, """{"saksnummer":["MEL-902"],"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderFunnet").value(0))
            .andExpect(jsonPath("$.antallRaderInnsatt").value(0))

        antallMetadata() shouldBe 2
    }

    @Test
    fun `skarp uten saksnummer avvises i stedet for å treffe default-sakene`() {
        seedDefektBehandling("MEL-903", "NY_VURDERING", "2024-10-23 09:54:00")

        kall(fiksUrl, """{"skarp":true}""")
            .andExpect(status().isBadRequest)

        antallMetadata() shouldBe 0
    }

    @Test
    fun `skarp avvises når den ville satt inn flere rader enn maksAntallRader`() {
        seedDefektBehandling("MEL-904", "NY_VURDERING", "2024-10-23 09:54:00")
        seedDefektBehandling("MEL-904", "FØRSTEGANG", "2023-11-23 08:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-904"],"skarp":true,"maksAntallRader":1}""")
            .andExpect(status().isBadRequest)

        antallMetadata() shouldBe 0
    }

    @Test
    fun `behandlingstype uten kjent vedtakstype flagges i preview og blokkerer skarp`() {
        val klage = seedDefektBehandling("MEL-905", "KLAGE", "2024-05-05 10:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-905"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ukjentBehType[0]").value(klage))

        kall(fiksUrl, """{"saksnummer":["MEL-905"],"skarp":true}""")
            .andExpect(status().isBadRequest)

        antallMetadata() shouldBe 0
    }

    @Test
    fun `filteret hopper over behandlinger som ikke er avsluttet eller allerede har metadata`() {
        val avsluttet = seedDefektBehandling("MEL-906", "NY_VURDERING", "2024-10-23 09:54:00")
        seedDefektBehandling("MEL-906", "NY_VURDERING", "2024-09-01 09:00:00", status = "UNDER_BEHANDLING")
        val medMetadata = seedDefektBehandling("MEL-906", "NY_VURDERING", "2024-08-01 09:00:00")
        jdbcTemplate.update(
            """INSERT INTO vedtak_metadata (behandlingsresultat_id, vedtak_dato, vedtak_klagefrist, vedtak_type,
               registrert_dato, endret_dato, registrert_av, endret_av)
               VALUES (?, SYSTIMESTAMP, SYSDATE, 'ENDRINGSVEDTAK', SYSTIMESTAMP, SYSTIMESTAMP, 'MELOSYS', 'MELOSYS')""",
            medMetadata
        )

        kall(fiksUrl, """{"saksnummer":["MEL-906"],"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderInnsatt").value(1))

        patchedeIder() shouldContainExactly listOf(avsluttet)
    }

    @Test
    fun `angre lar rader som er endret etterpå stå, og rører ikke ekte vedtaksmetadata`() {
        val urørt = seedDefektBehandling("MEL-907", "NY_VURDERING", "2024-10-23 09:54:00")
        val endretEtterpaa = seedDefektBehandling("MEL-907", "FØRSTEGANG", "2023-11-23 08:00:00")
        kall(fiksUrl, """{"saksnummer":["MEL-907"],"skarp":true}""").andExpect(status().isOk)

        // Slik ser raden ut hvis noe senere skriver en ekte vedtaksdato: registrert_av beholder
        // markøren (@CreatedBy settes kun ved insert), mens endret_av flyttes (@LastModifiedBy).
        jdbcTemplate.update(
            "UPDATE vedtak_metadata SET vedtak_dato = SYSTIMESTAMP, endret_av = 'Z994321' WHERE behandlingsresultat_id = ?",
            endretEtterpaa
        )

        kall(angreUrl, """{"saksnummer":["MEL-907"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderFunnet").value(1))
            .andExpect(jsonPath("$.antallSlettet").value(0))
            .andExpect(jsonPath("$.antallEndretEtterpaa").value(1))

        kall(angreUrl, """{"saksnummer":["MEL-907"],"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallSlettet").value(1))
            .andExpect(jsonPath("$.antallEndretEtterpaa").value(1))

        patchedeIder() shouldContainExactly listOf(endretEtterpaa)
        antallMetadata() shouldBe 1
    }

    @Test
    fun `angre med saksnummer rører kun den saken`() {
        seedDefektBehandling("MEL-908", "NY_VURDERING", "2024-10-23 09:54:00")
        val sak909 = seedDefektBehandling("MEL-909", "NY_VURDERING", "2024-10-23 09:54:00")
        kall(fiksUrl, """{"saksnummer":["MEL-908","MEL-909"],"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderInnsatt").value(2))

        kall(angreUrl, """{"saksnummer":["MEL-908"],"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallSlettet").value(1))

        patchedeIder() shouldContainExactly listOf(sak909)
    }

    @Test
    fun `ugyldig saksnummer og for lang liste avvises før noe kjøres`() {
        kall(fiksUrl, """{"saksnummer":["MEL-1' OR 1=1--"],"skarp":true}""")
            .andExpect(status().isBadRequest)

        val forMange = (1..26).joinToString(",") { "\"MEL-$it\"" }
        kall(fiksUrl, """{"saksnummer":[$forMange]}""")
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `preview varsler når patchen kaprer nyeste-plassen i vedtaksdato-sorteringen`() {
        // Ekte nyeste vedtak er fra 2023. Den defekte raden er sist rørt i 2024, så proxy-datoen
        // ville lagt seg øverst og byttet hvilken behandling avgiftsgrunnlaget hentes fra.
        val ekteNyeste = seedIntaktBehandling("MEL-910", "FØRSTEGANG", "2023-03-01 10:00:00")
        val defekt = seedDefektBehandling("MEL-910", "NY_VURDERING", "2024-05-10 12:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-910"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspaavirkning[0].saksnummer").value("MEL-910"))
            .andExpect(jsonPath("$.sorteringspaavirkning[0].patchenVinnerNyeste").value(true))
            .andExpect(jsonPath("$.sorteringspaavirkning[0].nyesteFoerId").value(ekteNyeste))
            .andExpect(jsonPath("$.sorteringspaavirkning[0].nyesteFoerDato").value("2023-03-01 10:00:00"))
            .andExpect(jsonPath("$.sorteringspaavirkning[0].nyestePatchetId").value(defekt))
            .andExpect(jsonPath("$.sorteringspaavirkning[0].nyestePatchetDato").value("2024-05-10 12:00:00"))

        antallMetadata() shouldBe 1
    }

    @Test
    fun `preview melder fra når patchen ikke rører nyeste-plassen`() {
        seedIntaktBehandling("MEL-911", "FØRSTEGANG", "2025-01-01 10:00:00")
        seedDefektBehandling("MEL-911", "NY_VURDERING", "2024-05-10 12:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-911"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspaavirkning[0].patchenVinnerNyeste").value(false))
    }

    @Test
    fun `sak uten en eneste ekte vedtaksdato flagges som at patchen vinner`() {
        seedDefektBehandling("MEL-912", "NY_VURDERING", "2024-05-10 12:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-912"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspaavirkning[0].nyesteFoerId").doesNotExist())
            .andExpect(jsonPath("$.sorteringspaavirkning[0].patchenVinnerNyeste").value(true))
    }

    private fun kall(url: String, body: String) = mockMvc.perform(
        post(url)
            .header(AdminControllerApiKeyIT.API_KEY_HEADER, AdminControllerApiKeyIT.GYLDIG_API_NOKKEL)
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken()}")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(body)
    )

    private fun hentBearerToken(): String = mockOAuth2Server.issueToken(
        issuerId = "issuer1",
        subject = "testbruker",
        audience = "dumbdumb",
        claims = mapOf("oid" to "test-oid", "azp" to "test-azp", "NAVident" to "test123")
    ).serialize()

    /**
     * Lager en behandling med et behandlingsresultat som mangler raden i `vedtak_metadata` — altså
     * nøyaktig datafeilen fiksen retter opp.
     */
    private fun seedDefektBehandling(
        saksnummer: String,
        behType: String,
        endretDato: String,
        status: String = "AVSLUTTET"
    ): Long {
        jdbcTemplate.update(
            """MERGE INTO fagsak f USING (SELECT ? AS saksnummer FROM dual) k ON (f.saksnummer = k.saksnummer)
               WHEN NOT MATCHED THEN INSERT (saksnummer, fagsak_type, status, tema, registrert_dato, endret_dato, registrert_av, endret_av)
               VALUES (k.saksnummer, 'EU_EOS', 'LOVVALG_AVKLART', 'UNNTAK', SYSTIMESTAMP, SYSTIMESTAMP, 'IT', 'IT')""",
            saksnummer
        )
        jdbcTemplate.update(
            """INSERT INTO behandling (saksnummer, status, beh_type, beh_tema, behandlingsfrist,
               registrert_dato, endret_dato, registrert_av, endret_av)
               VALUES (?, ?, ?, 'REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING', DATE '2024-02-14',
               SYSTIMESTAMP, SYSTIMESTAMP, 'IT', 'IT')""",
            saksnummer, status, behType
        )
        val behandlingId = jdbcTemplate.queryForObject(
            "SELECT MAX(id) FROM behandling WHERE saksnummer = ?", Long::class.java, saksnummer
        )!!
        jdbcTemplate.update(
            """INSERT INTO behandlingsresultat (behandling_id, resultat_type, behandlingsmaate,
               registrert_dato, endret_dato, registrert_av, endret_av)
               VALUES (?, 'MEDLEM_I_FOLKETRYGDEN', 'MANUELT', ?, ?, 'IT', 'IT')""",
            behandlingId, java.sql.Timestamp.valueOf(endretDato), java.sql.Timestamp.valueOf(endretDato)
        )
        return behandlingId
    }

    /**
     * Behandling som allerede HAR vedtaksmetadata med en ekte vedtaksdato — klokka patch-radene
     * sammenlignes mot.
     */
    private fun seedIntaktBehandling(saksnummer: String, behType: String, vedtaksdato: String): Long {
        val behandlingsresultatId = seedDefektBehandling(saksnummer, behType, vedtaksdato)
        jdbcTemplate.update(
            """INSERT INTO vedtak_metadata (behandlingsresultat_id, vedtak_dato, vedtak_klagefrist, vedtak_type,
               registrert_dato, endret_dato, registrert_av, endret_av)
               VALUES (?, ?, TRUNC(CAST(? AS DATE)) + 42, 'FØRSTEGANGSVEDTAK', SYSTIMESTAMP, SYSTIMESTAMP, 'IT', 'IT')""",
            behandlingsresultatId,
            java.sql.Timestamp.valueOf(vedtaksdato),
            java.sql.Timestamp.valueOf(vedtaksdato)
        )
        return behandlingsresultatId
    }

    private fun antallMetadata(): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vedtak_metadata", Int::class.java)!!

    private fun patchedeIder(): List<Long> = jdbcTemplate.queryForList(
        "SELECT behandlingsresultat_id FROM vedtak_metadata WHERE registrert_av = ? ORDER BY behandlingsresultat_id",
        Long::class.java, PATCH_MARKOER
    ).filterNotNull()

    private fun vedtakstype(behandlingsresultatId: Long): String? = jdbcTemplate.queryForObject(
        "SELECT vedtak_type FROM vedtak_metadata WHERE behandlingsresultat_id = ?", String::class.java, behandlingsresultatId
    )

    private fun vedtaksdato(behandlingsresultatId: Long): String? = jdbcTemplate.queryForObject(
        "SELECT TO_CHAR(vedtak_dato, 'YYYY-MM-DD HH24:MI:SS') FROM vedtak_metadata WHERE behandlingsresultat_id = ?",
        String::class.java, behandlingsresultatId
    )

    private fun registrertAv(behandlingsresultatId: Long): String? = jdbcTemplate.queryForObject(
        "SELECT registrert_av FROM vedtak_metadata WHERE behandlingsresultat_id = ?", String::class.java, behandlingsresultatId
    )
}
