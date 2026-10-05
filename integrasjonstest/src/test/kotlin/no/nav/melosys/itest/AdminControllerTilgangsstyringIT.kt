package no.nav.melosys.itest

import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.TokenRequest
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.Application
import no.nav.melosys.tjenester.gui.config.AdminTilgangInterceptor.Companion.MANGLER_DRIFTSGRUPPE
import no.nav.melosys.tjenester.gui.config.AdminTilgangInterceptor.Companion.UKJENT_KLIENT
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.token.OAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

/**
 * Tilgangsstyring for admin-endepunktene (MELOSYS-8271).
 *
 * - Alle kall må komme fra Console: tokenets `azp` må være Consoles klient-ID.
 * - Personkall krever i tillegg driftsgruppen i tokenets `groups`-claim.
 * - Maskinkall (`idtyp = app`) fra Console får tilgang til alle admin-endepunkter.
 * - Kall uten gyldig token avvises av `@Protected` med 401.
 * - Adminnøkkelen er fjernet. Nøkkelheaderen påvirker ikke svaret.
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
class AdminControllerTilgangsstyringIT(
    @Autowired var mockMvc: MockMvc,
    @Autowired var mockOAuth2Server: MockOAuth2Server,
    @Autowired @Qualifier("requestMappingHandlerMapping") var handlerMapping: RequestMappingHandlerMapping
) : OracleTestContainerBase() {

    companion object {
        // Samme verdier som Melosys-admin i application-test.yml
        const val CONSOLE_KLIENT_ID = "test-azp"
        const val DRIFTSGRUPPE_ID = "00000000-0000-0000-0000-000000000001"

        const val ANNEN_KLIENT_ID = "annen-klient-id"
        const val ANNEN_GRUPPE_ID = "00000000-0000-0000-0000-000000000002"

        // Den fjernede adminnøkkelen. Brukes bare for å vise at headeren ikke lenger har effekt.
        private const val API_KEY_HEADER = "X-MELOSYS-ADMIN-APIKEY"
    }

    // mock-oauth2-server overskriver azp i claims med klient-ID-en tokenet utstedes til,
    // så azp settes via clientId. Med azp = null fjernes claimet helt.
    private fun utstedToken(subject: String, azp: String?, claims: Map<String, Any>): SignedJWT {
        val callback = DefaultOAuth2TokenCallback(
            issuerId = "issuer1",
            subject = subject,
            audience = listOf("dumbdumb"),
            claims = claims
        )
        val callbackUtenAzp = object : OAuth2TokenCallback by callback {
            override fun addClaims(tokenRequest: TokenRequest): Map<String, Any> =
                callback.addClaims(tokenRequest) - "azp"
        }
        return mockOAuth2Server.issueToken(
            "issuer1",
            azp ?: "ubrukt",
            if (azp == null) callbackUtenAzp else callback
        )
    }

    private fun hentPersonToken(
        grupper: List<String> = listOf(DRIFTSGRUPPE_ID),
        azp: String? = CONSOLE_KLIENT_ID
    ): String = utstedToken(
        subject = "testbruker",
        azp = azp,
        claims = mapOf(
            "oid" to "test-oid",
            "NAVident" to "test123",
            "groups" to grupper
        )
    ).serialize()

    private fun hentMaskinToken(azp: String = CONSOLE_KLIENT_ID): String = utstedToken(
        subject = "test-app-oid",
        azp = azp,
        claims = mapOf(
            "oid" to "test-app-oid",
            "azp_name" to "test-cluster:teammelosys:melosys-console",
            "idtyp" to "app",
            "roles" to listOf("access_as_application")
        )
    ).serialize()

    private fun hent(endepunkt: String, token: String? = null, nøkkel: String? = null): ResultActions =
        mockMvc.perform(
            get(endepunkt).apply {
                token?.let { header(HttpHeaders.AUTHORIZATION, "Bearer $it") }
                nøkkel?.let { header(API_KEY_HEADER, it) }
            }.accept(MediaType.APPLICATION_JSON_VALUE)
        )

    private fun ResultActions.skalAvvisesMed(melding: String) {
        andExpect(status().isForbidden)
            .andReturn().response.contentAsString shouldBe melding
    }

    // --- Uten gyldig token ---

    @Test
    fun `skal returnere 401 når bearer token mangler`() {
        hent("/admin/kafka/errors").andExpect(status().isUnauthorized)
    }

    @Test
    fun `skal returnere 401 når bearer token er ugyldig`() {
        hent("/admin/kafka/errors", token = "ugyldig-token").andExpect(status().isUnauthorized)
    }

    // --- Personkall ---

    @Test
    fun `skal returnere 200 for personkall fra Console med driftsgruppe uten API-nøkkel`() {
        hent("/admin/kafka/errors", token = hentPersonToken()).andExpect(status().isOk)
    }

    @Test
    fun `skal returnere 403 for personkall med driftsgruppe fra en annen klient enn Console`() {
        hent("/admin/kafka/errors", token = hentPersonToken(azp = ANNEN_KLIENT_ID))
            .skalAvvisesMed(UKJENT_KLIENT)
    }

    @Test
    fun `skal returnere 403 når tokenet mangler azp`() {
        val token = utstedToken(
            subject = "testbruker",
            azp = null,
            claims = mapOf(
                "oid" to "test-oid",
                "NAVident" to "test123",
                "groups" to listOf(DRIFTSGRUPPE_ID)
            )
        )
        // Forutsetning: tokenet har faktisk ikke azp
        token.jwtClaimsSet.getClaim("azp").shouldBeNull()

        hent("/admin/kafka/errors", token = token.serialize()).skalAvvisesMed(UKJENT_KLIENT)
    }

    @Test
    fun `skal returnere 403 når personkall fra Console mangler driftsgruppe`() {
        hent("/admin/kafka/errors", token = hentPersonToken(grupper = emptyList()))
            .skalAvvisesMed(MANGLER_DRIFTSGRUPPE)
    }

    @Test
    fun `skal returnere 403 når personkall fra Console bare har en annen gruppe enn driftsgruppen`() {
        hent("/admin/kafka/errors", token = hentPersonToken(grupper = listOf(ANNEN_GRUPPE_ID)))
            .skalAvvisesMed(MANGLER_DRIFTSGRUPPE)
    }

    // --- Maskinkall ---

    @Test
    fun `skal returnere 200 for maskinkall fra Console mot automatisk rute`() {
        // Consoles automatiske synk bruker denne ruten med M2M-token
        hent("/admin/prosessinstanser/feilede", token = hentMaskinToken()).andExpect(status().isOk)
    }

    @Test
    fun `skal returnere 200 for maskinkall fra Console mot andre admin-endepunkter`() {
        hent("/admin/kafka/errors", token = hentMaskinToken()).andExpect(status().isOk)
    }

    @Test
    fun `skal returnere 403 for maskinkall fra en annen klient enn Console`() {
        hent("/admin/prosessinstanser/feilede", token = hentMaskinToken(azp = ANNEN_KLIENT_ID))
            .skalAvvisesMed(UKJENT_KLIENT)
    }

    // --- Den fjernede API-nøkkelen ---

    @Test
    fun `skal ignorere nøkkelheaderen når kallet ellers er gyldig`() {
        hent("/admin/kafka/errors", token = hentPersonToken(), nøkkel = "feil-nøkkel")
            .andExpect(status().isOk)
    }

    @Test
    fun `skal ikke gi tilgang uten driftsgruppe selv om nøkkelheaderen sendes`() {
        hent("/admin/kafka/errors", token = hentPersonToken(grupper = emptyList()), nøkkel = "dummy")
            .skalAvvisesMed(MANGLER_DRIFTSGRUPPE)
    }

    // --- På tvers av admin-kontrollere ---

    @Test
    fun `skal ha samme tilgangsstyring på tvers av admin-kontrollere`() {
        val endepunkter = listOf(
            "/admin/kafka/errors",
            "/admin/prosessinstanser/feilede",
            // Uttrekket lister Melosys saksnummer, så det er verdt å pinne at transporten faktisk er autentisert
            "/admin/statistikk/rammeavtale-fjernarbeid",
        )

        endepunkter.forEach { endepunkt ->
            hent(endepunkt).andExpect(status().isUnauthorized)
            hent(endepunkt, token = hentPersonToken()).andExpect(status().isOk)
            hent(endepunkt, token = hentPersonToken(grupper = emptyList())).skalAvvisesMed(MANGLER_DRIFTSGRUPPE)
            hent(endepunkt, token = hentPersonToken(azp = ANNEN_KLIENT_ID)).skalAvvisesMed(UKJENT_KLIENT)
        }
    }

    // --- Alle registrerte admin-endepunkter ---
    //
    // RestControllerInterceptor gir systemtoken til alle kall under /admin/ og stoler på at
    // AdminTilgangInterceptor allerede har avvist uautoriserte kall. Endepunktene hentes fra Spring,
    // så nye admin-kontrollere dekkes uten at testene må oppdateres. assertSoftly viser alle
    // endepunkter som feiler, ikke bare det første.

    @Test
    fun `skal avvise kall uten driftsgruppe på alle registrerte admin-endepunkter`() {
        val endepunkter = registrerteAdminEndepunkter()
        val token = hentPersonToken(grupper = emptyList())

        assertSoftly {
            endepunkter.forEach { endepunkt ->
                withClue(endepunkt) {
                    val respons = kall(endepunkt, token)
                    respons.status shouldBe 403
                    respons.contentAsString shouldBe MANGLER_DRIFTSGRUPPE
                }
            }
        }
    }

    private data class Endepunkt(val metode: HttpMethod, val mønster: String) {
        // Interceptoren avviser før argumentene leses, så stivariablene trenger bare å matche mønsteret
        val url = mønster.replace(Regex("\\{[^}]+}"), "1")

        override fun toString() = "$metode $mønster"
    }

    private fun registrerteAdminEndepunkter(): List<Endepunkt> {
        val endepunkter = handlerMapping.handlerMethods.keys.flatMap { info ->
            val metoder = info.methodsCondition.methods.ifEmpty { setOf(RequestMethod.GET) }
            info.patternValues
                .filter { it.startsWith("/admin/") }
                .flatMap { mønster -> metoder.map { Endepunkt(it.asHttpMethod(), mønster) } }
        }

        // Vakt mot falsk grønn: finner oppslaget ingen endepunkter, kjører forEach i testene ingen
        // assertions, og testene passerer uten å ha sjekket noe. De to endepunktene er hentet fra hver
        // sin modul (integrasjon og saksflyt), så vakten viser også at oppslaget når kontrollere utenfor
        // frontend-api.
        endepunkter.map { it.mønster }.shouldContainAll("/admin/kafka/errors", "/admin/prosessinstanser/feilede")
        return endepunkter
    }

    private fun kall(endepunkt: Endepunkt, token: String?): MockHttpServletResponse =
        mockMvc.perform(
            request(endepunkt.metode, endepunkt.url).apply {
                token?.let { header(HttpHeaders.AUTHORIZATION, "Bearer $it") }
            }.contentType(MediaType.APPLICATION_JSON)
        ).andReturn().response
}
