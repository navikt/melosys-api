package no.nav.melosys.itest

import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldBeEmpty
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
 * Dekker datafiksen i MELOSYS-8174 (Q4a/Q4b) — `VedtaksmetadataFiksController` og
 * `VedtaksmetadataFiksService`. Endepunktet skriver rett i `vedtak_metadata` i prod, så garantiene
 * her er de som avgjør om en feilkjøring kan gjøre skade.
 *
 * Testene fastholder særlig funnene fra kodereviewene:
 *  - skarp kjøring kan ikke skje uten eksplisitt scope, over taket, eller på en behandlingstype
 *    vi ikke kan utlede vedtakstype for,
 *  - angre rører kun rader som fortsatt er urørt patch (markør i BÅDE registrert_av og endret_av),
 *    og skarp angre uten scope krever egen bekreftelse,
 *  - en patch-rad med tømt `endret_av` forsvinner ikke i stillhet, men telles som urullbar,
 *  - sorteringsselen sier fra når den måler mot en dato fiksen selv har satt inn,
 *  - en kapret sak kvitteres ut per saksnummer, ikke ved å slå av selen for hele kallet.
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
    fun `skarp angre uten saksnummer avvises, og krever bekreftAlle for å slette alt`() {
        // Den mest destruktive stien: uten scope treffer DELETE alle patch-rader i basen, også
        // fikser fra tidligere kjøringer — og hver slettet rad gjeninnfører 8174-krasjen.
        val gammelFiks = seedDefektBehandling("MEL-930", "NY_VURDERING", "2024-01-15 10:00:00")
        val nyFiks = seedDefektBehandling("MEL-931", "NY_VURDERING", "2024-06-15 10:00:00")
        kall(fiksUrl, """{"saksnummer":["MEL-930","MEL-931"],"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderInnsatt").value(2))

        // Glemt scope skal ikke slette begge
        kall(angreUrl, """{"skarp":true}""")
            .andExpect(status().isBadRequest)
        patchedeIder() shouldContainExactly listOf(gammelFiks, nyFiks)

        // Preview uten scope er derimot ufarlig, og viser hele omfanget
        kall(angreUrl, """{}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderFunnet").value(2))
            .andExpect(jsonPath("$.antallSlettet").value(0))
        patchedeIder() shouldContainExactly listOf(gammelFiks, nyFiks)

        // Nødbryteren finnes fortsatt, men må kvitteres ut
        kall(angreUrl, """{"skarp":true,"bekreftAlle":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallSlettet").value(2))
        antallMetadata() shouldBe 0
    }

    @Test
    fun `angre uten body er en preview, ikke en sletting`() {
        seedDefektBehandling("MEL-932", "NY_VURDERING", "2024-01-15 10:00:00")
        kall(fiksUrl, """{"saksnummer":["MEL-932"],"skarp":true}""").andExpect(status().isOk)

        mockMvc.perform(
            post(angreUrl)
                .header(AdminControllerApiKeyIT.API_KEY_HEADER, AdminControllerApiKeyIT.GYLDIG_API_NOKKEL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken()}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.skarp").value(false))
            .andExpect(jsonPath("$.antallSlettet").value(0))

        antallMetadata() shouldBe 1
    }

    @Test
    fun `patch-rad med tømt endret_av kan ikke rulles tilbake, og telles i antallEndretEtterpaa`() {
        // endret_av er nullbar, og i Oracle er både = og <> UNKNOWN mot NULL. Uten NULL-grenen i
        // ENDRET_ETTERPAA_SQL faller raden ut av BÅDE angre-kandidatene og tellingen, og svaret
        // ser ut som «ingenting å angre» i stedet for «én rad kunne ikke rulles tilbake».
        val rad = seedDefektBehandling("MEL-933", "NY_VURDERING", "2024-01-15 10:00:00")
        kall(fiksUrl, """{"saksnummer":["MEL-933"],"skarp":true}""").andExpect(status().isOk)
        jdbcTemplate.update("UPDATE vedtak_metadata SET endret_av = NULL WHERE behandlingsresultat_id = ?", rad)

        kall(angreUrl, """{"saksnummer":["MEL-933"],"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderFunnet").value(0))
            .andExpect(jsonPath("$.antallSlettet").value(0))
            .andExpect(jsonPath("$.antallEndretEtterpaa").value(1))

        antallMetadata() shouldBe 1
    }

    @Test
    fun `en tidligere patchet rad rapporteres som proxy, ikke som ekte sammenligningsgrunnlag`() {
        // Runde 1 patcher saken. Runde 2 måler mot den raden — som er vår egen proxy-dato, ikke et
        // vedtak. Sammenligningen er da proxy mot proxy, og «patchen vinner ikke» er ikke et
        // frikjenn. Operatøren skal se det på nyesteFoerErPatchet og på tom ekteDatoer.
        seedDefektBehandling("MEL-934", "NY_VURDERING", "2026-01-01 10:00:00")
        kall(fiksUrl, """{"saksnummer":["MEL-934"],"skarp":true}""").andExpect(status().isOk)

        seedDefektBehandling("MEL-934", "NY_VURDERING", "2025-06-01 10:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-934"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspaavirkning[0].patchenVinnerNyeste").value(false))
            .andExpect(jsonPath("$.sorteringspaavirkning[0].nyesteFoerErPatchet").value(true))
            .andExpect(jsonPath("$.sorteringspaavirkning[0].nyesteFoerDato").value("2026-01-01 10:00:00"))
            // ekteDatoer skal kun inneholde datoer som faktisk stammer fra et vedtak
            .andExpect(jsonPath("$.sorteringspaavirkning[0].ekteDatoer").isEmpty)
    }

    @Test
    fun `ekte vedtaksdato skilles fra patchet når begge finnes`() {
        val ekte = seedIntaktBehandling("MEL-935", "FØRSTEGANG", "2023-01-01 10:00:00")
        seedDefektBehandling("MEL-935", "NY_VURDERING", "2026-01-01 10:00:00")
        kall(fiksUrl, """{"saksnummer":["MEL-935"],"skarp":true,"tillatSorteringsendring":["MEL-935"]}""")
            .andExpect(status().isOk)

        seedDefektBehandling("MEL-935", "NY_VURDERING", "2025-06-01 10:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-935"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspaavirkning[0].nyesteFoerErPatchet").value(true))
            .andExpect(jsonPath("$.sorteringspaavirkning[0].ekteDatoer[0]").value("2023-01-01 10:00:00"))
            .andExpect(jsonPath("$.sorteringspaavirkning[0].ekteDatoer.length()").value(1))

        vedtaksdato(ekte) shouldBe "2023-01-01 10:00:00"
    }

    @Test
    fun `én kapret sak blokkerer ikke de øvrige når den kvitteres ut per saksnummer`() {
        // Formen prod-dataene har: én av flere saker i samme kall kaprer nyeste-plassen. Et av-på-flagg
        // ville tvunget operatøren til å slå av selen for alle tre.
        seedIntaktBehandling("MEL-936", "FØRSTEGANG", "2024-08-16 09:51:06")
        seedDefektBehandling("MEL-936", "NY_VURDERING", "2024-08-16 09:54:28")   // kaprer
        seedIntaktBehandling("MEL-937", "FØRSTEGANG", "2025-01-06 10:15:04")
        seedDefektBehandling("MEL-937", "NY_VURDERING", "2024-10-23 09:02:15")   // kaprer ikke

        kall(fiksUrl, """{"saksnummer":["MEL-936","MEL-937"],"skarp":true}""")
            .andExpect(status().isBadRequest)
        patchedeIder().shouldBeEmpty()

        kall(fiksUrl, """{"saksnummer":["MEL-936","MEL-937"],"skarp":true,"tillatSorteringsendring":["MEL-936"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderInnsatt").value(2))
    }

    @Test
    fun `kvittering for feil sak hjelper ikke — selen gjelder fortsatt den som kaprer`() {
        seedIntaktBehandling("MEL-938", "FØRSTEGANG", "2024-08-16 09:51:06")
        seedDefektBehandling("MEL-938", "NY_VURDERING", "2024-08-16 09:54:28")

        kall(fiksUrl, """{"saksnummer":["MEL-938"],"skarp":true,"tillatSorteringsendring":["MEL-999"]}""")
            .andExpect(status().isBadRequest)

        antallMetadata() shouldBe 1
    }

    @Test
    fun `saksnummer uten kandidater listes, slik at en skrivefeil ikke forsvinner i tallene`() {
        seedDefektBehandling("MEL-939", "NY_VURDERING", "2024-01-15 10:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-939","MEL-940"],"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderInnsatt").value(1))
            .andExpect(jsonPath("$.saksnummerUtenKandidater[0]").value("MEL-940"))
            .andExpect(jsonPath("$.saksnummerUtenKandidater.length()").value(1))
    }

    @Test
    fun `endepunktene krever både admin-API-nøkkel og bearer token`() {
        // Endepunktet skriver rett i vedtak_metadata i prod, så transporten pinnes her og ikke bare
        // i AdminControllerApiKeyIT — den går på GET-endepunkter, og disse to er POST.
        listOf(fiksUrl, angreUrl).forEach { url ->
            mockMvc.perform(
                post(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken()}")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content("""{"saksnummer":["MEL-950"]}""")
            ).andExpect(status().isForbidden)

            mockMvc.perform(
                post(url)
                    .header(AdminControllerApiKeyIT.API_KEY_HEADER, AdminControllerApiKeyIT.GYLDIG_API_NOKKEL)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .content("""{"saksnummer":["MEL-950"]}""")
            ).andExpect(status().isUnauthorized)
        }

        antallMetadata() shouldBe 0
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
    fun `sak uten en eneste ekte vedtaksdato skilles ut som eget tilfelle`() {
        // Ikke det farlige tilfellet, men det tryggeste: etter patchen kommer alle datoene i saken
        // fra samme klokke, så den interne rekkefølgen er konsistent. Eget felt, ikke «patchen vinner».
        seedDefektBehandling("MEL-912", "NY_VURDERING", "2024-05-10 12:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-912"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspaavirkning[0].nyesteFoerId").doesNotExist())
            .andExpect(jsonPath("$.sorteringspaavirkning[0].ingenSammenligningsgrunnlag").value(true))
            .andExpect(jsonPath("$.sorteringspaavirkning[0].patchenVinnerNyeste").value(false))
            .andExpect(jsonPath("$.sorteringspaavirkning[0].ekteDatoer").isEmpty)
    }

    @Test
    fun `preview mot formen på de faktiske prod-sakene i MELOSYS-8174`() {
        // Tidsstemplene er hentet fra prod (vedtaksdato.csv, uttrekk 2026-08-24). Poenget er ikke
        // at akkurat disse sakene finnes, men at rapporten svarer riktig på den formen dataene har:
        // to saker der patchen legger seg midt i sorteringen, og én der den kaprer nyeste-plassen.
        seedIntaktBehandling("MEL-448193", "FØRSTEGANG", "2024-04-30 15:33:55")
        seedIntaktBehandling("MEL-448193", "NY_VURDERING", "2025-01-06 10:15:04")
        seedDefektBehandling("MEL-448193", "NY_VURDERING", "2024-10-23 09:02:15")

        seedIntaktBehandling("MEL-545776", "FØRSTEGANG", "2024-08-16 09:51:06")
        seedDefektBehandling("MEL-545776", "NY_VURDERING", "2024-08-16 09:54:28")

        seedIntaktBehandling("MEL-632908", "FØRSTEGANG", "2024-11-12 10:51:45")
        seedIntaktBehandling("MEL-632908", "NY_VURDERING", "2025-06-25 09:56:12")
        seedDefektBehandling("MEL-632908", "NY_VURDERING", "2024-11-12 10:58:02")

        kall(fiksUrl, """{"saksnummer":["MEL-448193","MEL-545776","MEL-632908"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderFunnet").value(3))
            // Patchen havner mellom to ekte vedtak — nyeste-plassen står
            .andExpect(jsonPath("$.sorteringspaavirkning[0].saksnummer").value("MEL-448193"))
            .andExpect(jsonPath("$.sorteringspaavirkning[0].patchenVinnerNyeste").value(false))
            .andExpect(jsonPath("$.sorteringspaavirkning[0].nyesteFoerDato").value("2025-01-06 10:15:04"))
            // Eneste sak der patchen kaprer nyeste-plassen: NY_VURDERING drøye tre minutter
            // etter førstegangsvedtaket, så her avgjør ekte vedtaksdato utfallet
            .andExpect(jsonPath("$.sorteringspaavirkning[1].saksnummer").value("MEL-545776"))
            .andExpect(jsonPath("$.sorteringspaavirkning[1].patchenVinnerNyeste").value(true))
            .andExpect(jsonPath("$.sorteringspaavirkning[1].nyesteFoerDato").value("2024-08-16 09:51:06"))
            .andExpect(jsonPath("$.sorteringspaavirkning[1].nyestePatchetDato").value("2024-08-16 09:54:28"))
            .andExpect(jsonPath("$.sorteringspaavirkning[2].saksnummer").value("MEL-632908"))
            .andExpect(jsonPath("$.sorteringspaavirkning[2].patchenVinnerNyeste").value(false))

        antallMetadata() shouldBe 5
    }

    @Test
    fun `rad uten vedtaksdato velter ikke rapporten, og teller ikke som nyeste`() {
        // vedtak_dato er nullbar, og Oracle sorterer NULL først i DESC. Uten NULLS LAST ville en
        // udatert rad kapret nyeste-plassen — og castet til String hadde gitt 500.
        seedBehandlingUtenVedtaksdato("MEL-920", "FØRSTEGANG", "2024-01-01 08:00:00")
        val ekte = seedIntaktBehandling("MEL-920", "NY_VURDERING", "2025-05-05 10:00:00")
        seedDefektBehandling("MEL-920", "NY_VURDERING", "2024-06-01 12:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-920"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspaavirkning[0].nyesteFoerId").value(ekte))
            .andExpect(jsonPath("$.sorteringspaavirkning[0].nyesteFoerDato").value("2025-05-05 10:00:00"))
            .andExpect(jsonPath("$.sorteringspaavirkning[0].patchenVinnerNyeste").value(false))
            .andExpect(jsonPath("$.sorteringspaavirkning[0].ingenSammenligningsgrunnlag").value(false))
    }

    @Test
    fun `et halvt sekund teller — sammenligningen har mikrosekunder`() {
        seedIntaktBehandling("MEL-921", "FØRSTEGANG", "2024-08-16 09:51:06")
        seedDefektBehandling("MEL-921", "NY_VURDERING", "2024-08-16 09:51:06.5")

        kall(fiksUrl, """{"saksnummer":["MEL-921"]}""")
            .andExpect(status().isOk)
            // Samme sekund, men patchen er 500 ms nyere og ville tatt plassen i den ekte sorteringen
            .andExpect(jsonPath("$.sorteringspaavirkning[0].patchenVinnerNyeste").value(true))
    }

    @Test
    fun `patch et halvt sekund eldre enn ekte vedtak flagges ikke`() {
        // Motprøven til testen over: uten mikrosekunder ville disse to formatert likt, og den
        // konservative >=-regelen ville flagget en sak som i virkeligheten ikke rører sorteringen.
        seedIntaktBehandling("MEL-926", "FØRSTEGANG", "2024-08-16 09:51:06.9")
        seedDefektBehandling("MEL-926", "NY_VURDERING", "2024-08-16 09:51:06.1")

        kall(fiksUrl, """{"saksnummer":["MEL-926"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspaavirkning[0].patchenVinnerNyeste").value(false))
    }

    @Test
    fun `eksakt likt tidsstempel flagges, fordi utfallet da er vilkårlig`() {
        seedIntaktBehandling("MEL-922", "FØRSTEGANG", "2024-08-16 09:51:06")
        seedDefektBehandling("MEL-922", "NY_VURDERING", "2024-08-16 09:51:06")

        kall(fiksUrl, """{"saksnummer":["MEL-922"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspaavirkning[0].patchenVinnerNyeste").value(true))
    }

    @Test
    fun `flere defekte rader i samme sak sammenlignes med den nyeste av dem`() {
        seedIntaktBehandling("MEL-923", "FØRSTEGANG", "2024-03-01 10:00:00")
        seedDefektBehandling("MEL-923", "NY_VURDERING", "2024-02-01 10:00:00")
        val nyesteKandidat = seedDefektBehandling("MEL-923", "NY_VURDERING", "2024-09-01 10:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-923"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderFunnet").value(2))
            .andExpect(jsonPath("$.sorteringspaavirkning[0].nyestePatchetId").value(nyesteKandidat))
            .andExpect(jsonPath("$.sorteringspaavirkning[0].patchenVinnerNyeste").value(true))
    }

    @Test
    fun `åpen behandling med vedtaksmetadata teller ikke som sammenligningsgrunnlag`() {
        // ÅrsavregningService ser kun avsluttede behandlinger, så en åpen rad skal ikke være
        // klokka vi måler mot — selv om den har vedtaksmetadata.
        seedIntaktBehandling("MEL-924", "NY_VURDERING", "2025-12-01 10:00:00", status = "UNDER_BEHANDLING")
        seedDefektBehandling("MEL-924", "NY_VURDERING", "2024-06-01 12:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-924"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspaavirkning[0].ingenSammenligningsgrunnlag").value(true))
            .andExpect(jsonPath("$.sorteringspaavirkning[0].nyesteFoerId").doesNotExist())
    }

    @Test
    fun `skarp avvises når patchen tar nyeste-plassen, og slipper gjennom når det kvitteres ut`() {
        seedIntaktBehandling("MEL-925", "FØRSTEGANG", "2024-08-16 09:51:06")
        seedDefektBehandling("MEL-925", "NY_VURDERING", "2024-08-16 09:54:28")

        kall(fiksUrl, """{"saksnummer":["MEL-925"],"skarp":true}""")
            .andExpect(status().isBadRequest)
        antallMetadata() shouldBe 1

        kall(fiksUrl, """{"saksnummer":["MEL-925"],"skarp":true,"tillatSorteringsendring":["MEL-925"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderInnsatt").value(1))
        antallMetadata() shouldBe 2
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
    private fun seedIntaktBehandling(
        saksnummer: String,
        behType: String,
        vedtaksdato: String,
        status: String = "AVSLUTTET",
        brEndretDato: String = vedtaksdato,
    ): Long {
        val behandlingsresultatId = seedDefektBehandling(saksnummer, behType, brEndretDato, status)
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

    /** Vedtaksmetadata finnes, men uten dato — `vedtak_dato` er nullbar i skjemaet. */
    private fun seedBehandlingUtenVedtaksdato(saksnummer: String, behType: String, brEndretDato: String): Long {
        val behandlingsresultatId = seedDefektBehandling(saksnummer, behType, brEndretDato)
        jdbcTemplate.update(
            """INSERT INTO vedtak_metadata (behandlingsresultat_id, vedtak_dato, vedtak_type,
               registrert_dato, endret_dato, registrert_av, endret_av)
               VALUES (?, NULL, 'FØRSTEGANGSVEDTAK', SYSTIMESTAMP, SYSTIMESTAMP, 'IT', 'IT')""",
            behandlingsresultatId
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
