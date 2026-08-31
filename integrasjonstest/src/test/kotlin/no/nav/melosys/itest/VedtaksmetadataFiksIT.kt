package no.nav.melosys.itest

import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldBeEmpty
import org.hamcrest.Matchers.containsString
import io.kotest.matchers.collections.shouldContainExactly
import no.nav.melosys.Application
import no.nav.melosys.service.avgift.aarsavregning.skattepliktig.VedtaksmetadataFiksService.Companion.PATCH_MARKØR
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
    fun `skarp setter inn radene med markør og tilnærmet dato, og er idempotent`() {
        val foerstegang = seedDefektBehandling("MEL-902", "FØRSTEGANG", "2023-11-23 08:00:00")
        val nyVurdering = seedDefektBehandling("MEL-902", "NY_VURDERING", "2024-10-23 09:54:00")

        kall(fiksUrl, """{"saksnummer":["MEL-902"],"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderInnsatt").value(2))
            .andExpect(jsonPath("$.avvik").value(false))
            .andExpect(jsonPath("$.utenMetadataPerSak").isEmpty)

        vedtakstype(foerstegang) shouldBe "FØRSTEGANGSVEDTAK"
        vedtakstype(nyVurdering) shouldBe "ENDRINGSVEDTAK"
        // vedtak_dato er den tilnærmede datoen behandlingsresultat.endret_dato, ikke tidspunktet fiksen kjørte
        vedtaksdato(nyVurdering) shouldBe "2024-10-23 09:54:00"
        registrertAv(nyVurdering) shouldBe PATCH_MARKØR

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
        val endretEtterpå = seedDefektBehandling("MEL-907", "FØRSTEGANG", "2023-11-23 08:00:00")
        kall(fiksUrl, """{"saksnummer":["MEL-907"],"skarp":true}""").andExpect(status().isOk)

        // Saksbehandler skriver ekte vedtaksdato: registrert_av beholder markøren, endret_av flyttes
        jdbcTemplate.update(
            "UPDATE vedtak_metadata SET vedtak_dato = SYSTIMESTAMP, endret_av = 'Z994321' WHERE behandlingsresultat_id = ?",
            endretEtterpå
        )

        kall(angreUrl, """{"saksnummer":["MEL-907"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderFunnet").value(1))
            .andExpect(jsonPath("$.antallSlettet").value(0))
            .andExpect(jsonPath("$.antallSomIkkeKanAngres").value(1))

        kall(angreUrl, """{"saksnummer":["MEL-907"],"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallSlettet").value(1))
            .andExpect(jsonPath("$.antallSomIkkeKanAngres").value(1))

        patchedeIder() shouldContainExactly listOf(endretEtterpå)
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
        val gammelFiks = seedDefektBehandling("MEL-930", "NY_VURDERING", "2024-01-15 10:00:00")
        val nyFiks = seedDefektBehandling("MEL-931", "NY_VURDERING", "2024-06-15 10:00:00")
        kall(fiksUrl, """{"saksnummer":["MEL-930","MEL-931"],"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderInnsatt").value(2))

        kall(angreUrl, """{"skarp":true}""")
            .andExpect(status().isBadRequest)
        patchedeIder() shouldContainExactly listOf(gammelFiks, nyFiks)

        kall(angreUrl, """{}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderFunnet").value(2))
            .andExpect(jsonPath("$.antallSlettet").value(0))
        patchedeIder() shouldContainExactly listOf(gammelFiks, nyFiks)

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
    fun `patch-rad med tømt endret_av kan ikke rulles tilbake, og telles i antallSomIkkeKanAngres`() {
        // Både = og <> er UNKNOWN mot NULL i Oracle — raden må ikke forsvinne fra begge tellingene
        val rad = seedDefektBehandling("MEL-933", "NY_VURDERING", "2024-01-15 10:00:00")
        kall(fiksUrl, """{"saksnummer":["MEL-933"],"skarp":true}""").andExpect(status().isOk)
        jdbcTemplate.update("UPDATE vedtak_metadata SET endret_av = NULL WHERE behandlingsresultat_id = ?", rad)

        kall(angreUrl, """{"saksnummer":["MEL-933"],"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderFunnet").value(0))
            .andExpect(jsonPath("$.antallSlettet").value(0))
            .andExpect(jsonPath("$.antallSomIkkeKanAngres").value(1))

        antallMetadata() shouldBe 1
    }

    @Test
    fun `en urørt patch-rad er ikke sammenligningsgrunnlag, og rapporteres for seg`() {
        seedDefektBehandling("MEL-934", "NY_VURDERING", "2026-01-01 10:00:00")
        kall(fiksUrl, """{"saksnummer":["MEL-934"],"skarp":true}""").andExpect(status().isOk)

        seedDefektBehandling("MEL-934", "NY_VURDERING", "2025-06-01 10:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-934"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspåvirkning[0].trengerGodkjenning").value(false))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].nyesteSammenlignbareId").doesNotExist())
            .andExpect(jsonPath("$.sorteringspåvirkning[0].ekteDatoer").isEmpty)
            .andExpect(jsonPath("$.sorteringspåvirkning[0].usikreDatoer").isEmpty)
            .andExpect(jsonPath("$.sorteringspåvirkning[0].patchedeDatoer[0]").value("2026-01-01 10:00:00"))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].patchenBlirNyesteIHeleSaken").value(false))
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
            .andExpect(jsonPath("$.sorteringspåvirkning[0].nyesteSammenlignbareDato").value("2023-01-01 10:00:00"))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].ekteDatoer[0]").value("2023-01-01 10:00:00"))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].ekteDatoer.length()").value(1))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].patchedeDatoer[0]").value("2026-01-01 10:00:00"))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].trengerGodkjenning").value(true))

        vedtaksdato(ekte) shouldBe "2023-01-01 10:00:00"
    }

    @Test
    fun `godkjenning av én sak blokkerer ikke de øvrige sakene i kallet`() {
        seedIntaktBehandling("MEL-936", "FØRSTEGANG", "2024-08-16 09:51:06")
        seedDefektBehandling("MEL-936", "NY_VURDERING", "2024-08-16 09:54:28")   // blir nyeste
        seedIntaktBehandling("MEL-937", "FØRSTEGANG", "2025-01-06 10:15:04")
        seedDefektBehandling("MEL-937", "NY_VURDERING", "2024-10-23 09:02:15")   // blir ikke nyeste

        kall(fiksUrl, """{"saksnummer":["MEL-936","MEL-937"],"skarp":true}""")
            .andExpect(status().isBadRequest)
        patchedeIder().shouldBeEmpty()

        kall(fiksUrl, """{"saksnummer":["MEL-936","MEL-937"],"skarp":true,"tillatSorteringsendring":["MEL-936"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderInnsatt").value(2))
    }

    @Test
    fun `godkjenning av feil sak hjelper ikke — kontrollen gjelder fortsatt saken som trenger den`() {
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
    fun `over tusen kandidater avvises kontrollert i stedet for å sprekke på Oracles IN-grense`() {
        seedDefekteBehandlingerIBulk("MEL-941", 1001)

        kall(fiksUrl, """{"saksnummer":["MEL-941"],"skarp":true,"maksAntallRader":2000}""")
            .andExpect(status().isBadRequest)
            // Pinner meldingen: maksAntallRader-kontrollen ville ellers gitt 400 for samme request
            .andExpect(jsonPath("$.feil").value(containsString("ORA-01795")))

        antallMetadata() shouldBe 0

        kall(fiksUrl, """{"saksnummer":["MEL-941"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderFunnet").value(1001))
    }

    @Test
    fun `ekte vedtaksdato skrevet oppå en patch-rad gjør den ekte igjen`() {
        val patchet = seedDefektBehandling("MEL-960", "NY_VURDERING", "2026-01-01 10:00:00")
        kall(fiksUrl, """{"saksnummer":["MEL-960"],"skarp":true}""").andExpect(status().isOk)

        kall(fiksUrl, """{"saksnummer":["MEL-960"]}""")
            .andExpect(jsonPath("$.sorteringspåvirkning").isEmpty)

        // Saksbehandler skriver ekte vedtaksdato: registrert_av beholder markøren, endret_av flyttes
        jdbcTemplate.update(
            """UPDATE vedtak_metadata SET vedtak_dato = TIMESTAMP '2023-05-05 08:00:00',
               endret_av = 'Z994321' WHERE behandlingsresultat_id = ?""",
            patchet
        )
        seedDefektBehandling("MEL-960", "NY_VURDERING", "2025-06-01 10:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-960"]}""")
            .andExpect(status().isOk)
            // Usikker, ikke ekte: vi vet ikke om det som ble skrevet var vedtaksdatoen
            .andExpect(jsonPath("$.sorteringspåvirkning[0].ekteDatoer").isEmpty)
            .andExpect(jsonPath("$.sorteringspåvirkning[0].usikreDatoer[0]").value("2023-05-05 08:00:00"))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].nyesteSammenlignbareDato").value("2023-05-05 08:00:00"))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].trengerGodkjenning").value(true))

        kall(fiksUrl, """{"saksnummer":["MEL-960"],"skarp":true}""")
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `sak der alle datoer stammer fra fiksen selv blokkeres ikke`() {
        seedDefektBehandling("MEL-961", "NY_VURDERING", "2024-01-01 10:00:00")
        kall(fiksUrl, """{"saksnummer":["MEL-961"],"skarp":true}""").andExpect(status().isOk)

        seedDefektBehandling("MEL-961", "NY_VURDERING", "2025-01-01 10:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-961"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspåvirkning[0].trengerGodkjenning").value(false))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].nyesteSammenlignbareId").doesNotExist())
            .andExpect(jsonPath("$.sorteringspåvirkning[0].ekteDatoer").isEmpty)
            .andExpect(jsonPath("$.sorteringspåvirkning[0].patchedeDatoer[0]").value("2024-01-01 10:00:00"))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].patchenBlirNyesteIHeleSaken").value(true))

        kall(fiksUrl, """{"saksnummer":["MEL-961"],"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderInnsatt").value(1))

        antallMetadata() shouldBe 2
    }

    @Test
    fun `godkjenning som ikke traff noen sak nevnes i avvisningen`() {
        seedIntaktBehandling("MEL-962", "FØRSTEGANG", "2024-08-16 09:51:06")
        seedDefektBehandling("MEL-962", "NY_VURDERING", "2024-08-16 09:54:28")

        // MEL-8888 er ikke prefiks av MEL-962 — containsString under må ikke kunne treffe saksnummeret
        kall(fiksUrl, """{"saksnummer":["MEL-962"],"skarp":true,"tillatSorteringsendring":["MEL-8888"]}""")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.feil").value(containsString("traff ingen sak som trengte godkjenning")))
            .andExpect(jsonPath("$.feil").value(containsString("MEL-8888")))

        antallMetadata() shouldBe 1
    }

    @Test
    fun `godkjenning med ugyldig saksnummerformat avvises på samme måte som saksnummer`() {
        seedDefektBehandling("MEL-980", "NY_VURDERING", "2024-01-15 10:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-980"],"skarp":true,"tillatSorteringsendring":["MEL-1' OR 1=1--"]}""")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.ugyldige[0]").value("MEL-1' OR 1=1--"))

        antallMetadata() shouldBe 0
    }

    @Test
    fun `forhåndsvisningen melder også fra om godkjenninger uten treff`() {
        seedDefektBehandling("MEL-981", "NY_VURDERING", "2024-01-15 10:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-981"],"tillatSorteringsendring":["MEL-8888"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.godkjenningerUtenTreff[0]").value("MEL-8888"))
            .andExpect(jsonPath("$.godkjenningerUtenTreff.length()").value(1))
    }

    @Test
    fun `de tre datokategoriene holdes fra hverandre i samme sak`() {
        // Den urørte patchen (2024) må være nyest av de tre, ellers skiller ikke testen riktig modell fra feil
        seedIntaktBehandling("MEL-982", "FØRSTEGANG", "2020-01-01 10:00:00")
        seedDefektBehandling("MEL-982", "NY_VURDERING", "2024-01-01 10:00:00")
        val skrevetTil = seedDefektBehandling("MEL-982", "NY_VURDERING", "2022-01-01 10:00:00")
        kall(fiksUrl, """{"saksnummer":["MEL-982"],"skarp":true,"tillatSorteringsendring":["MEL-982"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderInnsatt").value(2))

        jdbcTemplate.update(
            "UPDATE vedtak_metadata SET vedtak_klagefrist = SYSDATE, endret_av = 'Z994321' WHERE behandlingsresultat_id = ?",
            skrevetTil
        )
        seedDefektBehandling("MEL-982", "NY_VURDERING", "2023-01-01 10:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-982"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspåvirkning[0].ekteDatoer[0]").value("2020-01-01 10:00:00"))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].usikreDatoer[0]").value("2022-01-01 10:00:00"))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].patchedeDatoer[0]").value("2024-01-01 10:00:00"))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].nyesteSammenlignbareId").value(skrevetTil))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].nyesteSammenlignbareDato").value("2022-01-01 10:00:00"))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].trengerGodkjenning").value(true))
    }

    @Test
    fun `duplikate saksnummer innenfor taket avvises ikke, og ekkoes ikke tilbake`() {
        // 26 oppføringer, 13 unike — taket er 25
        seedDefektBehandling("MEL-990", "NY_VURDERING", "2024-01-15 10:00:00")
        val duplikater = (1..13).joinToString(",") { "\"MEL-99$it\",\"MEL-99$it\"" }

        kall(fiksUrl, """{"saksnummer":[$duplikater]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.saksnummer.length()").value(13))

        kall(fiksUrl, """{"saksnummer":["MEL-990","MEL-990"],"skarp":true}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderInnsatt").value(1))
            .andExpect(jsonPath("$.saksnummer.length()").value(1))
            .andExpect(jsonPath("$.saksnummerUtenKandidater").isEmpty)
    }

    @Test
    fun `endepunktene krever både admin-API-nøkkel og bearer token`() {
        // AdminControllerApiKeyIT dekker kun GET; disse er POST
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
    fun `preview varsler når den tilnærmede datoen blir nyeste i saken`() {
        val ekteNyeste = seedIntaktBehandling("MEL-910", "FØRSTEGANG", "2023-03-01 10:00:00")
        val defekt = seedDefektBehandling("MEL-910", "NY_VURDERING", "2024-05-10 12:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-910"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspåvirkning[0].saksnummer").value("MEL-910"))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].trengerGodkjenning").value(true))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].nyesteSammenlignbareId").value(ekteNyeste))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].nyesteSammenlignbareDato").value("2023-03-01 10:00:00"))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].nyesteKandidatId").value(defekt))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].nyesteKandidatDato").value("2024-05-10 12:00:00"))

        antallMetadata() shouldBe 1
    }

    @Test
    fun `preview melder fra når patchen ikke blir nyeste i saken`() {
        seedIntaktBehandling("MEL-911", "FØRSTEGANG", "2025-01-01 10:00:00")
        seedDefektBehandling("MEL-911", "NY_VURDERING", "2024-05-10 12:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-911"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspåvirkning[0].trengerGodkjenning").value(false))
    }

    @Test
    fun `sak uten en eneste ekte vedtaksdato skilles ut som eget tilfelle`() {
        seedDefektBehandling("MEL-912", "NY_VURDERING", "2024-05-10 12:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-912"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspåvirkning[0].nyesteSammenlignbareId").doesNotExist())
            .andExpect(jsonPath("$.sorteringspåvirkning[0].trengerGodkjenning").value(false))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].ekteDatoer").isEmpty)
            .andExpect(jsonPath("$.sorteringspåvirkning[0].antallUdaterteRader").value(0))
    }

    @Test
    fun `preview mot formen på de faktiske prod-sakene`() {
        // Tidsstempler fra prod-uttrekket 2026-08-24
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
            .andExpect(jsonPath("$.sorteringspåvirkning[0].saksnummer").value("MEL-448193"))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].trengerGodkjenning").value(false))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].nyesteSammenlignbareDato").value("2025-01-06 10:15:04"))
            .andExpect(jsonPath("$.sorteringspåvirkning[1].saksnummer").value("MEL-545776"))
            .andExpect(jsonPath("$.sorteringspåvirkning[1].trengerGodkjenning").value(true))
            .andExpect(jsonPath("$.sorteringspåvirkning[1].nyesteSammenlignbareDato").value("2024-08-16 09:51:06"))
            .andExpect(jsonPath("$.sorteringspåvirkning[1].nyesteKandidatDato").value("2024-08-16 09:54:28"))
            .andExpect(jsonPath("$.sorteringspåvirkning[2].saksnummer").value("MEL-632908"))
            .andExpect(jsonPath("$.sorteringspåvirkning[2].trengerGodkjenning").value(false))

        antallMetadata() shouldBe 5
    }

    @Test
    fun `rad uten vedtaksdato velter ikke rapporten, og teller ikke som nyeste`() {
        seedBehandlingUtenVedtaksdato("MEL-920", "FØRSTEGANG", "2024-01-01 08:00:00")
        val ekte = seedIntaktBehandling("MEL-920", "NY_VURDERING", "2025-05-05 10:00:00")
        seedDefektBehandling("MEL-920", "NY_VURDERING", "2024-06-01 12:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-920"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspåvirkning[0].nyesteSammenlignbareId").value(ekte))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].nyesteSammenlignbareDato").value("2025-05-05 10:00:00"))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].trengerGodkjenning").value(false))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].nyesteSammenlignbareId").exists())
    }

    @Test
    fun `et halvt sekund teller — sammenligningen har mikrosekunder`() {
        seedIntaktBehandling("MEL-921", "FØRSTEGANG", "2024-08-16 09:51:06")
        seedDefektBehandling("MEL-921", "NY_VURDERING", "2024-08-16 09:51:06.5")

        kall(fiksUrl, """{"saksnummer":["MEL-921"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspåvirkning[0].trengerGodkjenning").value(true))
    }

    @Test
    fun `patch et halvt sekund eldre enn ekte vedtak flagges ikke`() {
        seedIntaktBehandling("MEL-926", "FØRSTEGANG", "2024-08-16 09:51:06.9")
        seedDefektBehandling("MEL-926", "NY_VURDERING", "2024-08-16 09:51:06.1")

        kall(fiksUrl, """{"saksnummer":["MEL-926"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspåvirkning[0].trengerGodkjenning").value(false))
    }

    @Test
    fun `eksakt likt tidsstempel flagges, fordi utfallet da er vilkårlig`() {
        seedIntaktBehandling("MEL-922", "FØRSTEGANG", "2024-08-16 09:51:06")
        seedDefektBehandling("MEL-922", "NY_VURDERING", "2024-08-16 09:51:06")

        kall(fiksUrl, """{"saksnummer":["MEL-922"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspåvirkning[0].trengerGodkjenning").value(true))
    }

    @Test
    fun `flere defekte rader i samme sak sammenlignes med den nyeste av dem`() {
        seedIntaktBehandling("MEL-923", "FØRSTEGANG", "2024-03-01 10:00:00")
        seedDefektBehandling("MEL-923", "NY_VURDERING", "2024-02-01 10:00:00")
        val nyesteKandidat = seedDefektBehandling("MEL-923", "NY_VURDERING", "2024-09-01 10:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-923"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallRaderFunnet").value(2))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].nyesteKandidatId").value(nyesteKandidat))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].trengerGodkjenning").value(true))
    }

    @Test
    fun `åpen behandling med vedtaksmetadata teller ikke som sammenligningsgrunnlag`() {
        seedIntaktBehandling("MEL-924", "NY_VURDERING", "2025-12-01 10:00:00", status = "UNDER_BEHANDLING")
        seedDefektBehandling("MEL-924", "NY_VURDERING", "2024-06-01 12:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-924"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspåvirkning[0].nyesteSammenlignbareId").doesNotExist())
            .andExpect(jsonPath("$.sorteringspåvirkning[0].ekteDatoer").isEmpty)
            .andExpect(jsonPath("$.sorteringspåvirkning[0].antallUdaterteRader").value(0))
            .andExpect(jsonPath("$.sorteringspåvirkning[0].trengerGodkjenning").value(false))
    }

    @Test
    fun `rad med vedtaksmetadata uten dato skilles fra sak helt uten vedtaksmetadata`() {
        seedBehandlingUtenVedtaksdato("MEL-983", "FØRSTEGANG", "2024-01-01 08:00:00")
        seedDefektBehandling("MEL-983", "NY_VURDERING", "2024-06-01 12:00:00")

        kall(fiksUrl, """{"saksnummer":["MEL-983"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspåvirkning[0].nyesteSammenlignbareId").doesNotExist())
            .andExpect(jsonPath("$.sorteringspåvirkning[0].antallUdaterteRader").value(1))

        seedDefektBehandling("MEL-984", "NY_VURDERING", "2024-06-01 12:00:00")
        kall(fiksUrl, """{"saksnummer":["MEL-984"]}""")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sorteringspåvirkning[0].nyesteSammenlignbareId").doesNotExist())
            .andExpect(jsonPath("$.sorteringspåvirkning[0].antallUdaterteRader").value(0))
    }

    @Test
    fun `skarp avvises når den tilnærmede datoen blir nyeste, og slipper gjennom med godkjenning`() {
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

    /** Set-basert i stedet for rad-for-rad — tusen behandlinger enkeltvis ville dominert suitens kjøretid. */
    private fun seedDefekteBehandlingerIBulk(saksnummer: String, antall: Int) {
        jdbcTemplate.update(
            """MERGE INTO fagsak f USING (SELECT ? AS saksnummer FROM dual) k ON (f.saksnummer = k.saksnummer)
               WHEN NOT MATCHED THEN INSERT (saksnummer, fagsak_type, status, tema, registrert_dato, endret_dato, registrert_av, endret_av)
               VALUES (k.saksnummer, 'EU_EOS', 'LOVVALG_AVKLART', 'UNNTAK', SYSTIMESTAMP, SYSTIMESTAMP, 'IT', 'IT')""",
            saksnummer
        )
        jdbcTemplate.update(
            """INSERT INTO behandling (saksnummer, status, beh_type, beh_tema, behandlingsfrist,
               registrert_dato, endret_dato, registrert_av, endret_av)
               SELECT ?, 'AVSLUTTET', 'NY_VURDERING', 'REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING',
                      DATE '2024-02-14', SYSTIMESTAMP, SYSTIMESTAMP, 'IT', 'IT'
               FROM dual CONNECT BY LEVEL <= ?""",
            saksnummer, antall
        )
        jdbcTemplate.update(
            """INSERT INTO behandlingsresultat (behandling_id, resultat_type, behandlingsmaate,
               registrert_dato, endret_dato, registrert_av, endret_av)
               SELECT b.id, 'MEDLEM_I_FOLKETRYGDEN', 'MANUELT',
                      TIMESTAMP '2024-10-23 09:54:00', TIMESTAMP '2024-10-23 09:54:00', 'IT', 'IT'
               FROM behandling b WHERE b.saksnummer = ?""",
            saksnummer
        )
    }

    private fun antallMetadata(): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vedtak_metadata", Int::class.java)!!

    private fun patchedeIder(): List<Long> = jdbcTemplate.queryForList(
        "SELECT behandlingsresultat_id FROM vedtak_metadata WHERE registrert_av = ? ORDER BY behandlingsresultat_id",
        Long::class.java, PATCH_MARKØR
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
