package no.nav.melosys.itest

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.mockk.every
import io.mockk.mockk
import no.nav.melosys.Application
import no.nav.melosys.ProsessinstansTestManager
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.manglendebetaling.Betalingsstatus
import no.nav.melosys.domain.manglendebetaling.ManglendeFakturabetalingMelding
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflyt.steg.behandling.OpprettManglendeInnbetalingBehandling
import no.nav.melosys.saksflyt.steg.brev.SendManglendeInnbetalingVarselBrev
import no.nav.melosys.saksflyt.steg.oppgave.OpprettOppgave
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.fakturering.FakturaserieSakDto
import no.nav.melosys.service.avgift.fakturering.ManglendeInnbetalingProsessDto
import no.nav.melosys.tjenester.gui.config.AdminTilgangInterceptor.Companion.MANGLER_DRIFTSGRUPPE
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@ActiveProfiles("test")
@SpringBootTest(classes = [Application::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
@EnableMockOAuth2Server
@AutoConfigureMockMvc
@Import(ManglendeInnbetalingAdminControllerIT.TestConfig::class)
class ManglendeInnbetalingAdminControllerIT(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val mockOAuth2Server: MockOAuth2Server,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val jdbcTemplate: JdbcTemplate,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingsresultatRepository: BehandlingsresultatRepository,
    @Autowired private val prosessinstansService: ProsessinstansService,
    @Autowired private val prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val prosessinstansTestManager: ProsessinstansTestManager,
) : OracleTestContainerBase() {

    @Test
    fun `begge endepunktene ligger under admin og bak AdminTilgangInterceptor`() {
        hentSaker(listOf(SERIE_A), token = null).andExpect(status().isUnauthorized)
        hentProsesser(token = null).andExpect(status().isUnauthorized)

        // Gyldig token uten driftsgruppe slipper gjennom @Protected, så 403 viser at interceptoren for /admin/** gjelder
        listOf(hentSaker(listOf(SERIE_A), personToken()), hentProsesser(personToken())).forEach {
            it.andExpect(status().isForbidden).andReturn().response.contentAsString shouldBe MANGLER_DRIFTSGRUPPE
        }
    }

    @Test
    fun `saker gir én rad per unik referanse i samme rekkefølge, og nyeste behandlingsresultat vinner`() {
        val førstLagret = lagBehandling(lagFagsak("MEL-8372-1", Saksstatuser.ANNULLERT), SERIE_A)
        val sistLagret = lagBehandling(lagFagsak("MEL-8372-2", Saksstatuser.OPPRETTET), SERIE_A)
        lagBehandling(lagFagsak("MEL-8372-3", Saksstatuser.OPPHØRT), SERIE_B)
        // Den sist lagrede gjøres eldst, så testen ikke kan bestå på lagringsrekkefølgen
        settRegistrert(førstLagret, LocalDateTime.of(2026, 1, 2, 0, 0))
        settRegistrert(sistLagret, LocalDateTime.of(2026, 1, 1, 0, 0))

        val svar = hentSaker(listOf(SERIE_B, "UKJENT", SERIE_A, SERIE_B), maskinToken())
            .andExpect(status().isOk).json()

        objectMapper.treeToValue(svar["saker"], Array<FakturaserieSakDto>::class.java).toList().shouldContainExactly(
            FakturaserieSakDto(SERIE_B, "MEL-8372-3", "OPPHØRT", 1),
            FakturaserieSakDto("UKJENT", null, null, 0),
            FakturaserieSakDto(SERIE_A, "MEL-8372-1", "ANNULLERT", 2),
        )
    }

    @Test
    fun `saker gir 400 ved tom liste og ved mer enn 1000 unike referanser`() {
        hentSaker(emptyList(), maskinToken()).andExpect(status().isBadRequest)
        hentSaker((0..1000).map { "REF$it" }, maskinToken()).andExpect(status().isBadRequest)
        hentSaker((0 until 1000).map { "REF$it" }, maskinToken()).andExpect(status().isOk)
        hentSaker(listOf(null), maskinToken()).andExpect(status().isBadRequest)
        hentSaker(listOf(SERIE_A, null), maskinToken()).andExpect(status().isBadRequest)
    }

    @Test
    fun `prosesser tolker låsreferansen og prosessdataene og tar bare med de to prosesstypene`() {
        val behandling = lagBehandling(lagFagsak("MEL-8372-4", Saksstatuser.OPPRETTET), SERIE_A)
        behandlingForNyBehandling = behandling
        prosessinstansTestManager.executeAndWait(
            mapOf(
                ProsessType.OPPRETT_NY_BEHANDLING_MANGLENDE_INNBETALING to 1,
                ProsessType.MANGLENDE_INNBETALING_VARSELBREV to 1
            )
        ) {
            prosessinstansService.opprettProsessManglendeInnbetalingBehandling(melding(SERIE_A, "23004119"))
            prosessinstansService.opprettProsessManglendeInnbetalingVarselBrev(behandling, melding(SERIE_A, "23004120"))
        }
        // Lagres direkte så den ikke kjøres. Låsreferansen ser ut som en ubetalt-lås: bare typen skal skille den ut
        prosessinstansRepository.save(
            Prosessinstans.builder()
                .medType(ProsessType.OPPRETT_NY_BEHANDLING_AARSAVREGNING)
                .medStatus(ProsessStatus.FERDIG)
                .medLåsReferanse("UBETALT_${SERIE_B}_1")
                .build()
        )

        val svar = hentProsesser(maskinToken()).andExpect(status().isOk).json()["prosesser"]
        val prosesser = objectMapper.treeToValue(svar, Array<ManglendeInnbetalingProsessDto>::class.java)

        prosesser.map { listOf(it.prosesstype, it.fakturanummer, it.fakturaserieReferanse, it.saksnummer, it.status) }
            .sortedBy { it.first() } shouldContainExactly listOf(
            listOf("MANGLENDE_INNBETALING_VARSELBREV", "23004120", null, "MEL-8372-4", "FERDIG"),
            listOf("OPPRETT_NY_BEHANDLING_MANGLENDE_INNBETALING", "23004119", SERIE_A, "MEL-8372-4", "FERDIG"),
        )
        svar.forEach { it["registrert"].asString() shouldMatch Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?Z""") }
    }

    @Test
    fun `prosesser leser prosessdataene når låsreferansen ikke kan brukes, og ødelagte data gir bare null-felt for den raden`() {
        val sommertid = LocalDateTime.of(2026, 7, 1, 10, 0)
        lagreProsess(NY_BEHANDLING, lås = null, fakturanummer = "23004121", serie = SERIE_A, registrert = sommertid)
        // Fakturanummer som ikke er rene tall gir en låsreferanse som ikke kan tolkes
        lagreProsess(NY_BEHANDLING, lås = "UBETALT_${SERIE_B}_F-77", fakturanummer = "F-77", serie = SERIE_B)
        lagreProsess(VARSELBREV, lås = null, fakturanummer = "23004122", serie = null)
        ødeleggData(lagreProsess(NY_BEHANDLING, lås = null, fakturanummer = "23004123", serie = SERIE_A))
        ødeleggData(lagreProsess(VARSELBREV, lås = null, fakturanummer = "23004124", serie = null))

        val svar = hentProsesser(maskinToken()).andExpect(status().isOk).json()["prosesser"]
        val prosesser = objectMapper.treeToValue(svar, Array<ManglendeInnbetalingProsessDto>::class.java)

        prosesser.map { listOf(it.prosesstype, it.fakturanummer, it.fakturaserieReferanse) } shouldContainExactlyInAnyOrder listOf(
            listOf(NY_BEHANDLING.name, "23004121", SERIE_A),
            listOf(NY_BEHANDLING.name, "F-77", SERIE_B),
            listOf(VARSELBREV.name, "23004122", null),
            listOf(NY_BEHANDLING.name, null, null),
            listOf(VARSELBREV.name, null, null),
        )
        // registrert_dato er veggtid i JVM-sonen, og skal ut som det tilsvarende UTC-tidspunktet
        prosesser.single { it.fakturanummer == "23004121" }.registrert shouldBe
            sommertid.atZone(ZoneId.systemDefault()).toInstant()
        svar.single { it["fakturanummer"]?.asString() == "23004121" }["registrert"].asString() shouldBe
            sommertid.atZone(ZoneId.systemDefault()).toInstant().toString()
    }

    private fun lagFagsak(saksnummer: String, status: Saksstatuser): Fagsak = fagsakRepository.save(
        Fagsak.forTest {
            this.saksnummer = saksnummer
            type = Sakstyper.EU_EOS
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG
            this.status = status
            medBruker()
        }
    )

    private fun lagBehandling(fagsak: Fagsak, fakturaserieReferanse: String): Behandling =
        behandlingsresultatRepository.saveAndFlush(
            Behandlingsresultat.forTest {
                this.fakturaserieReferanse = fakturaserieReferanse
                behandling {
                    medFagsak(fagsak)
                    type = Behandlingstyper.FØRSTEGANG
                    status = Behandlingsstatus.AVSLUTTET
                    tema = Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY
                }
            }
        ).hentBehandling()

    private fun settRegistrert(behandling: Behandling, tidspunkt: LocalDateTime) {
        jdbcTemplate.update(
            "UPDATE behandlingsresultat SET registrert_dato = ? WHERE behandling_id = ?",
            Timestamp.valueOf(tidspunkt), behandling.id
        )
    }

    private fun lagreProsess(
        type: ProsessType,
        lås: String?,
        fakturanummer: String,
        serie: String?,
        registrert: LocalDateTime = LocalDateTime.now()
    ): UUID = prosessinstansRepository.save(
        Prosessinstans.builder()
            .medType(type)
            .medStatus(ProsessStatus.FERDIG)
            .medLåsReferanse(lås)
            .medRegistrertDato(registrert)
            .medData(ProsessDataKey.FAKTURANUMMER, fakturanummer)
            .medData(ProsessDataKey.FAKTURASERIE_REFERANSE, serie)
            .build()
    ).id!!

    // Ugyldig \u-escape får Properties.load til å kaste, slik PropertiesConverter også ville gjort
    private fun ødeleggData(id: UUID) {
        jdbcTemplate.update(
            "UPDATE prosessinstans SET data = ? WHERE uuid = HEXTORAW(?)",
            "fakturanummer=\\uZZZZ", id.toString().replace("-", "")
        ) shouldBe 1
    }

    private fun melding(serie: String, fakturanummer: String) =
        ManglendeFakturabetalingMelding(serie, Betalingsstatus.IKKE_BETALT, LocalDate.of(2026, 8, 14), fakturanummer)

    private fun hentSaker(referanser: List<String?>, token: String?): ResultActions = mockMvc.perform(
        post("/admin/fakturaserier/saker")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(mapOf("fakturaserieReferanser" to referanser)))
            .apply { token?.let { header(HttpHeaders.AUTHORIZATION, "Bearer $it") } }
    )

    private fun hentProsesser(token: String?): ResultActions = mockMvc.perform(
        get("/admin/manglende-innbetaling/prosesser")
            .apply { token?.let { header(HttpHeaders.AUTHORIZATION, "Bearer $it") } }
    )

    private fun ResultActions.json(): JsonNode = objectMapper.readTree(andReturn().response.contentAsString)

    private fun maskinToken() = token(
        mapOf(
            "oid" to "test-app-oid",
            "azp_name" to "test-cluster:teammelosys:melosys-console",
            "idtyp" to "app",
            "roles" to listOf("access_as_application")
        )
    )

    private fun personToken() = token(mapOf("oid" to "test-oid", "NAVident" to "test123", "groups" to emptyList<String>()))

    // Samme klient-ID som Melosys-admin.console-klient-id i application-test.yml
    private fun token(claims: Map<String, Any>): String = mockOAuth2Server.issueToken(
        "issuer1",
        "test-azp",
        DefaultOAuth2TokenCallback(issuerId = "issuer1", subject = "test", audience = listOf("dumbdumb"), claims = claims)
    ).serialize()

    @TestConfiguration
    class TestConfig {
        @Bean
        @Primary
        fun opprettManglendeInnbetalingBehandlingForTest(): OpprettManglendeInnbetalingBehandling =
            mockk<OpprettManglendeInnbetalingBehandling>().apply {
                every { inngangsSteg() } returns ProsessSteg.OPPRETT_MANGLENDE_INNBETALING_BEHANDLING
                every { utfør(any()) } answers { firstArg<Prosessinstans>().behandling = behandlingForNyBehandling }
            }

        @Bean
        @Primary
        fun opprettOppgaveForTest(): OpprettOppgave = mockk<OpprettOppgave>().apply {
            every { inngangsSteg() } returns ProsessSteg.OPPRETT_OPPGAVE
            every { utfør(any()) } returns Unit
        }

        @Bean
        @Primary
        fun sendManglendeInnbetalingVarselBrevForTest(): SendManglendeInnbetalingVarselBrev =
            mockk<SendManglendeInnbetalingVarselBrev>().apply {
                every { inngangsSteg() } returns ProsessSteg.SEND_MANGLENDE_INNBETALING_VARSELBREV
                every { utfør(any()) } returns Unit
            }
    }

    companion object {
        private const val SERIE_A = "01HHFM03YMHHQAVZ4SQF9Y29E4"
        private const val SERIE_B = "01HHFM03YMHHQAVZ4SQF9Y29E5"
        private val NY_BEHANDLING = ProsessType.OPPRETT_NY_BEHANDLING_MANGLENDE_INNBETALING
        private val VARSELBREV = ProsessType.MANGLENDE_INNBETALING_VARSELBREV

        // Settes på prosessen av steget som ellers finner behandlingen via seriereferansen
        @Volatile
        private var behandlingForNyBehandling: Behandling? = null
    }
}
