package no.nav.melosys.service.oppgave

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.oppgave.konsument.OppgaveV2Client
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

@ExtendWith(MockKExtension::class)
internal class FeilmerketNøkkelordOppryddingTest {

    @MockK
    private lateinit var oppgaveV2Client: OppgaveV2Client

    private lateinit var opprydding: FeilmerketNøkkelordOpprydding

    @BeforeEach
    fun setUp() {
        opprydding = FeilmerketNøkkelordOpprydding(oppgaveV2Client)
    }

    @Test
    fun `finnFeilmerkede plukker BEH_SAK_MK med år-nøkkelord og lar BEH_ARSAVREG og oppgaver uten år-nøkkelord stå`() {
        every { oppgaveV2Client.søkOppgaverForEnhet(ENHET, null) } returns søkRespons(
            oppgaveJson(100, "BEH_SAK_MK", listOf("Årsavregning 2024")),
            oppgaveJson(200, "BEH_ARSAVREG", listOf("Årsavregning 2024")),
            oppgaveJson(300, "BEH_SAK_MK", listOf("Noe helt annet")),
            oppgaveJson(400, "BEH_SAK_MK", emptyList())
        )

        val rapport = opprydding.finnFeilmerkede(ENHET)

        rapport.antallSkannet shouldBe 4
        rapport.antallFeilmerkede shouldBe 1
        rapport.feilmerkede.map { it.id } shouldContainExactly listOf("100")
        rapport.feilmerkede.single().run {
            oppgavetype shouldBe "BEH_SAK_MK"
            erFeilmerket shouldBe true
            status shouldBe "AAPEN"
            saksreferanse shouldBe "MEL-100"
            beskrivelse shouldBe "beskrivelse-100"
        }
        // Korrekt merkede BEH_ARSAVREG med år-nøkkelord skal vises, men ikke ryddes.
        rapport.antallKorrektMerkede shouldBe 1
        rapport.korrektMerkede.map { it.id } shouldContainExactly listOf("200")
        rapport.korrektMerkede.single().erFeilmerket shouldBe false
    }

    @Test
    fun `rydd fjerner år-nøkkelordet fra feilmerket oppgave med riktig predikat`() {
        every { oppgaveV2Client.søkOppgaverForEnhet(ENHET, null) } returns søkRespons(
            oppgaveJson(100, "BEH_SAK_MK", listOf("Årsavregning 2024", "Behold meg"))
        )
        val predikat = slot<(String) -> Boolean>()
        every { oppgaveV2Client.fjernNøkkelord("100", capture(predikat)) } returns Unit

        val resultat = opprydding.rydd(ENHET, dryRun = false)

        resultat.antallFunnet shouldBe 1
        resultat.antallFjernet shouldBe 1
        resultat.antallFeilet shouldBe 0
        resultat.fjernetIder shouldContainExactly listOf("100")
        verify(exactly = 1) { oppgaveV2Client.fjernNøkkelord("100", any()) }
        // Predikatet skal kun fjerne «Årsavregning <år>», ikke andre nøkkelord.
        predikat.captured("Årsavregning 2024") shouldBe true
        predikat.captured("Årsavregning 99") shouldBe false
        predikat.captured("Behold meg") shouldBe false
    }

    @Test
    fun `rydd med dryRun true patcher ingenting`() {
        every { oppgaveV2Client.søkOppgaverForEnhet(ENHET, null) } returns søkRespons(
            oppgaveJson(100, "BEH_SAK_MK", listOf("Årsavregning 2024"))
        )

        val resultat = opprydding.rydd(ENHET, dryRun = true)

        resultat.antallFunnet shouldBe 1
        resultat.antallFjernet shouldBe 0
        resultat.fjernetIder.shouldBeEmpty()
        verify(exactly = 0) { oppgaveV2Client.fjernNøkkelord(any(), any()) }
    }

    @Test
    fun `rydd lar en feilende oppgave ikke stoppe resten`() {
        every { oppgaveV2Client.søkOppgaverForEnhet(ENHET, null) } returns søkRespons(
            oppgaveJson(100, "BEH_SAK_MK", listOf("Årsavregning 2024")),
            oppgaveJson(200, "BEH_SAK_MK", listOf("Årsavregning 2025"))
        )
        every { oppgaveV2Client.fjernNøkkelord("100", any()) } throws TekniskException("Kall mot Oppgave v2 feilet.")
        every { oppgaveV2Client.fjernNøkkelord("200", any()) } returns Unit

        val resultat = opprydding.rydd(ENHET, dryRun = false)

        resultat.antallFunnet shouldBe 2
        resultat.antallFjernet shouldBe 1
        resultat.antallFeilet shouldBe 1
        resultat.fjernetIder shouldContainExactly listOf("200")
        resultat.feiletIder shouldContainExactly listOf("100")
        verify(exactly = 1) { oppgaveV2Client.fjernNøkkelord("200", any()) }
    }

    @Test
    fun `ryddAsynkront kjører jobben og status reflekterer resultatet`() {
        every { oppgaveV2Client.søkOppgaverForEnhet(ENHET, null) } returns søkRespons(
            oppgaveJson(100, "BEH_SAK_MK", listOf("Årsavregning 2024")),
            oppgaveJson(200, "BEH_ARSAVREG", listOf("Årsavregning 2024"))
        )
        every { oppgaveV2Client.fjernNøkkelord("100", any()) } returns Unit

        opprydding.ryddAsynkront(ENHET, dryRun = false)

        val status = opprydding.status()
        status["isRunning"] shouldBe false
        status["antallSkannet"] shouldBe 2
        status["antallFunnet"] shouldBe 1
        status["antallKorrektMerkede"] shouldBe 1
        status["antallFjernet"] shouldBe 1
        status["antallFeilet"] shouldBe 0
        verify(exactly = 1) { oppgaveV2Client.fjernNøkkelord("100", any()) }
    }

    @Test
    fun `finnFeilmerkede paginerer til hasNext er false`() {
        every { oppgaveV2Client.søkOppgaverForEnhet(ENHET, null) } returns søkRespons(
            oppgaveJson(100, "BEH_SAK_MK", listOf("Årsavregning 2024")),
            hasNext = true,
            endCursor = "side2"
        )
        every { oppgaveV2Client.søkOppgaverForEnhet(ENHET, "side2") } returns søkRespons(
            oppgaveJson(200, "BEH_SAK_MK", listOf("Årsavregning 2025"))
        )

        val rapport = opprydding.finnFeilmerkede(ENHET)

        rapport.antallSkannet shouldBe 2
        rapport.feilmerkede.map { it.id } shouldContainExactly listOf("100", "200")
        verify(exactly = 1) { oppgaveV2Client.søkOppgaverForEnhet(ENHET, "side2") }
    }

    companion object {
        private const val ENHET = "4530"
        private val mapper = ObjectMapper()

        private fun oppgaveJson(id: Int, oppgavetype: String, nokkelord: List<String>): String {
            val nokkelordJson = nokkelord.joinToString(",") { "\"$it\"" }
            return """
                {
                  "id": $id,
                  "status": "AAPEN",
                  "saksreferanse": "MEL-$id",
                  "beskrivelse": "beskrivelse-$id",
                  "fristDato": "2026-09-23",
                  "nokkelord": [$nokkelordJson],
                  "kategorisering": {
                    "tema": { "kode": "TRY", "term": "Trygdeavgift" },
                    "oppgavetype": { "kode": "$oppgavetype", "term": "$oppgavetype" },
                    "behandlingstema": { "term": "EU/EØS - Yrkesaktiv" }
                  },
                  "fordeling": { "enhet": { "nr": "4530" } },
                  "opprettet": { "tidspunkt": "2026-01-15T10:00:00Z" }
                }
            """.trimIndent()
        }

        private fun søkRespons(
            vararg oppgaver: String,
            hasNext: Boolean = false,
            endCursor: String? = null
        ): JsonNode {
            val cursorJson = endCursor?.let { "\"$it\"" } ?: "null"
            return mapper.readTree(
                """
                {
                  "oppgaver": [${oppgaver.joinToString(",")}],
                  "pagination": { "hasNext": $hasNext, "endCursor": $cursorJson }
                }
                """.trimIndent()
            )
        }
    }
}
