package no.nav.melosys.service.sak

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.kodeverk.Saksstatuser
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@WebMvcTest(controllers = [SkjemaSaksstatusAdminController::class], properties = ["Melosys-admin.apikey=Dummy"])
class SkjemaSaksstatusAdminControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var skjemaSaksstatusSyncService: SkjemaSaksstatusSyncService

    @Test
    fun `synk uten parameter kjoerer dry-run som default`() {
        every { skjemaSaksstatusSyncService.massesynk(dryRun = true) } returns lagRapport(dryRun = true)

        mockMvc.perform(post("$BASE_URL/synk"))
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    """{
                        "dryRun": true,
                        "antallTotalt": 3,
                        "antallMottatt": 1,
                        "antallAvsluttet": 2,
                        "perMelosysStatus": {"OPPRETTET": 1, "LOVVALG_AVKLART": 2},
                        "antallOppdatert": null,
                        "ukjenteSkjemaIder": null
                    }"""
                )
            )

        verify(exactly = 1) { skjemaSaksstatusSyncService.massesynk(dryRun = true) }
    }

    @Test
    fun `synk med dryRun=false kjoerer reell synk og returnerer aggregert resultat`() {
        val ukjentSkjemaId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        val konfliktSkjemaId = UUID.fromString("650e8400-e29b-41d4-a716-446655440000")
        every { skjemaSaksstatusSyncService.massesynk(dryRun = false) } returns lagRapport(
            dryRun = false,
            antallOppdatert = 5,
            ukjenteSkjemaIder = listOf(ukjentSkjemaId),
            konfliktSkjemaIder = listOf(konfliktSkjemaId)
        )

        mockMvc.perform(post("$BASE_URL/synk").param("dryRun", "false"))
            .andExpect(status().isOk)
            .andExpect(
                content().json(
                    """{
                        "dryRun": false,
                        "antallTotalt": 3,
                        "antallOppdatert": 5,
                        "ukjenteSkjemaIder": ["550e8400-e29b-41d4-a716-446655440000"],
                        "konfliktSkjemaIder": ["650e8400-e29b-41d4-a716-446655440000"]
                    }"""
                )
            )

        verify(exactly = 1) { skjemaSaksstatusSyncService.massesynk(dryRun = false) }
    }

    private fun lagRapport(
        dryRun: Boolean,
        antallOppdatert: Int? = null,
        ukjenteSkjemaIder: List<UUID>? = null,
        konfliktSkjemaIder: List<UUID>? = null
    ) = SkjemaSaksstatusSynkRapport(
        dryRun = dryRun,
        antallTotalt = 3,
        antallMottatt = 1,
        antallAvsluttet = 2,
        perMelosysStatus = mapOf(
            Saksstatuser.OPPRETTET to 1,
            Saksstatuser.LOVVALG_AVKLART to 2
        ),
        antallOppdatert = antallOppdatert,
        ukjenteSkjemaIder = ukjenteSkjemaIder,
        konfliktSkjemaIder = konfliktSkjemaIder
    )

    companion object {
        private const val BASE_URL = "/admin/skjema-saksstatus"
    }
}
