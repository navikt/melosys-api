package no.nav.melosys.tjenester.gui.avklartefakta

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.avklartefakta.AvklartefaktaRegistrering
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl.SAMBOER_UTEN_FELLES_BARN
import no.nav.melosys.service.avklartefakta.*
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.service.tilgang.Ressurs
import no.nav.melosys.tjenester.gui.dto.oppsummertefakta.ArbeidslandDto
import no.nav.melosys.tjenester.gui.dto.oppsummertefakta.VirksomheterDto
import no.nav.melosys.tjenester.gui.util.responseBody
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AvklartefaktaController::class])
class AvklartefaktaControllerTest {

    @MockkBean
    private lateinit var avklartefaktaService: AvklartefaktaService

    @MockkBean
    private lateinit var avklarteVirksomheterService: AvklarteVirksomheterService

    @MockkBean
    private lateinit var avklarteFaktaArbeidslandService: AvklarteFaktaArbeidslandService

    @MockkBean
    private lateinit var avklarteMedfolgendeFamilieService: AvklarteMedfolgendeFamilieService

    @MockkBean
    private lateinit var avklartUkjentSluttdatoMedlemskapsperiodeService: AvklartUkjentSluttdatoMedlemskapsperiodeService

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @MockkBean
    private lateinit var avklartManglendeInnbetalingService: AvklartManglendeInnbetalingService

    @MockkBean
    private lateinit var avklartFamilieRelasjonTypeService: AvklartFamilieRelasjonTypeService

    @MockkBean
    private lateinit var avklartArbeidssituasjonTypeService: AvklartArbeidssituasjonTypeService

    @MockkBean
    private lateinit var avklartOppholdTypeService: AvklartOppholdTypeService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `skal hente avklarte fakta strukturert`() {
        val dtos = lagAvklarteFaktaDtoSet()
        every { avklartefaktaService.hentAlleAvklarteFakta(1L) } returns dtos
        every { aksesskontroll.autoriser(1L) } returns Unit


        mockMvc.perform(
            get("$BASE_URL/{behandlingID}/oppsummering", 1L)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(AvklartefaktaOppsummeringDto(dtos), AvklartefaktaOppsummeringDto::class.java))
    }

    @Test
    fun `skal lagre virksomheter som avklarte fakta`() {
        val virksomheterDto = VirksomheterDto().apply {
            virksomhetIDer = listOf("000000000")
        }
        val dtos = lagAvklarteFaktaDtoSet()
        every { avklartefaktaService.hentAlleAvklarteFakta(1L) } returns dtos
        every { aksesskontroll.autoriser(1L) } returns Unit
        every { aksesskontroll.autoriserSkrivTilRessurs(1L, Ressurs.AVKLARTE_FAKTA) } returns Unit
        every { avklarteVirksomheterService.lagreVirksomheterSomAvklartefakta(1L, listOf("000000000")) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}/virksomheter", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(virksomheterDto))
        )
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(AvklartefaktaOppsummeringDto(dtos), AvklartefaktaOppsummeringDto::class.java))
    }

    @Test
    fun `skal lagre arbeidsland som avklarte fakta`() {
        val arbeidslandDto = ArbeidslandDto().apply {
            arbeidsland = listOf("NO")
        }
        val dtos = lagAvklarteFaktaDtoSet()
        every { avklartefaktaService.hentAlleAvklarteFakta(1L) } returns dtos
        every { aksesskontroll.autoriser(1L) } returns Unit
        every { aksesskontroll.autoriserSkrivTilRessurs(1L, Ressurs.AVKLARTE_FAKTA) } returns Unit
        every { avklarteFaktaArbeidslandService.lagreArbeidslandSomAvklartefakta(1L, listOf("NO")) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}/arbeidsland", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(arbeidslandDto))
        )
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(AvklartefaktaOppsummeringDto(dtos), AvklartefaktaOppsummeringDto::class.java))
    }

    private fun lagAvklarteFaktaDtoSet(): Set<AvklartefaktaDto> = setOf(
        lagAvklartefaktaDto(uuid1, Avklartefaktatyper.VURDERING_LOVVALG_BARN, true, null, null),
        lagAvklartefaktaDto(uuid2, Avklartefaktatyper.VURDERING_LOVVALG_BARN, false, "fritekstForUuid2", OVER_18_AR.kode),
        lagAvklartefaktaDto(uuid3, Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER, true, null, null),
        lagAvklartefaktaDto(
            uuid4,
            Avklartefaktatyper.VURDERING_MEDLEMSKAP_EKTEFELLE_SAMBOER,
            false,
            "fritekstForUuid4",
            SAMBOER_UTEN_FELLES_BARN.kode
        )
    )

    private fun lagAvklartefaktaDto(
        subjektID: String,
        type: Avklartefaktatyper,
        fakta: Boolean,
        begrunnelseFritekst: String?,
        begrunnelsekode: String?
    ): AvklartefaktaDto {
        val avklartefakta = Avklartefakta().apply {
            subjekt = subjektID
            this.type = type
            referanse = type.kode
            this.begrunnelseFritekst = begrunnelseFritekst
        }

        if (fakta) {
            avklartefakta.fakta = Avklartefakta.VALGT_FAKTA
        } else {
            avklartefakta.fakta = Avklartefakta.IKKE_VALGT_FAKTA
            val registrering = AvklartefaktaRegistrering().apply {
                this.avklartefakta = avklartefakta
                begrunnelseKode = begrunnelsekode
            }
            avklartefakta.registreringer = hashSetOf(registrering)
        }
        return AvklartefaktaDto(avklartefakta)
    }

    companion object {
        private const val BASE_URL = "/api/avklartefakta"
        private const val uuid1 = "36053ce6-75e5-4430-b8af-2ce60092877d"
        private const val uuid2 = "e502441e-9cdd-4d2a-84c2-25261b6e7cb2"
        private const val uuid3 = "d7645947-e7e9-46c0-987a-d0e91d6fed6f"
        private const val uuid4 = "4136cdce-0c09-4693-a032-5914575c3ac3"
    }
}
