package no.nav.melosys.tjenester.gui.fagsaker

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.FagsakTestFactory.BEHANDLING_ID
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseNorge
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseUtland
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.ÅRSAVREGNING
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.registeropplysninger.OrganisasjonOppslagService
import no.nav.melosys.service.sak.*
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.FagsakDto
import no.nav.melosys.tjenester.gui.dto.FagsakSokDto
import no.nav.melosys.tjenester.gui.dto.periode.LovvalgsperiodeDto
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto
import no.nav.melosys.tjenester.gui.util.NumericStringRandomizer
import no.nav.melosys.tjenester.gui.util.SaksbehandlingDataFactory
import org.hamcrest.Matchers
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates
import org.jeasy.random.api.Randomizer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import java.time.Instant
import java.time.LocalDate
import java.util.*

@WebMvcTest(controllers = [FagsakController::class])
internal class FagsakControllerTest {
    companion object {
        const val BASE_URL = "/api/fagsaker"
        private val FOM = LocalDate.now()
        private val TOM = LocalDate.now()
        private val MOTTAKSDATO = LocalDate.now()
        private val FORVENTET_LOVVALGSPERIODE = LovvalgsperiodeDto(
            "1L", PeriodeDto(FOM, TOM),
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2,
            Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_4_1,
            Land_iso2.SK,
            InnvilgelsesResultat.AVSLAATT,
            Trygdedekninger.FULL_DEKNING_EOSFO,
            Medlemskapstyper.FRIVILLIG,
            "10"
        )
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var random: EasyRandom

    @MockkBean(relaxed = true)
    lateinit var fagsakService: FagsakService

    @MockkBean(relaxed = true)
    lateinit var opprettSak: OpprettSak

    @MockkBean(relaxed = true)
    lateinit var endreSakService: EndreSakService

    @MockkBean(relaxed = true)
    lateinit var aksesskontroll: Aksesskontroll

    @MockkBean(relaxed = true)
    lateinit var organisasjonOppslagService: OrganisasjonOppslagService

    @MockkBean(relaxed = true)
    lateinit var persondataFasade: PersondataFasade

    @MockkBean(relaxed = true)
    lateinit var saksopplysningerService: SaksopplysningerService

    @MockkBean(relaxed = true)
    lateinit var mottatteOpplysningerService: MottatteOpplysningerService

    @MockkBean(relaxed = true)
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockkBean(relaxed = true)
    lateinit var opprettBehandlingForSak: OpprettBehandlingForSak

    @MockkBean(relaxed = true)
    lateinit var ferdigbehandleService: FerdigbehandleService


    @BeforeEach
    fun setUp() {
        random = EasyRandom(
            EasyRandomParameters()
                .overrideDefaultInitialization(true)
                .collectionSizeRange(1, 4)
                .objectPoolSize(100)
                .dateRange(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))
                .excludeField(
                    FieldPredicates.named("tilleggsinformasjonDetaljer").and(
                        FieldPredicates.ofType(
                            TilleggsinformasjonDetaljer::class.java
                        )
                    ).and(FieldPredicates.inClass(Tilleggsinformasjon::class.java))
                )
                .stringLengthRange(2, 10)
                .randomize<MidlertidigPostadresse?>(
                    MidlertidigPostadresse::class.java,
                    Randomizer {
                        if (Math.random() > 0.5) random.nextObject<MidlertidigPostadresseNorge?>(
                            MidlertidigPostadresseNorge::class.java
                        ) else random.nextObject<MidlertidigPostadresseUtland?>(MidlertidigPostadresseUtland::class.java)
                    })
                .randomize<String?>(
                    FieldPredicates.named("fnr").and(FieldPredicates.ofType(String::class.java)),
                    NumericStringRandomizer(11)
                )
                .randomize<String?>(
                    FieldPredicates.named("orgnummer").and(FieldPredicates.ofType(String::class.java)),
                    NumericStringRandomizer(9)
                )
        )
    }


    @Test
    fun hentFagsak() {
        val fagsak = FagsakTestFactory.builder().medBruker().build()
        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak

        val expectedResponse = lagFagsakDto(fagsak)
        mockMvc.perform(
            MockMvcRequestBuilders.get("$BASE_URL/{saksnr}", FagsakTestFactory.SAKSNUMMER)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonPath<String>("saksnummer", Matchers.equalTo(expectedResponse.saksnummer)))
            .andExpect(jsonPath<Long>("gsakSaksnummer", Matchers.equalTo(expectedResponse.gsakSaksnummer)))
            .andExpect(jsonPath<String>("sakstema.kode", Matchers.equalTo(expectedResponse.sakstema.kode)))
            .andExpect(jsonPath<String>("sakstype.kode", Matchers.equalTo(expectedResponse.sakstype.kode)))
            .andExpect(jsonPath<String>("saksstatus.kode", Matchers.equalTo(expectedResponse.saksstatus.kode)))
            .andExpect(jsonPath<String>("registrertDato", Matchers.equalTo(expectedResponse.registrertDato.toString())))
            .andExpect(jsonPath<String>("endretDato", Matchers.equalTo(expectedResponse.endretDato.toString())))
            .andExpect(jsonPath<String>("hovedpartRolle", Matchers.equalTo(expectedResponse.hovedpartRolle.toString())))
    }

    @Test
    fun opprettFagsak() {
        val opprettSakDto = OpprettSakDto().apply {
            brukerID = FagsakTestFactory.BRUKER_AKTØR_ID
        }

        mockMvc.perform(
            MockMvcRequestBuilders.post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(opprettSakDto))
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent())

        verify { aksesskontroll.autoriserFolkeregisterIdent(opprettSakDto.brukerID) }
        verify { opprettSak.opprettNySakOgBehandling(any<OpprettSakDto>()) }
    }

    @Test
    fun opprettSak_utenFnrEllerOrgnr_badRequestException() {
        val opprettSakDto = OpprettSakDto()
        opprettSakDto.hovedpart = Aktoersroller.BRUKER

        mockMvc.perform(
            MockMvcRequestBuilders.post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(opprettSakDto))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
    }

    @Test
    fun lagNyBehandling() {
        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        val behandling = Behandling().apply {
            this.fagsak = fagsak
            this.id = 123L
        }
        fagsak.leggTilBehandling(behandling)

        val opprettSakDto = OpprettSakDto().apply {
            brukerID = FagsakTestFactory.BRUKER_AKTØR_ID
            behandlingstema = Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY
            behandlingstype = Behandlingstyper.NY_VURDERING
        }

        mockMvc.perform(
            MockMvcRequestBuilders.post("$BASE_URL/{saksnr}/behandlinger", fagsak.saksnummer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(opprettSakDto))
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent())

        verify { aksesskontroll.autoriserFolkeregisterIdent(opprettSakDto.brukerID) }
    }

    @Test
    fun hentFagsaker_medFnr_verifiserErMappetKorrekt() {
        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        lagBehandling { this.fagsak = fagsak }

        mockFagsakController(fagsak, null)
        val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.post("$BASE_URL/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonPath<String>("$[0].hovedpartRolle", Matchers.equalTo(Aktoersroller.BRUKER.toString())))
            .andExpect(jsonPath<String>("$[0].saksnummer", Matchers.equalTo(FagsakTestFactory.SAKSNUMMER)))
    }

    @Test
    fun hentFagsaker_medBehandlingsresultatOgLovvalgsperiode_verifiserErMappetKorrekt() {
        val behandlingID = 123L

        mockFagsakMedBehandling(behandlingID)

        val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.post("$BASE_URL/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonPath<String>("$[0].hovedpartRolle", Matchers.equalTo(Aktoersroller.BRUKER.toString())))
            .andExpect(jsonPath<String>("$[0].saksnummer", Matchers.equalTo(FagsakTestFactory.SAKSNUMMER)))
            .andExpect(jsonPath<String>("$[0].behandlingOversikter[0].soknadsperiode.fom", Matchers.equalTo("2019-01-01")))
            .andExpect(jsonPath<String>("$[0].behandlingOversikter[0].soknadsperiode.tom", Matchers.equalTo("2019-02-01")))
    }

    @Test
    fun hentFagsaker_medMedlemAvFolketrygdenOgMedlemskapsperioder_verifiserErMappetKorrekt() {
        val fagsak = SaksbehandlingDataFactory.lagFagsak()

        lagBehandling { this.fagsak = fagsak }

        val medlemskapsperiode = Medlemskapsperiode().apply {
            this.fom = FOM
            this.tom = TOM
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }

        val behandlingsresultat = Behandlingsresultat().apply {
            this.id = 123
            this.medlemskapsperioder = listOf(medlemskapsperiode)
            this.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        }

        mockFagsakController(fagsak, behandlingsresultat)

        val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)
        mockMvc.perform(
            MockMvcRequestBuilders.post("$BASE_URL/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonPath<String>("$[0].hovedpartRolle", Matchers.equalTo(Aktoersroller.BRUKER.toString())))
            .andExpect(jsonPath<String>("$[0].saksnummer", Matchers.equalTo(FagsakTestFactory.SAKSNUMMER)))
            .andExpect(jsonPath<String>("$[0].behandlingOversikter[0].soknadsperiode.fom", Matchers.equalTo("2019-01-01")))
            .andExpect(jsonPath<String>("$[0].behandlingOversikter[0].soknadsperiode.tom", Matchers.equalTo("2019-02-01")))
    }

    @Test
    fun hentFagsaker_medTomtFnr_verifiserAtNavnErUkjent() {
        val brukerUtenFnr = Aktoer()
        brukerUtenFnr.rolle = Aktoersroller.BRUKER
        val fagsak = FagsakTestFactory.builder().aktører(brukerUtenFnr).build()

        lagBehandling { this.fagsak = fagsak }
        mockFagsakController(fagsak, null)
        val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                jsonPath<String?>(
                    "$[0].hovedpartRolle",
                    Matchers.equalTo<String?>(Aktoersroller.BRUKER.toString())
                )
            )
            .andExpect(jsonPath<String?>("$[0].navn", Matchers.equalTo<String?>("UKJENT")))
            .andExpect(
                jsonPath<String?>(
                    "$[0].saksnummer",
                    Matchers.equalTo<String?>(FagsakTestFactory.SAKSNUMMER)
                )
            )
    }

    @Test
    fun hentFagsaker_medOrgnr_verifiserErMappetKorrekt() {
        val fagsak = FagsakTestFactory.builder().medVirksomhet().build()
        lagBehandling { this.fagsak = fagsak }
        mockFagsakController(fagsak, null)

        val organisajonsdokument = OrganisasjonDokumentTestFactory.builder()
            .navn("Moe Organisasjon")
            .build()

        every { organisasjonOppslagService.hentOrganisasjon(FagsakTestFactory.ORGNR) } returns organisajonsdokument
        val fagsakSokDto = FagsakSokDto(null, null, FagsakTestFactory.ORGNR)

        mockMvc.perform(
            MockMvcRequestBuilders.post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                jsonPath<String?>(
                    "$[0].hovedpartRolle",
                    Matchers.equalTo<String?>(Aktoersroller.VIRKSOMHET.toString())
                )
            )
            .andExpect(
                jsonPath<String?>(
                    "$[0].navn",
                    Matchers.equalTo<String?>("Moe Organisasjon")
                )
            )
    }

    @Test
    fun hentFagsaker_verifiserAtLandSettesPaaFagsak() {
        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        lagBehandling { this.fagsak = fagsak }

        mockFagsakController(fagsak, null)
        val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.post("$BASE_URL/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                jsonPath<String>(
                    "$[0].land.landkoder[0]", Matchers.equalTo(
                        Landkoder.DK.kode
                    )
                )
            )
    }

    @Test
    fun hentFagsaker_med_kun_aarsavregning_verifiserAtTomtLandSettesPaaFagsak() {
        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        lagBehandling {
            this.fagsak = fagsak
            this.type = ÅRSAVREGNING
        }

        val behandlingsresultat = Behandlingsresultat().apply {
            this.id = 123
            this.årsavregning = Årsavregning().apply { aar = 2024 }
            this.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            this.lovvalgsperioder.add(lagLovvalgsPeriode())
        }

        mockFagsakController(fagsak, behandlingsresultat)
        val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.post("$BASE_URL/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonPath("$[0].land.landkoder").isArray)
            .andExpect(jsonPath("$[0].land.landkoder").isEmpty)
    }


    @ParameterizedTest
    @EnumSource(Sakstyper::class, names = ["FTRL", "EU_EOS"])
    fun hentFagsaker_verifiserAtPeriodeSettesPaaFagsak(sakstype: Sakstyper) {
        val fagsak = SaksbehandlingDataFactory.lagFagsak().apply {
            type = sakstype
        }
        lagBehandling { this.fagsak = fagsak }

        mockFagsakController(fagsak, null)
        val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.post("$BASE_URL/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonPath<String>("$[0].periode.fom", Matchers.equalTo(FOM.toString())))
            .andExpect(
                jsonPath<String>("$[0].periode.tom", Matchers.equalTo(TOM.toString()))
            )
    }

    @Test
    fun hentFagsaker_med_forstegangsbehandlingMedVedtak_nyvurderingUtenVedtak_benytter_forstegangsbehandling_for_grunnlag() {
        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        val førstegangsbehandling = lagBehandling {
            id = 123
            this.fagsak = fagsak
            this.type = Behandlingstyper.FØRSTEGANG
        }
        val nyvurdering = lagBehandling {
            id = 124
            this.fagsak = fagsak
            this.type = Behandlingstyper.NY_VURDERING
        }

        fagsak.leggTilBehandling(førstegangsbehandling)
        fagsak.leggTilBehandling(nyvurdering)

        mockFagsakController(fagsak, null)

        val nyvurderingPeriode = Periode(LocalDate.of(2030, 1, 1), LocalDate.of(2030, 2, 1))
        val nyVurderingBehandlingsresultat = Behandlingsresultat().apply {
            this.id = 124
            this.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            this.lovvalgsperioder.add(lagLovvalgsPeriode().apply {
                fom = nyvurderingPeriode.fom
                tom = nyvurderingPeriode.tom
            })
        }
        mockBehandlingsresultat(nyVurderingBehandlingsresultat)

        val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.post("$BASE_URL/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonPath<String>("$[0].periode.fom", Matchers.equalTo(FORVENTET_LOVVALGSPERIODE.periode.fom.toString())))
            .andExpect(
                jsonPath<String>("$[0].periode.tom", Matchers.equalTo(FORVENTET_LOVVALGSPERIODE.periode.tom.toString()))
            )
    }

    @Test
    fun hentFagsaker_verifiserAtNyVurderingMedVedtakBehandling_benyttes_for_periode() {
        val fagsak = SaksbehandlingDataFactory.lagFagsak().apply {
            type = Sakstyper.FTRL
        }

        val førstegangsbehandling = lagBehandling {
            this.fagsak = fagsak
        }

        val nyVurdering = lagBehandling {
            this.id = 124
            this.fagsak = fagsak
            this.type = Behandlingstyper.NY_VURDERING
        }

        val årsavregning = lagBehandling {
            this.id = 125
            this.fagsak = fagsak
            this.type = ÅRSAVREGNING
        }
        val årsavregningPeriode = Periode(LocalDate.of(2040, 1, 1), LocalDate.of(2040, 2, 1))

        val årsavregningBehandlingsresultat = Behandlingsresultat().apply {
            this.id = 124
            this.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            this.lovvalgsperioder.add(lagLovvalgsPeriode().apply {
                fom = årsavregningPeriode.fom
                tom = årsavregningPeriode.tom
            })
            this.medlemskapsperioder.add(lagMedlemskapsPeriode().apply {
                fom = årsavregningPeriode.fom
                tom = årsavregningPeriode.tom
            })
            this.vedtakMetadata = VedtakMetadata()
        }
        mockBehandlingsresultat(årsavregningBehandlingsresultat)


        val nyvurderingPeriode = Periode(LocalDate.of(2030, 1, 1), LocalDate.of(2030, 2, 1))
        val nyVurderingBehandlingsresultat = Behandlingsresultat().apply {
            this.id = 124
            this.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            this.lovvalgsperioder.add(lagLovvalgsPeriode().apply {
                fom = nyvurderingPeriode.fom
                tom = nyvurderingPeriode.tom
            })
            this.medlemskapsperioder.add(lagMedlemskapsPeriode().apply {
                fom = nyvurderingPeriode.fom
                tom = nyvurderingPeriode.tom
            })
            this.vedtakMetadata = VedtakMetadata()
        }
        mockBehandlingsresultat(nyVurderingBehandlingsresultat)

        fagsak.leggTilBehandling(førstegangsbehandling)
        fagsak.leggTilBehandling(nyVurdering)
        fagsak.leggTilBehandling(årsavregning)

        mockFagsakController(fagsak, null)

        val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.post("$BASE_URL/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonPath<String>("$[0].periode.fom", Matchers.equalTo(nyvurderingPeriode.fom.toString())))
            .andExpect(
                jsonPath<String>("$[0].periode.tom", Matchers.equalTo(nyvurderingPeriode.tom.toString()))
            )
    }

    @Test
    fun hentFagsaker_verifiserAtTittelSettesPaaFagsakBehandling() {
        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        lagBehandling { this.fagsak = fagsak }

        mockFagsakController(fagsak, null)
        val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.post("$BASE_URL/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonPath<String>("$[0].behandlingOversikter[0].tittel", Matchers.equalTo("Yrkesaktiv - Førstegangsbehandling")))
    }

    @Test
    fun hentFagsaker_NårBehandlingErÅrsavregningVerifiserAtTittelSettesPaaFagsakBehandling() {
        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        lagBehandling {
            this.fagsak = fagsak
            this.type = ÅRSAVREGNING
        }

        val behandlingsresultat = Behandlingsresultat().apply {
            this.id = 123
            this.årsavregning = Årsavregning().apply { aar = 2024 }
            this.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            this.lovvalgsperioder.add(lagLovvalgsPeriode())
        }

        mockFagsakController(fagsak, behandlingsresultat)

        val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.post("$BASE_URL/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonPath<String>("$[0].behandlingOversikter[0].tittel", Matchers.equalTo("Yrkesaktiv - Årsavregning 2024")))
    }

    @Test
    fun hentFagsaker_medTomtOrgnr_verifiserAtNavnErUkjent() {
        val aktoer = Aktoer()
        aktoer.rolle = Aktoersroller.VIRKSOMHET
        val fagsak = FagsakTestFactory.builder().aktører(aktoer).build()
        val behandling = Behandling().apply {
            this.id = 123L
            this.fagsak = fagsak
            this.tema = Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY
            this.type = Behandlingstyper.NY_VURDERING
            status = Behandlingsstatus.OPPRETTET
            registrertDato = Instant.now()
        }

        fagsak.leggTilBehandling(behandling)
        mockFagsakController(fagsak, null)
        val fagsakSokDto = FagsakSokDto(null, null, FagsakTestFactory.ORGNR)

        mockMvc.perform(
            MockMvcRequestBuilders.post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                jsonPath<String?>(
                    "$[0].hovedpartRolle",
                    Matchers.equalTo<String?>(Aktoersroller.VIRKSOMHET.toString())
                )
            )
            .andExpect(jsonPath<String?>("$[0].navn", Matchers.equalTo<String?>("UKJENT")))
    }

    @Test
    fun hentFagsaker_medSaksnummer_finnerIkkeSakMottarTomListe() {
        val fagsakSokDto = FagsakSokDto(null, "NEI-123", null)

        mockMvc.perform(
            MockMvcRequestBuilders.post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(jsonPath<Int?>("$.length()", Matchers.equalTo<Int?>(0)))
    }

    @Test
    fun endreSak() {
        val endreSakDto = EndreSakDto(
            null,
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.UNNTAK,
            Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET,
            Behandlingstyper.NY_VURDERING,
            Behandlingsstatus.OPPRETTET,
            null
        )

        mockMvc.perform(
            MockMvcRequestBuilders.put(BASE_URL + "/{saksnr}", FagsakTestFactory.SAKSNUMMER)
                .content(objectMapper.writeValueAsString(endreSakDto))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent())

        verify { aksesskontroll.autoriserSakstilgang(FagsakTestFactory.SAKSNUMMER) }
        verify {
            endreSakService.endre(
                FagsakTestFactory.SAKSNUMMER,
                Sakstyper.TRYGDEAVTALE,
                Sakstemaer.UNNTAK,
                Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET,
                Behandlingstyper.NY_VURDERING,
                Behandlingsstatus.OPPRETTET,
                null
            )
        }
    }

    @Test
    fun endreÅrsavregningOppsummering() {
        val endreSakDto = EndreSakDto(
            BEHANDLING_ID, Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.YRKESAKTIV, ÅRSAVREGNING, Behandlingsstatus.UNDER_BEHANDLING, MOTTAKSDATO
        )

        mockMvc.perform(
            MockMvcRequestBuilders.put(BASE_URL + "/{saksnr}", FagsakTestFactory.SAKSNUMMER)
                .content(objectMapper.writeValueAsString(endreSakDto))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent())
        verify { aksesskontroll.autoriserSakstilgang(FagsakTestFactory.SAKSNUMMER) }
        verify {
            endreSakService.endreÅrsavregningBehandling(
                BEHANDLING_ID,
                Behandlingsstatus.UNDER_BEHANDLING,
                MOTTAKSDATO
            )
        }
    }

    @Test
    fun ferdigbehandleSak() {
        mockMvc.perform(
            MockMvcRequestBuilders.put("$BASE_URL/{behandlingID}/ferdigbehandle", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent())

        verify { aksesskontroll.autoriserSkriv(BEHANDLING_ID) }
        verify { ferdigbehandleService.ferdigbehandle(BEHANDLING_ID) }
    }

    private fun mockFagsakController(fagsak: Fagsak, eksisterendeBehres: Behandlingsresultat?) {
        mockBehandlingsresultat(eksisterendeBehres)
        mockMotatteOpplysninger(fagsak.behandlinger[0].id)

        every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak
        every { persondataFasade.hentSammensattNavn(any()) } returns "Joe Moe"
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, FagsakTestFactory.BRUKER_AKTØR_ID) } returns listOf(fagsak)
        every { fagsakService.hentFagsakerMedOrgnr(Aktoersroller.VIRKSOMHET, FagsakTestFactory.ORGNR) } returns listOf(fagsak)
    }

    private fun mockMotatteOpplysninger(behandlingId: Long) {
        val søknadDokument = SaksbehandlingDataFactory.lagSøknadDokument()
        val mottatteOpplysninger = MottatteOpplysninger().apply {
            this.mottatteOpplysningerData = søknadDokument
        }

        every { mottatteOpplysningerService.finnMottatteOpplysninger(behandlingId) } returns Optional.of(mottatteOpplysninger)
    }

    private fun mockBehandlingsresultat(eksisterendeBehres: Behandlingsresultat?) {
        val nyttBehres = Behandlingsresultat().apply {
            this.id = 123
            this.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            this.lovvalgsperioder.add(lagLovvalgsPeriode())
            this.medlemskapsperioder.add(lagMedlemskapsPeriode())
            this.vedtakMetadata = VedtakMetadata()
        }
        val behandlingsresultat = eksisterendeBehres ?: nyttBehres

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingsresultat.id) } returns behandlingsresultat
        every { behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(behandlingsresultat.id) } returns behandlingsresultat
    }

    private fun mockFagsakMedBehandling(behandlingID: Long) {
        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        val behandling = Behandling().apply {
            this.id = behandlingID
            this.fagsak = fagsak
            this.tema = Behandlingstema.YRKESAKTIV
            this.type = Behandlingstyper.FØRSTEGANG
            this.status = Behandlingsstatus.OPPRETTET
            this.registrertDato = Instant.now()
        }

        fagsak.leggTilBehandling(behandling)
        mockFagsakController(fagsak, null)
    }

    private fun lagLovvalgsPeriode() = Lovvalgsperiode().apply {
        fom = FORVENTET_LOVVALGSPERIODE.periode.fom
        tom = FORVENTET_LOVVALGSPERIODE.periode.tom
        dekning = Trygdedekninger.FULL_DEKNING_EOSFO
        lovvalgsland = Land_iso2.valueOf(FORVENTET_LOVVALGSPERIODE.lovvalgsland)
        bestemmelse = Lovvalgbestemmelser_883_2004.valueOf(FORVENTET_LOVVALGSPERIODE.lovvalgsbestemmelse)
        tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.valueOf(FORVENTET_LOVVALGSPERIODE.tilleggBestemmelse)
        innvilgelsesresultat = InnvilgelsesResultat.valueOf(FORVENTET_LOVVALGSPERIODE.innvilgelsesResultat)
        medlemskapstype = Medlemskapstyper.valueOf(FORVENTET_LOVVALGSPERIODE.medlemskapstype)
        medlPeriodeID = FORVENTET_LOVVALGSPERIODE.medlemskapsperiodeID.toLong()
    }

    private fun lagMedlemskapsPeriode() = Medlemskapsperiode().apply {
        fom = FORVENTET_LOVVALGSPERIODE.periode.fom
        tom = FORVENTET_LOVVALGSPERIODE.periode.tom
        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
    }

    private fun lagFagsakDto(fagsak: Fagsak) = FagsakDto().apply {
        endretDato = fagsak.endretDato
        gsakSaksnummer = fagsak.gsakSaksnummer
        registrertDato = fagsak.getRegistrertDato()
        saksnummer = fagsak.saksnummer
        sakstema = fagsak.tema
        sakstype = fagsak.type
        saksstatus = fagsak.status
        hovedpartRolle = fagsak.hovedpartRolle
    }

    private fun lagBehandling(block: Behandling.() -> Unit = {}) = Behandling().apply {
        this.id = 123L
        this.fagsak = fagsak
        this.tema = Behandlingstema.YRKESAKTIV
        this.type = Behandlingstyper.FØRSTEGANG
        status = Behandlingsstatus.OPPRETTET
        registrertDato = Instant.now()
        block()
        this.fagsak.leggTilBehandling(this)
    }
}
