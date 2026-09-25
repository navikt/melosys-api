package no.nav.melosys.itest

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.melosys.Application
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Tester for admin-kontroller autentisering som krever både API-nøkkel og bearer token.
 *
 * MELOSYS-8271: Personkall krever i tillegg driftsgruppen i tokenets `groups`-claim.
 * Maskinkall (`idtyp = app`) krever fortsatt bare API-nøkkel og gyldig token.
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
class AdminControllerApiKeyIT(
    @Autowired var mockMvc: MockMvc,
    @Autowired var mockOAuth2Server: MockOAuth2Server
) : OracleTestContainerBase() {

    companion object {
        const val API_KEY_HEADER = "X-MELOSYS-ADMIN-APIKEY"
        const val GYLDIG_API_NOKKEL = "dummy"
        const val UGYLDIG_API_NOKKEL = "incorrect"

        // Samme verdi som Melosys-admin.driftsgruppe i application-test.yml
        const val DRIFTSGRUPPE_ID = "00000000-0000-0000-0000-000000000001"
        const val ANNEN_GRUPPE_ID = "00000000-0000-0000-0000-000000000002"
    }

    private fun hentBearerToken(grupper: List<String> = listOf(DRIFTSGRUPPE_ID)): String {
        return mockOAuth2Server.issueToken(
            issuerId = "issuer1",
            subject = "testbruker",
            audience = "dumbdumb",
            claims = mapOf(
                "oid" to "test-oid",
                "azp" to "test-azp",
                "NAVident" to "test123",
                "groups" to grupper
            )
        ).serialize()
    }

    private fun hentMaskinToken(): String {
        return mockOAuth2Server.issueToken(
            issuerId = "issuer1",
            subject = "test-app-oid",
            audience = "dumbdumb",
            claims = mapOf(
                "oid" to "test-app-oid",
                "azp" to "test-azp",
                "azp_name" to "test-cluster:teammelosys:melosys-console",
                "idtyp" to "app",
                "roles" to listOf("access_as_application")
            )
        ).serialize()
    }

    @Test
    fun `skal returnere 403 når både API-nøkkel og bearer token mangler`() {
        mockMvc.perform(
            get("/admin/kafka/errors")
                .accept(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isForbidden)
            .andReturn().response.contentAsString shouldBe "Invalid API key"
    }

    @Test
    fun `skal returnere 403 når API-nøkkel er feil og bearer token mangler`() {
        mockMvc.perform(
            get("/admin/kafka/errors")
                .header(API_KEY_HEADER, UGYLDIG_API_NOKKEL)
                .accept(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isForbidden)
            .andReturn().response.contentAsString shouldBe "Invalid API key"
    }

    @Test
    fun `skal returnere 403 når API-nøkkel mangler men bearer token er oppgitt`() {
        mockMvc.perform(
            get("/admin/kafka/errors")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken()}")
                .accept(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isForbidden)
            .andReturn().response.contentAsString shouldBe "Invalid API key"
    }

    @Test
    fun `skal returnere 401 når API-nøkkel er korrekt men bearer token mangler`() {
        mockMvc.perform(
            get("/admin/kafka/errors")
                .header(API_KEY_HEADER, GYLDIG_API_NOKKEL)
                .accept(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `skal returnere 401 når API-nøkkel er korrekt men bearer token er ugyldig`() {
        mockMvc.perform(
            get("/admin/kafka/errors")
                .header(API_KEY_HEADER, GYLDIG_API_NOKKEL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ugyldig-token")
                .accept(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `skal returnere 200 når både API-nøkkel og bearer token er korrekte`() {
        mockMvc.perform(
            get("/admin/kafka/errors")
                .header(API_KEY_HEADER, GYLDIG_API_NOKKEL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken()}")
                .accept(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(status().isOk)
    }

    @Test
    fun `skal returnere 403 når API-nøkkel er feil selv om bearer token er korrekt`() {
        mockMvc.perform(
            get("/admin/kafka/errors")
                .header(API_KEY_HEADER, UGYLDIG_API_NOKKEL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken()}")
                .accept(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isForbidden)
            .andReturn().response.contentAsString shouldBe "Invalid API key"
    }

    @Test
    fun `skal kreve både API-nøkkel og bearer token for ProsessinstansAdminController`() {
        // Test med manglende API-nøkkel
        mockMvc.perform(
            get("/admin/prosessinstanser/feilede")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken()}")
                .accept(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isForbidden)
            .andReturn().response.contentAsString shouldBe "Invalid API key"

        // Test med manglende bearer token
        mockMvc.perform(
            get("/admin/prosessinstanser/feilede")
                .header(API_KEY_HEADER, GYLDIG_API_NOKKEL)
                .accept(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isUnauthorized)

        // Test med begge korrekte
        mockMvc.perform(
            get("/admin/prosessinstanser/feilede")
                .header(API_KEY_HEADER, GYLDIG_API_NOKKEL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken()}")
                .accept(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(status().isOk)
    }

    @Test
    fun `skal kreve konsistent autentisering på tvers av admin-kontrollere`() {
        val endepunkter = listOf(
            "/admin/kafka/errors",
            "/admin/prosessinstanser/feilede",
            // Uttrekket lister Melosys saksnummer, så det er verdt å pinne at transporten faktisk er autentisert
            "/admin/statistikk/rammeavtale-fjernarbeid",
        )

        endepunkter.forEach { endepunkt ->
            // Test manglende API-nøkkel
            mockMvc.perform(
                get(endepunkt)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken()}")
                    .accept(MediaType.APPLICATION_JSON_VALUE)
            )
                .andExpect(status().isForbidden)
                .andReturn().response.contentAsString shouldBe "Invalid API key"

            // Test manglende bearer token
            mockMvc.perform(
                get(endepunkt)
                    .header(API_KEY_HEADER, GYLDIG_API_NOKKEL)
                    .accept(MediaType.APPLICATION_JSON_VALUE)
            )
                .andExpect(status().isUnauthorized)

            // Test begge korrekte
            mockMvc.perform(
                get(endepunkt)
                    .header(API_KEY_HEADER, GYLDIG_API_NOKKEL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken()}")
                    .accept(MediaType.APPLICATION_JSON_VALUE)
            ).andExpect(status().isOk)
        }
    }

    // --- MELOSYS-8271: driftsgruppe for personkall ---

    @Test
    fun `skal returnere 403 når personkall har korrekt API-nøkkel men mangler driftsgruppe`() {
        mockMvc.perform(
            get("/admin/kafka/errors")
                .header(API_KEY_HEADER, GYLDIG_API_NOKKEL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken(grupper = emptyList())}")
                .accept(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isForbidden)
            // Avvisningen skal komme fra gruppesjekken, ikke fra nøkkelsjekken
            .andReturn().response.contentAsString shouldNotBe "Invalid API key"
    }

    @Test
    fun `skal returnere 403 når personkall bare har en annen gruppe enn driftsgruppen`() {
        mockMvc.perform(
            get("/admin/kafka/errors")
                .header(API_KEY_HEADER, GYLDIG_API_NOKKEL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken(grupper = listOf(ANNEN_GRUPPE_ID))}")
                .accept(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isForbidden)
            .andReturn().response.contentAsString shouldNotBe "Invalid API key"
    }

    @Test
    fun `skal fortsatt kreve API-nøkkel når personkall har driftsgruppe`() {
        mockMvc.perform(
            get("/admin/kafka/errors")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken(grupper = listOf(DRIFTSGRUPPE_ID))}")
                .accept(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isForbidden)
            .andReturn().response.contentAsString shouldBe "Invalid API key"
    }

    @Test
    fun `skal returnere 200 for maskinkall med korrekt API-nøkkel uten driftsgruppe`() {
        // Consoles automatiske synk bruker denne ruten med M2M-token
        mockMvc.perform(
            get("/admin/prosessinstanser/feilede")
                .header(API_KEY_HEADER, GYLDIG_API_NOKKEL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentMaskinToken()}")
                .accept(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(status().isOk)
    }

    @Test
    fun `skal returnere 403 for maskinkall uten API-nøkkel`() {
        mockMvc.perform(
            get("/admin/prosessinstanser/feilede")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentMaskinToken()}")
                .accept(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isForbidden)
            .andReturn().response.contentAsString shouldBe "Invalid API key"
    }

    @Test
    fun `skal kreve driftsgruppe for personkall på tvers av admin-kontrollere`() {
        val endepunkter = listOf(
            "/admin/kafka/errors",
            "/admin/prosessinstanser/feilede",
            "/admin/statistikk/rammeavtale-fjernarbeid",
        )

        endepunkter.forEach { endepunkt ->
            mockMvc.perform(
                get(endepunkt)
                    .header(API_KEY_HEADER, GYLDIG_API_NOKKEL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer ${hentBearerToken(grupper = emptyList())}")
                    .accept(MediaType.APPLICATION_JSON_VALUE)
            )
                .andExpect(status().isForbidden)
                .andReturn().response.contentAsString shouldNotBe "Invalid API key"
        }
    }
}
