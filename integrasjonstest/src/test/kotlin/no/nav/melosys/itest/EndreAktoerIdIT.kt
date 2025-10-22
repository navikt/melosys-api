package no.nav.melosys.itest

import io.kotest.matchers.shouldBe
import no.nav.melosys.Application
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.repository.AktoerRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import kotlin.test.Test

@ActiveProfiles("test")
@SpringBootTest(
    classes = [Application::class],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@EmbeddedKafka
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext
@EnableMockOAuth2Server
@AutoConfigureMockMvc
class EndreAktoerIdIT(
    @Autowired var mockMvc: MockMvc,
    @Autowired var mockOAuth2Server: MockOAuth2Server,
    @Autowired var fagsakRepository: FagsakRepository,
    @Autowired var aktoerRepository: AktoerRepository
) : OracleTestContainerBase() {

    private fun hentBearerToken(): String {
        return mockOAuth2Server.issueToken(
            issuerId = "issuer1",
            subject = "testbruker",
            audience = "dumbdumb",
            claims = mapOf(
                "oid" to "test-oid",
                "azp" to "test-azp",
                "NAVident" to "test123"
            )
        ).serialize()
    }

    @Test
    @Transactional
    fun `endreAktoerId skal oppdatere aktørId på eksisterende bruker i fagsak`() {
        val saksnummer = "MEL-123"
        val gammelAktoerid = "1111111111111"
        val nyAktoerid = "2222222222222"

        // Create fagsak with its actor
        val fagsak = Fagsak(
            saksnummer = saksnummer,
            type = Sakstyper.FTRL,
            tema = Sakstemaer.MEDLEMSKAP_LOVVALG,
            status = Saksstatuser.OPPRETTET
        )
        fagsakRepository.save(fagsak)

        val brukerAktor = Aktoer().apply {
            rolle = Aktoersroller.BRUKER
            aktørId = gammelAktoerid
            setFagsak(fagsak)
        }
        aktoerRepository.save(brukerAktor)
        fagsak.aktører.add(brukerAktor)
        fagsakRepository.save(fagsak)

        // Change the aktørId for the existing actor
        mockMvc.perform(
            MockMvcRequestBuilders.put("/admin/fagsaker/$saksnummer/endreAktoerId/$nyAktoerid")
                .header("X-MELOSYS-ADMIN-APIKEY", "dummy")
                .header("Authorization", "Bearer ${hentBearerToken()}")
        ).andExpect(MockMvcResultMatchers.status().isNoContent)

        // Hent den oppdaterte aktøren
        val oppdatertAktor = aktoerRepository.findById(brukerAktor.id).get()

        // Samme aktør men med ny aktørId
        oppdatertAktor.aktørId shouldBe nyAktoerid
        oppdatertAktor.fagsak?.saksnummer shouldBe saksnummer
        oppdatertAktor.id shouldBe brukerAktor.id

        // Fagsak skal være oppdater og ha kun EN bruker aktør
        val oppdatertFagsak = fagsakRepository.findById(saksnummer).get()
        val fagsakBrukerAktorer = aktoerRepository.findByFagsakAndRolle(oppdatertFagsak, Aktoersroller.BRUKER)
        fagsakBrukerAktorer.size shouldBe 1
        fagsakBrukerAktorer.first().aktørId shouldBe nyAktoerid
        fagsakBrukerAktorer.first().id shouldBe brukerAktor.id
    }
}
