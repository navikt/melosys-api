package no.nav.melosys.tjenester.gui

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Trygdeavtale_myndighetsland
import no.nav.melosys.domain.kodeverk.begrunnelser.Nyvurderingbakgrunner
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_barn_begrunnelser_ftrl
import no.nav.melosys.domain.kodeverk.begrunnelser.folketrygdloven.Medfolgende_ektefelle_samboer_begrunnelser_ftrl
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie.Relasjonsrolle
import no.nav.melosys.domain.mottatteopplysninger.data.MedfolgendeFamilie.tilMedfolgendeFamilie
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.person.familie.AvklarteMedfolgendeFamilie
import no.nav.melosys.domain.person.familie.IkkeOmfattetFamilie
import no.nav.melosys.domain.person.familie.OmfattetFamilie
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.service.trygdeavtale.TrygdeavtaleResultat
import no.nav.melosys.service.trygdeavtale.TrygdeavtaleService
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import no.nav.melosys.tjenester.gui.dto.trygdeavtale.TrygdeavtaleInfoDto
import no.nav.melosys.tjenester.gui.dto.trygdeavtale.TrygdeavtaleResultatDto
import no.nav.melosys.tjenester.gui.util.responseBody
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.*

@WebMvcTest(controllers = [TrygdeavtaleController::class])
class TrygdeavtaleControllerTest {

    @MockkBean
    private lateinit var trygdeavtaleService: TrygdeavtaleService

    @MockkBean
    private lateinit var behandlingService: BehandlingService

    @MockkBean
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockkBean
    private lateinit var aksesskontroll: Aksesskontroll

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val trygdeavtaleResultatSlot = slot<TrygdeavtaleResultat>()

    private lateinit var behandling: Behandling
    private lateinit var behandlingsresultat: Behandlingsresultat

    @BeforeEach
    fun setup() {
        SpringSubjectHandler.set(TestSubjectHandler())
        behandling = lagBehandling()
        behandlingsresultat = lagBehandlingsresultat()
    }

    @Test
    fun `overførResultat med TrygdeavtaleResultatDto mappes korrekt`() {
        val trygdeavtaleResultatDto = lagTrygdeavtaleResultatDto()
        every { aksesskontroll.autoriser(1L) } returns Unit
        every { aksesskontroll.autoriserSkriv(1L) } returns Unit
        every { trygdeavtaleService.overførResultat(1L, any()) } returns Unit


        mockMvc.perform(
            post("$BASE_URL/resultat/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(trygdeavtaleResultatDto))
        )
            .andExpect(status().isNoContent)


        verify { trygdeavtaleService.overførResultat(1L, capture(trygdeavtaleResultatSlot)) }
        val trygdeavtaleResultat = trygdeavtaleResultatSlot.captured

        trygdeavtaleResultat.run {
            shouldNotBeNull()
            virksomhet() shouldBe trygdeavtaleResultatDto.virksomhet()
            bestemmelse() shouldBe trygdeavtaleResultatDto.bestemmelse()
            lovvalgsperiodeFom() shouldBe trygdeavtaleResultatDto.lovvalgsperiodeFom()
            lovvalgsperiodeTom() shouldBe trygdeavtaleResultatDto.lovvalgsperiodeTom()
        }
        trygdeavtaleResultat.familie().familieIkkeOmfattetAvNorskTrygd.run {
            shouldHaveSize(2)
            val ektefelle = first { it.uuid == UUID_EKTEFELLE }
            val barn1 = first { it.uuid == UUID_BARN_1 }
            ektefelle.run {
                begrunnelse shouldBe Medfolgende_ektefelle_samboer_begrunnelser_ftrl.EGEN_INNTEKT.kode
                begrunnelseFritekst shouldBe BEGRUNNELSE_SAMBOER
            }
            barn1.run {
                begrunnelse shouldBe Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR.kode
                begrunnelseFritekst shouldBe BEGRUNNELSE_BARN
            }
        }
        trygdeavtaleResultat.familie().familieOmfattetAvNorskTrygd.run {
            shouldHaveSize(1)
            first().uuid shouldBe UUID_BARN_2
        }
    }

    @Test
    fun `hentTrygdeavtaleInfo uten virksomhet og barn ektefelle returnerer korrekt`() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(1L) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { aksesskontroll.autoriser(1L) } returns Unit
        every { aksesskontroll.behandlingKanRedigeresAvSaksbehandler(any(), any()) } returns false


        val mvcResult = mockMvc.perform(
            get("$BASE_URL/mottatteopplysninger/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .requestAttr("virksomheter", false)
                .requestAttr("barnEktefeller", false)
        )
            .andExpect(status().isOk)
            .andReturn()


        val body = mvcResult.response.getContentAsString(StandardCharsets.UTF_8)
        val response = objectMapper.readValue(body, TrygdeavtaleInfoDto::class.java)

        verify(exactly = 0) { trygdeavtaleService.hentVirksomheter(any()) }
        verify(exactly = 0) { trygdeavtaleService.hentFamiliemedlemmer(any()) }

        response.shouldNotBeNull().run {
            aktoerId() shouldBe behandling.fagsak.hentBrukersAktørID()
            behandlingstema() shouldBe behandling.tema.kode
            behandlingstype() shouldBe behandling.type.kode
            redigerbart() shouldBe false
            behandling.mottatteOpplysninger.shouldNotBeNull().mottatteOpplysningerData.run {
                periodeFom() shouldBe periode.fom
                periodeTom() shouldBe periode.tom
            }
            soeknadsland() shouldBe Trygdeavtale_myndighetsland.GB
            innledningFritekst() shouldBe behandlingsresultat.innledningFritekst
            begrunnelseFritekst() shouldBe behandlingsresultat.begrunnelseFritekst
            nyVurderingBakgrunn().shouldBeNull()
        }
    }

    @Test
    fun `hentTrygdeavtaleInfo med virksomhet og barn ektefelle returnerer korrekt`() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(1L) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { trygdeavtaleService.hentVirksomheter(any()) } returns emptyMap()
        every { trygdeavtaleService.hentFamiliemedlemmer(any()) } returns emptyList()
        every { aksesskontroll.autoriser(1L) } returns Unit
        every { aksesskontroll.behandlingKanRedigeresAvSaksbehandler(any(), any()) } returns false


        val mvcResult = mockMvc.perform(
            get("$BASE_URL/mottatteopplysninger/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .param("virksomheter", "true")
                .param("barnEktefeller", "true")
        )
            .andExpect(status().isOk)
            .andReturn()


        val body = mvcResult.response.getContentAsString(StandardCharsets.UTF_8)
        val response = objectMapper.readValue(body, TrygdeavtaleInfoDto::class.java)

        verify { trygdeavtaleService.hentVirksomheter(any()) }
        verify { trygdeavtaleService.hentFamiliemedlemmer(any()) }

        response.shouldNotBeNull().run {
            aktoerId() shouldBe behandling.fagsak.hentBrukersAktørID()
            behandlingstema() shouldBe behandling.tema.kode
            behandlingstype() shouldBe behandling.type.kode
            redigerbart() shouldBe false
            val mottatteOpplysningerData = behandling.mottatteOpplysninger!!.mottatteOpplysningerData
            periodeFom() shouldBe mottatteOpplysningerData.periode.fom
            periodeTom() shouldBe mottatteOpplysningerData.periode.tom
            soeknadsland() shouldBe Trygdeavtale_myndighetsland.GB
            innledningFritekst() shouldBe behandlingsresultat.innledningFritekst
            begrunnelseFritekst() shouldBe behandlingsresultat.begrunnelseFritekst
            nyVurderingBakgrunn().shouldBeNull()
        }
    }

    @Test
    fun `hentTrygdeavtaleInfo som ny vurdering returnerer korrekt`() {
        behandlingsresultat.nyVurderingBakgrunn = Nyvurderingbakgrunner.NYE_OPPLYSNINGER.kode
        behandling.type = Behandlingstyper.NY_VURDERING

        every { behandlingService.hentBehandlingMedSaksopplysninger(1L) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { aksesskontroll.autoriser(1L) } returns Unit
        every { aksesskontroll.behandlingKanRedigeresAvSaksbehandler(any(), any()) } returns false


        val mvcResult = mockMvc.perform(
            get("$BASE_URL/mottatteopplysninger/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .requestAttr("virksomheter", true)
                .requestAttr("barnEktefeller", true)
        )
            .andExpect(status().isOk)
            .andReturn()


        val body = mvcResult.response.getContentAsString()
        val response = objectMapper.readValue(body, TrygdeavtaleInfoDto::class.java)

        response.run {
            shouldNotBeNull()
            behandlingstype() shouldBe Behandlingstyper.NY_VURDERING.kode
            nyVurderingBakgrunn() shouldBe behandlingsresultat.nyVurderingBakgrunn
        }
    }

    @Test
    fun `hentTrygdeavtaleInfo saksbehandler har tillatelse til å redigere returnerer korrekt`() {
        every { behandlingService.hentBehandlingMedSaksopplysninger(1L) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { aksesskontroll.behandlingKanRedigeresAvSaksbehandler(behandling, any()) } returns true
        every { aksesskontroll.autoriser(1L) } returns Unit


        val mvcResult = mockMvc.perform(
            get("$BASE_URL/mottatteopplysninger/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .requestAttr("virksomheter", true)
                .requestAttr("barnEktefeller", true)
        )
            .andExpect(status().isOk)
            .andReturn()


        val body = mvcResult.response.getContentAsString()
        val response = objectMapper.readValue(body, TrygdeavtaleInfoDto::class.java)

        response.run {
            shouldNotBeNull()
            redigerbart() shouldBe true
        }
    }

    @Test
    fun `hentResultat bygg opp resultat returnerer korrekt`() {
        val behandling = lagBehandling()
        behandling.mottatteOpplysninger!!.mottatteOpplysningerData.personOpplysninger.medfolgendeFamilie = listOf(
            tilMedfolgendeFamilie(UUID_EKTEFELLE, EKTEFELLE_FNR, EKTEFELLE_NAVN, Relasjonsrolle.EKTEFELLE_SAMBOER),
            tilMedfolgendeFamilie(UUID_BARN_1, BARN1_FNR, BARN_NAVN_1, Relasjonsrolle.BARN),
            tilMedfolgendeFamilie(UUID_BARN_2, BARN2_FNR, BARN_NAVN_2, Relasjonsrolle.BARN)
        )

        every { behandlingService.hentBehandlingMedSaksopplysninger(1L) } returns behandling
        every { trygdeavtaleService.hentResultat(1L) } returns lagTrygdeavtaleResultat()
        every { aksesskontroll.autoriser(1L) } returns Unit


        mockMvc.perform(
            get("$BASE_URL/resultat/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(responseBody(objectMapper).containsObjectAsJson(lagTrygdeavtaleResultatDto(), TrygdeavtaleResultatDto::class.java))
    }

    @Test
    fun `hentResultat tomt resultat returnerer korrekt`() {
        val behandling = lagBehandling()
        behandling.mottatteOpplysninger!!.mottatteOpplysningerData.personOpplysninger.medfolgendeFamilie = emptyList()

        val tomtTrygdeavtaleResultat = TrygdeavtaleResultat
            .Builder().familie(AvklarteMedfolgendeFamilie(emptySet(), emptySet())).build()

        every { behandlingService.hentBehandlingMedSaksopplysninger(1L) } returns behandling
        every { trygdeavtaleService.hentResultat(1L) } returns tomtTrygdeavtaleResultat
        every { aksesskontroll.autoriser(1L) } returns Unit


        mockMvc.perform(
            get("$BASE_URL/resultat/{behandlingID}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(
                responseBody(objectMapper).containsObjectAsJson(
                    TrygdeavtaleResultatDto.Builder().build(),
                    TrygdeavtaleResultatDto::class.java
                )
            )
    }

    private fun lagMottatteOpplysninger(): MottatteOpplysninger {
        val mottatteOpplysningerData = MottatteOpplysningerData().apply {
            soeknadsland.landkoder.add(Landkoder.GB.kode)
            periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1))
        }
        return MottatteOpplysninger().apply {
            this.mottatteOpplysningerData = mottatteOpplysningerData
        }
    }

    private fun lagBehandling(): Behandling = Behandling.forTest {
        fagsak = Fagsak.forTest {
            medBruker()
        }
        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        type = Behandlingstyper.FØRSTEGANG
        mottatteOpplysninger = lagMottatteOpplysninger()
    }

    private fun lagBehandlingsresultat(): Behandlingsresultat = Behandlingsresultat().apply {
        innledningFritekst = "innledningFritekst"
        begrunnelseFritekst = "begrunnelseFritekst"
    }

    private fun lagTrygdeavtaleResultatDto(): TrygdeavtaleResultatDto = TrygdeavtaleResultatDto.Builder()
        .virksomhet(ORGNR_1)
        .bestemmelse(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1.kode)
        .lovvalgsperiodeFom(LocalDate.now())
        .lovvalgsperiodeTom(LocalDate.now().plusYears(1))
        .addBarn(
            UUID_BARN_1,
            false,
            Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR.kode,
            BEGRUNNELSE_BARN
        )
        .addBarn(UUID_BARN_2, true, null, null)
        .ektefelle(
            UUID_EKTEFELLE,
            false,
            Medfolgende_ektefelle_samboer_begrunnelser_ftrl.EGEN_INNTEKT.kode,
            BEGRUNNELSE_SAMBOER
        )
        .build()

    private fun lagTrygdeavtaleResultat(): TrygdeavtaleResultat = TrygdeavtaleResultat
        .Builder()
        .virksomhet(ORGNR_1)
        .lovvalgsperiodeFom(LocalDate.now())
        .lovvalgsperiodeTom(LocalDate.now().plusYears(1))
        .bestemmelse(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART6_1.kode)
        .familie(lagAvklartMedfølgendeBarn()).build()

    private fun lagAvklartMedfølgendeBarn(): AvklarteMedfolgendeFamilie {
        val ektefelle = IkkeOmfattetFamilie(UUID_EKTEFELLE, Medfolgende_ektefelle_samboer_begrunnelser_ftrl.EGEN_INNTEKT.kode, BEGRUNNELSE_SAMBOER)
        val barn1 = IkkeOmfattetFamilie(UUID_BARN_1, Medfolgende_barn_begrunnelser_ftrl.OVER_18_AR.kode, BEGRUNNELSE_BARN).apply {
            ident = BARN1_FNR
        }
        val barn2 = OmfattetFamilie(UUID_BARN_2).apply {
            ident = BARN2_FNR
        }
        return AvklarteMedfolgendeFamilie(
            setOf(barn2), setOf(ektefelle, barn1)
        )
    }

    companion object {
        private const val ORGNR_1 = "11111111111"
        private val UUID_BARN_1 = UUID.randomUUID().toString()
        private val UUID_BARN_2 = UUID.randomUUID().toString()
        private val UUID_EKTEFELLE = UUID.randomUUID().toString()
        private const val BEGRUNNELSE_BARN = "begrunnelse barn"
        private const val BEGRUNNELSE_SAMBOER = "begrunnelse samboer"
        private const val EKTEFELLE_FNR = "01108049800"
        private const val BARN1_FNR = "01100099728"
        private const val BARN2_FNR = "02109049878"
        private const val BARN_NAVN_1 = "Doffen Duck"
        private const val BARN_NAVN_2 = "Dole Duck"
        private const val EKTEFELLE_NAVN = "Dolly Duck"
        private const val BASE_URL = "/api/trygdeavtale"
    }
}
