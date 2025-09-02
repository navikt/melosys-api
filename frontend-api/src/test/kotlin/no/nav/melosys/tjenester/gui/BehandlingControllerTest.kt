package no.nav.melosys.tjenester.gui

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseNorge
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseUtland
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.*
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.bruker.SaksbehandlerService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.SaksopplysningerDto
import no.nav.melosys.tjenester.gui.dto.TidligereMedlemsperioderDto
import no.nav.melosys.tjenester.gui.dto.saksopplysninger.SaksopplysningerTilDto
import no.nav.melosys.tjenester.gui.util.NumericStringRandomizer
import no.nav.melosys.tjenester.gui.util.responseBody
import org.hamcrest.Matchers.equalTo
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate


@WebMvcTest(controllers = [BehandlingController::class])
class BehandlingControllerTest {

    @MockkBean
    private lateinit var behandlingService: BehandlingService

    @MockkBean
    private lateinit var saksopplysningerTilDto: SaksopplysningerTilDto

    @MockkBean
    private lateinit var saksbehandlerService: SaksbehandlerService

    @MockkBean
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var random: EasyRandom

    @BeforeEach
    fun setUp() {
        random = EasyRandom(EasyRandomParameters()
            .overrideDefaultInitialization(true)
            .collectionSizeRange(1, 4)
            .objectPoolSize(100)
            .dateRange(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))
            .excludeField(named("tilleggsinformasjonDetaljer").and(ofType(TilleggsinformasjonDetaljer::class.java)).and(inClass(Tilleggsinformasjon::class.java)))
            .excludeField(named("sed").and(ofType(SedDokument::class.java)))
            .stringLengthRange(2, 10)
            .randomize(GeografiskAdresse::class.java) { random.nextObject(SemistrukturertAdresse::class.java) }
            .randomize(MidlertidigPostadresse::class.java) {
                if (Math.random() > 0.5) random.nextObject(MidlertidigPostadresseNorge::class.java)
                else random.nextObject(MidlertidigPostadresseUtland::class.java)
            }
            .randomize(named("fnr").and(ofType(String::class.java)), NumericStringRandomizer(11))
            .randomize(named("fnrAnnenForelder").and(ofType(String::class.java)), NumericStringRandomizer(11))
            .randomize(named("orgnummer").and(ofType(String::class.java)), NumericStringRandomizer(9))
        )
    }

    @Test
    fun `skal knytte medlemsperioder til behandling`() {
        val tidligereMedlemsperioderDto = TidligereMedlemsperioderDto().apply {
            periodeIder = PERIODE_IDER
        }
        every { aksesskontroll.autoriser(BEHANDLING_ID) } returns Unit
        every { aksesskontroll.autoriserSkriv(BEHANDLING_ID) } returns Unit
        every { aksesskontroll.auditAutoriserSkriv(any(), any()) } returns Unit
        every { behandlingService.knyttMedlemsperioder(BEHANDLING_ID, PERIODE_IDER) } returns Unit
        every { behandlingService.oppdaterBehandlingsstatusHvisTilhørendeSaksbehandler(any(), any()) } returns Unit
        every { saksbehandlerService.finnNavnForIdent(any()) } returns java.util.Optional.of("Test User")


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}/tidligere-medlemsperioder", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tidligereMedlemsperioderDto))
        ).andExpect(status().isOk())


        verify { behandlingService.knyttMedlemsperioder(BEHANDLING_ID, PERIODE_IDER) }
    }

    @Test
    fun `skal hente medlemsperioder for behandling`() {
        every { behandlingService.hentMedlemsperioder(BEHANDLING_ID) } returns PERIODE_IDER
        every { aksesskontroll.autoriser(BEHANDLING_ID) } returns Unit
        val dto = TidligereMedlemsperioderDto().apply {
            periodeIder = PERIODE_IDER
        }


        mockMvc.perform(
            get("$BASE_URL/{behandlingID}/tidligere-medlemsperioder", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(responseBody(objectMapper).containsObjectAsJson(dto, TidligereMedlemsperioderDto::class.java))


        verify { behandlingService.hentMedlemsperioder(BEHANDLING_ID) }
    }

    @Test
    fun `skal hente behandling med saksopplysninger`() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID) } returns opprettTomBehandlingMedId()
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns BEHANDLINGSRESULTAT
        every { saksopplysningerTilDto.getSaksopplysningerDto(any()) } returns SaksopplysningerDto()
        every { aksesskontroll.autoriser(BEHANDLING_ID) } returns Unit
        every { aksesskontroll.autoriserSkriv(BEHANDLING_ID) } returns Unit
        every { aksesskontroll.auditAutoriserSkriv(any(), any()) } returns Unit
        every { aksesskontroll.behandlingKanRedigeresAvSaksbehandler(any(), any()) } returns true
        every { behandlingService.oppdaterBehandlingsstatusHvisTilhørendeSaksbehandler(any(), any()) } returns Unit
        every { saksbehandlerService.finnNavnForIdent(any()) } returns java.util.Optional.of("Test User")


        mockMvc.perform(
            get("$BASE_URL/{behandlingID}", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk())
    }

    @Test
    fun `skal hente mulige statuser for behandling`() {
        every { behandlingService.hentMuligeStatuser(BEHANDLING_ID) } returns MULIGE_STATUSER
        every { aksesskontroll.autoriser(BEHANDLING_ID) } returns Unit


        mockMvc.perform(
            get("$BASE_URL/{behandlingID}/mulige-statuser", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", equalTo(MULIGE_STATUSER.size)))
    }

    private fun opprettTomBehandlingMedId() = Behandling.forTest {
        id = BEHANDLING_ID
        fagsak = no.nav.melosys.domain.FagsakTestFactory.lagFagsak()
    }

    companion object {
        private const val BEHANDLING_ID = 11L
        private val PERIODE_IDER = listOf(2L, 3L, 5L)
        private const val BASE_URL = "/api/behandlinger"
        private val BEHANDLINGSRESULTAT = Behandlingsresultat()
        private val MULIGE_STATUSER = setOf(AVVENT_DOK_PART, AVVENT_DOK_UTL, UNDER_BEHANDLING, AVVENT_FAGLIG_AVKLARING)
    }
}
