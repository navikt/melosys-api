package no.nav.melosys.tjenester.gui.fagsaker

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.FagsakTestFactory.BEHANDLING_ID
import no.nav.melosys.domain.FagsakTestFactory.BRUKER_AKTØR_ID
import no.nav.melosys.domain.FagsakTestFactory.ORGNR
import no.nav.melosys.domain.FagsakTestFactory.SAKSNUMMER
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
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDate
import java.util.*

@WebMvcTest(controllers = [FagsakController::class])
internal class FagsakControllerTest {
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
    private val BASE_URL = "/api/fagsaker"
    private val FOM: LocalDate = LocalDate.now()
    private val TOM: LocalDate = LocalDate.now()
    private val MOTTAKSDATO: LocalDate = LocalDate.now()
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
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns fagsak

        val expectedResponse = lagFagsakDto(fagsak)
        mockMvc.perform(
            MockMvcRequestBuilders.get("$BASE_URL/{saksnr}", SAKSNUMMER)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath<String>("saksnummer", Matchers.equalTo(expectedResponse.saksnummer)))
            .andExpect(MockMvcResultMatchers.jsonPath<Long>("gsakSaksnummer", Matchers.equalTo(expectedResponse.gsakSaksnummer)))
            .andExpect(MockMvcResultMatchers.jsonPath<String>("sakstema.kode", Matchers.equalTo(expectedResponse.sakstema.kode)))
            .andExpect(MockMvcResultMatchers.jsonPath<String>("sakstype.kode", Matchers.equalTo(expectedResponse.sakstype.kode)))
            .andExpect(MockMvcResultMatchers.jsonPath<String>("saksstatus.kode", Matchers.equalTo(expectedResponse.saksstatus.kode)))
            .andExpect(MockMvcResultMatchers.jsonPath<String>("registrertDato", Matchers.equalTo(expectedResponse.registrertDato.toString())))
            .andExpect(MockMvcResultMatchers.jsonPath<String>("endretDato", Matchers.equalTo(expectedResponse.endretDato.toString())))
            .andExpect(MockMvcResultMatchers.jsonPath<String>("hovedpartRolle", Matchers.equalTo(expectedResponse.hovedpartRolle.toString())))
    }

    @Test
    fun opprettFagsak() {
        val opprettSakDto = OpprettSakDto().apply {
            brukerID = BRUKER_AKTØR_ID
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
            brukerID = BRUKER_AKTØR_ID
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
        val behandling = Behandling().apply {
            this.fagsak = fagsak
            this.id = 123L
        }
        fagsak.leggTilBehandling(behandling)
        mockFagsakController(fagsak, null)
        val fagsakSokDto = FagsakSokDto(BRUKER_AKTØR_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.post("$BASE_URL/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath<String>("$[0].hovedpartRolle", Matchers.equalTo(Aktoersroller.BRUKER.toString())))
            .andExpect(MockMvcResultMatchers.jsonPath<String>("$[0].saksnummer", Matchers.equalTo(SAKSNUMMER)))
    }

    @Test
    fun hentFagsaker_medBehandlingsresultatOgLovvalgsperiode_verifiserErMappetKorrekt() {
        val behandlingID = 123L

        mockFagsakMedBehandling(behandlingID)

        val fagsakSokDto = FagsakSokDto(BRUKER_AKTØR_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.post("$BASE_URL/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath<String>("$[0].hovedpartRolle", Matchers.equalTo(Aktoersroller.BRUKER.toString())))
            .andExpect(MockMvcResultMatchers.jsonPath<String>("$[0].saksnummer", Matchers.equalTo(SAKSNUMMER)))
            .andExpect(MockMvcResultMatchers.jsonPath<String>("$[0].behandlingOversikter[0].land.landkoder[0]", Matchers.equalTo(Landkoder.DK.kode)))
            .andExpect(
                MockMvcResultMatchers.jsonPath<String>(
                    "$[0].behandlingOversikter[0].land.landkoder[0]",
                    Matchers.not(Matchers.equalTo(FORVENTET_LOVVALGSPERIODE.lovvalgsland))
                )
            )
            .andExpect(MockMvcResultMatchers.jsonPath<String>("$[0].behandlingOversikter[0].soknadsperiode.fom", Matchers.equalTo("2019-01-01")))
            .andExpect(MockMvcResultMatchers.jsonPath<String>("$[0].behandlingOversikter[0].soknadsperiode.tom", Matchers.equalTo("2019-02-01")))
            .andExpect(
                MockMvcResultMatchers.jsonPath<String>(
                    "$[0].behandlingOversikter[0].lovvalgsperiode.fom",
                    Matchers.equalTo(FORVENTET_LOVVALGSPERIODE.periode.fom.toString())
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String>(
                    "$[0].behandlingOversikter[0].lovvalgsperiode.tom",
                    Matchers.equalTo(FORVENTET_LOVVALGSPERIODE.periode.tom.toString())
                )
            )
            .andExpect(MockMvcResultMatchers.jsonPath<Any?>("$[0].behandlingOversikter[0].medlemskapsperiode.fom", Matchers.equalTo(null)))
            .andExpect(MockMvcResultMatchers.jsonPath<Any?>("$[0].behandlingOversikter[0].medlemskapsperiode.tom", Matchers.equalTo(null)))
    }

    @Test
    fun hentFagsaker_medMedlemAvFolketrygdenOgMedlemskapsperioder_verifiserErMappetKorrekt() {
        val behandlingID = 123L

        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        val behandling = Behandling()
        behandling.fagsak = fagsak
        behandling.id = behandlingID
        fagsak.leggTilBehandling(behandling)
        val behandlingsresultat = Behandlingsresultat()
        val medlemskapsperiode = Medlemskapsperiode()
        medlemskapsperiode.fom = FOM
        medlemskapsperiode.tom = TOM
        medlemskapsperiode.innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        behandlingsresultat.medlemskapsperioder = listOf(medlemskapsperiode)
        mockFagsakController(fagsak, behandlingsresultat)

        val fagsakSokDto = FagsakSokDto(BRUKER_AKTØR_ID, null, null)
        mockMvc.perform(
            MockMvcRequestBuilders.post("$BASE_URL/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath<String>("$[0].hovedpartRolle", Matchers.equalTo(Aktoersroller.BRUKER.toString())))
            .andExpect(MockMvcResultMatchers.jsonPath<String>("$[0].saksnummer", Matchers.equalTo(SAKSNUMMER)))
            .andExpect(MockMvcResultMatchers.jsonPath<String>("$[0].behandlingOversikter[0].land.landkoder[0]", Matchers.equalTo(Landkoder.DK.kode)))
            .andExpect(
                MockMvcResultMatchers.jsonPath<String>(
                    "$[0].behandlingOversikter[0].land.landkoder[0]",
                    Matchers.not(Matchers.equalTo(FORVENTET_LOVVALGSPERIODE.lovvalgsland))
                )
            )
            .andExpect(MockMvcResultMatchers.jsonPath<String>("$[0].behandlingOversikter[0].soknadsperiode.fom", Matchers.equalTo("2019-01-01")))
            .andExpect(MockMvcResultMatchers.jsonPath<String>("$[0].behandlingOversikter[0].soknadsperiode.tom", Matchers.equalTo("2019-02-01")))
            .andExpect(MockMvcResultMatchers.jsonPath<Any?>("$[0].behandlingOversikter[0].lovvalgsperiode", Matchers.equalTo(null)))
            .andExpect(
                MockMvcResultMatchers.jsonPath<String>(
                    "$[0].behandlingOversikter[0].medlemskapsperiode.fom",
                    Matchers.equalTo(FOM.toString())
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String>(
                    "$[0].behandlingOversikter[0].medlemskapsperiode.tom",
                    Matchers.equalTo(TOM.toString())
                )
            )
    }

    @Test
    fun hentFagsaker_medTomtFnr_verifiserAtNavnErUkjent() {
        val brukerUtenFnr = Aktoer()
        brukerUtenFnr.rolle = Aktoersroller.BRUKER
        val fagsak = FagsakTestFactory.builder().aktører(brukerUtenFnr).build()
        val behandling = Behandling()
        behandling.id = 123L
        behandling.fagsak = fagsak
        fagsak.leggTilBehandling(behandling)
        mockFagsakController(fagsak, null)
        val fagsakSokDto = FagsakSokDto(BRUKER_AKTØR_ID, null, null)

        mockMvc.perform(
            MockMvcRequestBuilders.post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].hovedpartRolle",
                    Matchers.equalTo<String?>(Aktoersroller.BRUKER.toString())
                )
            )
            .andExpect(MockMvcResultMatchers.jsonPath<String?>("$[0].navn", Matchers.equalTo<String?>("UKJENT")))
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].saksnummer",
                    Matchers.equalTo<String?>(SAKSNUMMER)
                )
            )
    }

    @Test
    fun hentFagsaker_medOrgnr_verifiserErMappetKorrekt() {
        val fagsak = FagsakTestFactory.builder().medVirksomhet().build()
        val behandling = Behandling()
        behandling.id = 123L
        behandling.fagsak = fagsak
        fagsak.leggTilBehandling(behandling)
        mockFagsakController(fagsak, null)

        val organisajonsdokument = OrganisasjonDokumentTestFactory.builder()
            .navn("Moe Organisasjon")
            .build()

        every { organisasjonOppslagService.hentOrganisasjon(ORGNR) } returns organisajonsdokument
        val fagsakSokDto = FagsakSokDto(null, null, ORGNR)

        mockMvc.perform(
            MockMvcRequestBuilders.post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].hovedpartRolle",
                    Matchers.equalTo<String?>(Aktoersroller.VIRKSOMHET.toString())
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].navn",
                    Matchers.equalTo<String?>("Moe Organisasjon")
                )
            )
    }

    @Test
    fun hentFagsaker_medTomtOrgnr_verifiserAtNavnErUkjent() {
        val aktoer = Aktoer()
        aktoer.rolle = Aktoersroller.VIRKSOMHET
        val fagsak = FagsakTestFactory.builder().aktører(aktoer).build()
        val behandling = Behandling()
        behandling.id = 123L
        behandling.fagsak = fagsak
        fagsak.leggTilBehandling(behandling)
        mockFagsakController(fagsak, null)
        val fagsakSokDto = FagsakSokDto(null, null, ORGNR)

        mockMvc.perform(
            MockMvcRequestBuilders.post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].hovedpartRolle",
                    Matchers.equalTo<String?>(Aktoersroller.VIRKSOMHET.toString())
                )
            )
            .andExpect(MockMvcResultMatchers.jsonPath<String?>("$[0].navn", Matchers.equalTo<String?>("UKJENT")))
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
            .andExpect(MockMvcResultMatchers.jsonPath<Int?>("$.length()", Matchers.equalTo<Int?>(0)))
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
            MockMvcRequestBuilders.put(BASE_URL + "/{saksnr}", SAKSNUMMER)
                .content(objectMapper.writeValueAsString(endreSakDto))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent())

        verify { aksesskontroll.autoriserSakstilgang(SAKSNUMMER) }
        verify {
            endreSakService.endre(
                SAKSNUMMER,
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
            Behandlingstema.YRKESAKTIV, Behandlingstyper.ÅRSAVREGNING, Behandlingsstatus.UNDER_BEHANDLING, MOTTAKSDATO
        )

        mockMvc.perform(
            MockMvcRequestBuilders.put(BASE_URL + "/{saksnr}", SAKSNUMMER)
                .content(objectMapper.writeValueAsString(endreSakDto))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent())
        verify { aksesskontroll.autoriserSakstilgang(SAKSNUMMER) }
        verify { endreSakService.endreÅrsavregningBehandling(BEHANDLING_ID, Behandlingsstatus.UNDER_BEHANDLING, MOTTAKSDATO) }
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
        val søknadDokument = SaksbehandlingDataFactory.lagSøknadDokument()
        val mottatteOpplysninger = MottatteOpplysninger()
        mottatteOpplysninger.mottatteOpplysningerData = søknadDokument
        val nyttBehres = Behandlingsresultat()
        nyttBehres.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        nyttBehres.lovvalgsperioder.add(lagLovvalgsPeriode())
        val behandlingsresultat = eksisterendeBehres ?: nyttBehres

        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingsresultat
        every { behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(any()) } returns behandlingsresultat
        every { mottatteOpplysningerService.finnMottatteOpplysninger(fagsak.behandlinger[0].id) } returns Optional.of(mottatteOpplysninger)
        every { fagsakService.hentFagsak(SAKSNUMMER) } returns fagsak
        every { persondataFasade.hentSammensattNavn(any()) } returns "Joe Moe"
        every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, BRUKER_AKTØR_ID) } returns listOf(fagsak)
        every { fagsakService.hentFagsakerMedOrgnr(Aktoersroller.VIRKSOMHET, ORGNR) } returns listOf(fagsak)
    }

    private fun mockFagsakMedBehandling(behandlingID: Long) {
        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        val behandling = Behandling()
        behandling.fagsak = fagsak
        behandling.id = behandlingID
        fagsak.leggTilBehandling(behandling)
        mockFagsakController(fagsak, null)
    }

    private fun lagLovvalgsPeriode(): Lovvalgsperiode {
        val lovvalgsperiode = Lovvalgsperiode()
        lovvalgsperiode.fom = FORVENTET_LOVVALGSPERIODE.periode.fom
        lovvalgsperiode.tom = FORVENTET_LOVVALGSPERIODE.periode.tom
        lovvalgsperiode.dekning = Trygdedekninger.FULL_DEKNING_EOSFO
        lovvalgsperiode.lovvalgsland = Land_iso2.valueOf(FORVENTET_LOVVALGSPERIODE.lovvalgsland)
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.valueOf(FORVENTET_LOVVALGSPERIODE.lovvalgsbestemmelse)
        lovvalgsperiode.tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.valueOf(FORVENTET_LOVVALGSPERIODE.tilleggBestemmelse)
        lovvalgsperiode.innvilgelsesresultat = InnvilgelsesResultat.valueOf(FORVENTET_LOVVALGSPERIODE.innvilgelsesResultat)
        lovvalgsperiode.medlemskapstype = Medlemskapstyper.valueOf(FORVENTET_LOVVALGSPERIODE.medlemskapstype)
        lovvalgsperiode.medlPeriodeID = FORVENTET_LOVVALGSPERIODE.medlemskapsperiodeID.toLong()

        return lovvalgsperiode
    }

    private fun lagFagsakDto(fagsak: Fagsak): FagsakDto {
        val resultat = FagsakDto()
        resultat.endretDato = fagsak.endretDato
        resultat.gsakSaksnummer = fagsak.gsakSaksnummer
        resultat.registrertDato = fagsak.getRegistrertDato()
        resultat.saksnummer = fagsak.saksnummer
        resultat.sakstema = fagsak.tema
        resultat.sakstype = fagsak.type
        resultat.saksstatus = fagsak.status
        resultat.hovedpartRolle = fagsak.hovedpartRolle
        return resultat
    }
}
