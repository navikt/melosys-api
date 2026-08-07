package no.nav.melosys.tjenester.gui.saksbehandling

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService
import no.nav.melosys.service.lovligekombinasjoner.SakstemaKombinasjoner
import no.nav.melosys.service.lovligekombinasjoner.SakstypeKombinasjoner
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Låser wire-formen på kombinasjonstreet. Serialiseringen er hele poenget med endepunktet:
 * KodeSerializer ville gjort kodeverkstypene om til {kode, term}, og klienten sammenligner
 * treet mot tekstblokkenes avgrensning, som leveres som rene koder.
 */
@WebMvcTest(controllers = [LovligeKombinasjonerSaksbehandlingController::class])
class LovligeKombinasjonerSaksbehandlingControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {

    @MockkBean
    private lateinit var lovligeKombinasjonerSaksbehandlingService: LovligeKombinasjonerSaksbehandlingService

    private companion object {
        // Api-et eksponeres under /api; controllerens @RequestMapping er relativ til det.
        const val URL = "/api/saksbehandling/kombinasjoner/tre"
    }

    @Test
    fun `kombinasjonstreet leveres som rene koder, ikke som kode-term-objekter`() {
        every { lovligeKombinasjonerSaksbehandlingService.hentKombinasjonstre() } returns listOf(
            SakstypeKombinasjoner(
                sakstype = Sakstyper.EU_EOS,
                sakstemaer = listOf(
                    SakstemaKombinasjoner(
                        sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG,
                        behandlingstemaer = setOf(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingstema.PENSJONIST),
                    ),
                ),
            ),
        )

        mockMvc.perform(get(URL).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].sakstype").value("EU_EOS"))
            .andExpect(jsonPath("$[0].sakstemaer[0].sakstema").value("MEDLEMSKAP_LOVVALG"))
            // Sortert, slik at responsen ikke stokker om på seg selv mellom kall
            .andExpect(jsonPath("$[0].sakstemaer[0].behandlingstemaer", contains("PENSJONIST", "UTSENDT_ARBEIDSTAKER")))
    }

    @Test
    fun `tomt tre gir tom liste, ikke feil`() {
        every { lovligeKombinasjonerSaksbehandlingService.hentKombinasjonstre() } returns emptyList()

        mockMvc.perform(get(URL).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }
}
