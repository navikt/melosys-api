package no.nav.melosys.tjenester.gui.ftrl.bestemmelser.vilkaar

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.service.ftrl.bestemmelse.vilkaar.Vilkår
import no.nav.melosys.service.ftrl.bestemmelse.vilkaar.VilkårForBestemmelse
import no.nav.melosys.service.tilgang.Aksesskontroll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [VilkårController::class])
@ExtendWith(MockKExtension::class)
class VilkårControllerTest(@Autowired private val mockMvc: MockMvc) {
    @MockkBean
    lateinit var vilkårForBestemmelse: VilkårForBestemmelse

    @MockkBean
    lateinit var aksesskontroll: Aksesskontroll

    @Test
    fun `hent vilkår for ftrl bestemmelse yrkesaktiv`() {
        val bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1

        justRun { aksesskontroll.autoriser(any()) }
        every { vilkårForBestemmelse.hentVilkår(bestemmelse, Behandlingstema.YRKESAKTIV, emptyMap(), 1) } returns listOf(
            Vilkår(
                Vilkaar.FTRL_2_1_BOSATT_NORGE_FORUT
            ),
            Vilkår(
                Vilkaar.FTRL_2_1_OPPHOLD_UNDER_12MND
            ),
            Vilkår(
                Vilkaar.FTRL_2_1_LOVLIG_OPPHOLD
            ),
        )

        mockMvc.perform(
            get("/api/ftrl/bestemmelser/{bestemmelse}/vilkaar", bestemmelse)
                .param("behandlingstema", "YRKESAKTIV")
                .param("behandlingID", "1")
        )
            .andExpect(status().isOk())
    }

    @Test
    fun `hent vilkår for ftrl bestemmelse ikke yrkesaktiv`() {
        val bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1

        justRun { aksesskontroll.autoriser(any()) }
        every { vilkårForBestemmelse.hentVilkår(bestemmelse, Behandlingstema.IKKE_YRKESAKTIV, emptyMap(), 1) } returns listOf(
            Vilkår(
                Vilkaar.FTRL_2_1_BOSATT_NORGE_FORUT
            ),
            Vilkår(
                Vilkaar.FTRL_2_1_OPPHOLD_UNDER_12MND
            ),
            Vilkår(
                Vilkaar.FTRL_2_1_LOVLIG_OPPHOLD
            ),
        )


        mockMvc.perform(
            get("/api/ftrl/bestemmelser/{bestemmelse}/vilkaar", bestemmelse)
                .param("behandlingstema", "IKKE_YRKESAKTIV")
                .param("behandlingID", "1")
        )
            .andExpect(status().isOk())
    }
}
