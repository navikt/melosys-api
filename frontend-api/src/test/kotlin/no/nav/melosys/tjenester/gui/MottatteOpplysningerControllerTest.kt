package no.nav.melosys.tjenester.gui

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.kodeverk.Flyvningstyper
import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.mottatteopplysninger.PeriodeOgLandPostDto
import no.nav.melosys.tjenester.gui.util.NumericStringRandomizer
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates.named
import org.jeasy.random.FieldPredicates.ofType
import org.jeasy.random.randomizers.misc.EnumRandomizer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.util.*

@WebMvcTest(controllers = [MottatteOpplysningerController::class])
class MottatteOpplysningerControllerTest {

    @MockkBean
    private lateinit var mottatteOpplysningerService: MottatteOpplysningerService

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var random: EasyRandom

    @BeforeEach
    fun setup() {
        random = EasyRandom(EasyRandomParameters()
            .overrideDefaultInitialization(true)
            .collectionSizeRange(1, 4)
            .randomize(GeografiskAdresse::class.java) { EasyRandom().nextObject(SemistrukturertAdresse::class.java) }
            .stringLengthRange(2, 10)
            .randomize(named("fnr").and(ofType(String::class.java)), NumericStringRandomizer(11))
            .randomize(named("orgnr").and(ofType(String::class.java)), NumericStringRandomizer(9))
            .randomize(named("orgnummer").and(ofType(String::class.java)), NumericStringRandomizer(9))
            .randomize(named("typeFlyvninger")) { EnumRandomizer(Flyvningstyper::class.java).randomValue }
            .randomize(named("uuid")) { UUID.randomUUID().toString() }
        )
    }

    @Test
    fun `skal hente eller opprette mottatte opplysninger`() {
        val soeknad = random.nextObject(Soeknad::class.java)
        val mottatteOpplysninger = MottatteOpplysninger().apply {
            type = Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS
            mottatteOpplysningerData = soeknad
        }
        every { mottatteOpplysningerService.hentEllerOpprettMottatteOpplysninger(any(), any()) } returns mottatteOpplysninger
        every { aksesskontroll.autoriser(BEHANDLING_ID) } returns Unit
        every { aksesskontroll.behandlingKanRedigeresAvSaksbehandler(any()) } returns true


        mockMvc.perform(
            get("$BASE_URL/{behandlingID}", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk())


        verify { mottatteOpplysningerService.hentEllerOpprettMottatteOpplysninger(any(), any()) }
    }

    @Test
    fun `skal oppdatere mottatte opplysninger`() {
        val soeknad = random.nextObject(Soeknad::class.java)
        val mottatteOpplysninger = MottatteOpplysninger().apply {
            type = Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS
            mottatteOpplysningerData = soeknad
        }
        every { mottatteOpplysningerService.oppdaterMottatteOpplysninger(any(), any()) } returns mottatteOpplysninger
        every { aksesskontroll.autoriser(BEHANDLING_ID) } returns Unit
        every { aksesskontroll.behandlingKanRedigeresAvSaksbehandler(any()) } returns true
        every { aksesskontroll.autoriserSkriv(BEHANDLING_ID) } returns Unit
        every { aksesskontroll.auditAutoriserSkriv(any(), any()) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(soeknad))
        ).andExpect(status().isOk())


        verify { mottatteOpplysningerService.oppdaterMottatteOpplysninger(any(), any()) }
    }

    @Test
    fun `skal oppdatere mottatte opplysninger periode og land`() {
        val periodeOgLandPostDto = PeriodeOgLandPostDto(LocalDate.now(), LocalDate.now().plusYears(1), listOf("Denmark", "Sweden"))
        every { aksesskontroll.autoriser(BEHANDLING_ID) } returns Unit
        every { aksesskontroll.behandlingKanRedigeresAvSaksbehandler(any()) } returns true
        every { aksesskontroll.autoriserSkriv(BEHANDLING_ID) } returns Unit
        every { aksesskontroll.auditAutoriserSkriv(any(), any()) } returns Unit
        every { mottatteOpplysningerService.oppdaterMottatteOpplysningerPeriodeOgLand(any(), any(), any()) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/{behandlingID}/periodeOgLand", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(periodeOgLandPostDto))
        ).andExpect(status().isNoContent())


        verify { mottatteOpplysningerService.oppdaterMottatteOpplysningerPeriodeOgLand(any(), any(), any()) }
    }

    companion object {
        private const val BASE_URL = "/api/mottatteopplysninger"
        private const val BEHANDLING_ID = 1L
    }
}
