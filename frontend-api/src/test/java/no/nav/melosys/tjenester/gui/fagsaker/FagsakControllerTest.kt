package no.nav.melosys.tjenester.gui.fagsaker

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.melosys.domain.*
import no.nav.melosys.domain.FagsakTestFactory.BEHANDLING_ID
import no.nav.melosys.domain.FagsakTestFactory.BRUKER_AKTØR_ID
import no.nav.melosys.domain.FagsakTestFactory.ORGNR
import no.nav.melosys.domain.FagsakTestFactory.SAKSNUMMER
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.Tilleggsinformasjon
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument
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
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDate
import java.util.*
import java.util.List

@WebMvcTest(controllers = [FagsakController::class])
internal class FagsakControllerTest {
    @Autowired
    private val mockMvc: MockMvc? = null

    @Autowired
    private val objectMapper: ObjectMapper? = null
    private var random: EasyRandom? = null

    @MockBean
    private val fagsakService: FagsakService? = null

    @MockBean
    private val opprettSak: OpprettSak? = null

    @MockBean
    private val endreSakService: EndreSakService? = null

    @MockBean
    private val aksesskontroll: Aksesskontroll? = null

    @MockBean
    private val organisasjonOppslagService: OrganisasjonOppslagService? = null

    @MockBean
    private val persondataFasade: PersondataFasade? = null

    @MockBean
    @Suppress("unused")
    private val saksopplysningerService: SaksopplysningerService? = null

    @MockBean
    private val mottatteOpplysningerService: MottatteOpplysningerService? = null

    @MockBean
    private val behandlingsresultatService: BehandlingsresultatService? = null

    @MockBean
    @Suppress("unused")
    private val opprettBehandlingForSak: OpprettBehandlingForSak? = null

    @MockBean
    private val ferdigbehandleService: FerdigbehandleService? = null

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
                        if (Math.random() > 0.5) random!!.nextObject<MidlertidigPostadresseNorge?>(
                            MidlertidigPostadresseNorge::class.java
                        ) else random!!.nextObject<MidlertidigPostadresseUtland?>(MidlertidigPostadresseUtland::class.java)
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
    @Throws(Exception::class)
    fun hentFagsak() {
        val fagsak = FagsakTestFactory.builder().medBruker().build()
        Mockito.`when`<Fagsak?>(fagsakService!!.hentFagsak(SAKSNUMMER)).thenReturn(fagsak)

        val expectedResponse = lagFagsakDto(fagsak)
        mockMvc!!.perform(
            MockMvcRequestBuilders.get(BASE_URL + "/{saksnr}", SAKSNUMMER)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "saksnummer",
                    Matchers.equalTo<String?>(expectedResponse.getSaksnummer())
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<Long?>(
                    "gsakSaksnummer",
                    Matchers.equalTo<Long?>(expectedResponse.getGsakSaksnummer())
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "sakstema.kode",
                    Matchers.equalTo<String?>(expectedResponse.getSakstema().getKode())
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "sakstype.kode",
                    Matchers.equalTo<String?>(expectedResponse.getSakstype().getKode())
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "saksstatus.kode",
                    Matchers.equalTo<String?>(expectedResponse.getSaksstatus().getKode())
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "registrertDato",
                    Matchers.equalTo<String?>(expectedResponse.getRegistrertDato().toString())
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "endretDato",
                    Matchers.equalTo<String?>(expectedResponse.getEndretDato().toString())
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "hovedpartRolle",
                    Matchers.equalTo<String?>(expectedResponse.getHovedpartRolle().toString())
                )
            )
    }

    @Test
    @Throws(Exception::class)
    fun opprettFagsak() {
        val opprettSakDto = OpprettSakDto()
        opprettSakDto.brukerID = BRUKER_AKTØR_ID

        mockMvc!!.perform(
            MockMvcRequestBuilders.post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper!!.writeValueAsString(opprettSakDto))
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent())
        Mockito.verify<Aksesskontroll?>(aksesskontroll).autoriserFolkeregisterIdent(opprettSakDto.brukerID)
        Mockito.verify<OpprettSak?>(opprettSak)
            .opprettNySakOgBehandling(ArgumentMatchers.any<OpprettSakDto?>(OpprettSakDto::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun opprettSak_utenFnrEllerOrgnr_badRequestException() {
        val opprettSakDto = OpprettSakDto()
        opprettSakDto.hovedpart = Aktoersroller.BRUKER

        mockMvc!!.perform(
            MockMvcRequestBuilders.post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper!!.writeValueAsString(opprettSakDto))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
    }

    @Test
    @Throws(Exception::class)
    fun lagNyBehandling() {
        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        val behandling = Behandling()
        behandling.setFagsak(fagsak)
        behandling.setId(123L)

        fagsak.leggTilBehandling(behandling)
        val opprettSakDto = OpprettSakDto()
        opprettSakDto.brukerID = BRUKER_AKTØR_ID
        opprettSakDto.behandlingstema = Behandlingstema.ARBEID_TJENESTEPERSON_ELLER_FLY
        opprettSakDto.behandlingstype = Behandlingstyper.NY_VURDERING

        mockMvc!!.perform(
            MockMvcRequestBuilders.post(BASE_URL + "/{saksnr}/behandlinger", fagsak.saksnummer)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper!!.writeValueAsString(opprettSakDto))
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent())
        Mockito.verify<Aksesskontroll?>(aksesskontroll).autoriserFolkeregisterIdent(opprettSakDto.brukerID)
    }

    @Test
    @Throws(Exception::class)
    fun hentFagsaker_medFnr_verifiserErMappetKorrekt() {
        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        val behandling = Behandling()
        behandling.setFagsak(fagsak)
        behandling.setId(123L)
        fagsak.leggTilBehandling(behandling)
        mockFagsakController(fagsak, null)
        val fagsakSokDto = FagsakSokDto(BRUKER_AKTØR_ID, null, null)

        mockMvc!!.perform(
            MockMvcRequestBuilders.post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper!!.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].hovedpartRolle",
                    Matchers.equalTo<String?>(Aktoersroller.BRUKER.toString())
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].saksnummer",
                    Matchers.equalTo<String?>(SAKSNUMMER)
                )
            )
    }

    @Test
    @Throws(Exception::class)
    fun hentFagsaker_medBehandlingsresultatOgLovvalgsperiode_verifiserErMappetKorrekt() {
        val behandlingID = 123L

        mockFagsakMedBehandling(behandlingID)

        val fagsakSokDto = FagsakSokDto(BRUKER_AKTØR_ID, null, null)

        mockMvc!!.perform(
            MockMvcRequestBuilders.post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper!!.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].hovedpartRolle",
                    Matchers.equalTo<String?>(Aktoersroller.BRUKER.toString())
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].saksnummer",
                    Matchers.equalTo<String?>(SAKSNUMMER)
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].behandlingOversikter[0].land.landkoder[0]", Matchers.equalTo<String?>(
                        Landkoder.DK.getKode()
                    )
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].behandlingOversikter[0].land.landkoder[0]", Matchers.`is`<String?>(
                        Matchers.not<String?>(Matchers.equalTo<String?>(FORVENTET_LOVVALGSPERIODE.lovvalgsland))
                    )
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].behandlingOversikter[0].soknadsperiode.fom",
                    Matchers.equalTo<String?>("2019-01-01")
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].behandlingOversikter[0].soknadsperiode.tom",
                    Matchers.equalTo<String?>("2019-02-01")
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].behandlingOversikter[0].lovvalgsperiode.fom", Matchers.equalTo<String?>(
                        FORVENTET_LOVVALGSPERIODE.periode.getFom().toString()
                    )
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].behandlingOversikter[0].lovvalgsperiode.tom", Matchers.equalTo<String?>(
                        FORVENTET_LOVVALGSPERIODE.periode.getTom().toString()
                    )
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<Any?>(
                    "$[0].behandlingOversikter[0].medlemskapsperiode.fom",
                    Matchers.equalTo<Any?>(null)
                )
            ) //TODO: Her burde vi kanskje returnere null på medlemskapsperiode?
            .andExpect(
                MockMvcResultMatchers.jsonPath<Any?>(
                    "$[0].behandlingOversikter[0].medlemskapsperiode.tom",
                    Matchers.equalTo<Any?>(null)
                )
            ) //TODO: Her burde vi kanskje returnere null på medlemskapsperiode?
    }

    @Test
    @Throws(Exception::class)
    fun hentFagsaker_medMedlemAvFolketrygdenOgMedlemskapsperioder_verifiserErMappetKorrekt() {
        val behandlingID = 123L

        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        val behandling = Behandling()
        behandling.setFagsak(fagsak)
        behandling.setId(behandlingID)
        fagsak.leggTilBehandling(behandling)
        val behandlingsresultat = Behandlingsresultat()
        val medlemskapsperiode = Medlemskapsperiode()
        medlemskapsperiode.setFom(FOM)
        medlemskapsperiode.setTom(TOM)
        medlemskapsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET)
        behandlingsresultat.setMedlemskapsperioder(List.of<Medlemskapsperiode?>(medlemskapsperiode))
        mockFagsakController(fagsak, behandlingsresultat)

        val fagsakSokDto = FagsakSokDto(BRUKER_AKTØR_ID, null, null)

        mockMvc!!.perform(
            MockMvcRequestBuilders.post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper!!.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].hovedpartRolle",
                    Matchers.equalTo<String?>(Aktoersroller.BRUKER.toString())
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].saksnummer",
                    Matchers.equalTo<String?>(SAKSNUMMER)
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].behandlingOversikter[0].land.landkoder[0]", Matchers.equalTo<String?>(
                        Landkoder.DK.getKode()
                    )
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].behandlingOversikter[0].land.landkoder[0]", Matchers.`is`<String?>(
                        Matchers.not<String?>(Matchers.equalTo<String?>(FORVENTET_LOVVALGSPERIODE.lovvalgsland))
                    )
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].behandlingOversikter[0].soknadsperiode.fom",
                    Matchers.equalTo<String?>("2019-01-01")
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].behandlingOversikter[0].soknadsperiode.tom",
                    Matchers.equalTo<String?>("2019-02-01")
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<Any?>(
                    "$[0].behandlingOversikter[0].lovvalgsperiode",
                    Matchers.equalTo<Any?>(null)
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].behandlingOversikter[0].medlemskapsperiode.fom", Matchers.equalTo<String?>(
                        FOM.toString()
                    )
                )
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath<String?>(
                    "$[0].behandlingOversikter[0].medlemskapsperiode.tom", Matchers.equalTo<String?>(
                        TOM.toString()
                    )
                )
            )
    }

    @Test
    @Throws(Exception::class)
    fun hentFagsaker_medTomtFnr_verifiserAtNavnErUkjent() {
        val brukerUtenFnr = Aktoer()
        brukerUtenFnr.setRolle(Aktoersroller.BRUKER)
        val fagsak = FagsakTestFactory.builder().aktører(brukerUtenFnr).build()
        val behandling = Behandling()
        behandling.setId(123L)
        behandling.setFagsak(fagsak)
        fagsak.leggTilBehandling(behandling)
        mockFagsakController(fagsak, null)
        val fagsakSokDto = FagsakSokDto(BRUKER_AKTØR_ID, null, null)

        mockMvc!!.perform(
            MockMvcRequestBuilders.post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper!!.writeValueAsString(fagsakSokDto))
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
    @Throws(Exception::class)
    fun hentFagsaker_medOrgnr_verifiserErMappetKorrekt() {
        val fagsak = FagsakTestFactory.builder().medVirksomhet().build()
        val behandling = Behandling()
        behandling.setId(123L)
        behandling.setFagsak(fagsak)
        fagsak.leggTilBehandling(behandling)
        mockFagsakController(fagsak, null)

        val organisajonsdokument = OrganisasjonDokumentTestFactory.builder()
            .navn("Moe Organisasjon")
            .build()
        Mockito.`when`<OrganisasjonDokument?>(organisasjonOppslagService!!.hentOrganisasjon(ORGNR))
            .thenReturn(organisajonsdokument)
        val fagsakSokDto = FagsakSokDto(null, null, ORGNR)

        mockMvc!!.perform(
            MockMvcRequestBuilders.post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper!!.writeValueAsString(fagsakSokDto))
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
    @Throws(Exception::class)
    fun hentFagsaker_medTomtOrgnr_verifiserAtNavnErUkjent() {
        val aktoer = Aktoer()
        aktoer.setRolle(Aktoersroller.VIRKSOMHET)
        val fagsak = FagsakTestFactory.builder().aktører(aktoer).build()
        val behandling = Behandling()
        behandling.setId(123L)
        behandling.setFagsak(fagsak)
        fagsak.leggTilBehandling(behandling)
        mockFagsakController(fagsak, null)
        val fagsakSokDto = FagsakSokDto(null, null, ORGNR)

        mockMvc!!.perform(
            MockMvcRequestBuilders.post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper!!.writeValueAsString(fagsakSokDto))
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
    @Throws(Exception::class)
    fun hentFagsaker_medSaksnummer_finnerIkkeSakMottarTomListe() {
        val fagsakSokDto = FagsakSokDto(null, "NEI-123", null)

        mockMvc!!.perform(
            MockMvcRequestBuilders.post(BASE_URL + "/sok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper!!.writeValueAsString(fagsakSokDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath<Int?>("$.length()", Matchers.equalTo<Int?>(0)))
    }

    @Test
    @Throws(Exception::class)
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

        mockMvc!!.perform(
            MockMvcRequestBuilders.put(BASE_URL + "/{saksnr}", SAKSNUMMER)
                .content(objectMapper!!.writeValueAsString(endreSakDto))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent())

        Mockito.verify<Aksesskontroll?>(aksesskontroll).autoriserSakstilgang(SAKSNUMMER)
        Mockito.verify<EndreSakService?>(endreSakService).endre(
            SAKSNUMMER,
            Sakstyper.TRYGDEAVTALE,
            Sakstemaer.UNNTAK,
            Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET,
            Behandlingstyper.NY_VURDERING,
            Behandlingsstatus.OPPRETTET,
            null
        )
    }

    @Test
    @Throws(Exception::class)
    fun endreÅrsavregningOppsummering() {
        val endreSakDto = EndreSakDto(
            BEHANDLING_ID, Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG,
            Behandlingstema.YRKESAKTIV, Behandlingstyper.ÅRSAVREGNING, Behandlingsstatus.UNDER_BEHANDLING, MOTTAKSDATO
        )

        mockMvc!!.perform(
            MockMvcRequestBuilders.put(BASE_URL + "/{saksnr}", SAKSNUMMER)
                .content(objectMapper!!.writeValueAsString(endreSakDto))
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent())

        Mockito.verify<Aksesskontroll?>(aksesskontroll).autoriserSakstilgang(SAKSNUMMER)
        Mockito.verify<EndreSakService?>(endreSakService)
            .endreÅrsavregningBehandling(BEHANDLING_ID, Behandlingsstatus.UNDER_BEHANDLING, MOTTAKSDATO)
    }

    @Test
    @Throws(Exception::class)
    fun ferdigbehandleSak() {
        mockMvc!!.perform(
            MockMvcRequestBuilders.put(BASE_URL + "/{behandlingID}/ferdigbehandle", BEHANDLING_ID)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isNoContent())

        Mockito.verify<Aksesskontroll?>(aksesskontroll).autoriserSkriv(BEHANDLING_ID)
        Mockito.verify<FerdigbehandleService?>(ferdigbehandleService).ferdigbehandle(BEHANDLING_ID)
    }

    private fun mockFagsakController(fagsak: Fagsak, eksisterendeBehres: Behandlingsresultat?) {
        val søknadDokument = SaksbehandlingDataFactory.lagSøknadDokument()
        val mottatteOpplysninger = MottatteOpplysninger()
        mottatteOpplysninger.setMottatteOpplysningerData(søknadDokument)
        val nyttBehres = Behandlingsresultat()
        nyttBehres.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND)
        nyttBehres.getLovvalgsperioder().add(lagLovvalgsPeriode())
        val behandlingsresultat = if (eksisterendeBehres == null) nyttBehres else eksisterendeBehres

        Mockito.`when`<Behandlingsresultat?>(behandlingsresultatService!!.hentBehandlingsresultat(ArgumentMatchers.anyLong()))
            .thenReturn(behandlingsresultat)
        Mockito.`when`<Behandlingsresultat?>(
            behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(
                ArgumentMatchers.anyLong()
            )
        ).thenReturn(behandlingsresultat)
        Mockito.`when`<Optional<MottatteOpplysninger>?>(
            mottatteOpplysningerService!!.finnMottatteOpplysninger(
                fagsak.behandlinger.get(
                    0
                ).getId()
            )
        ).thenReturn(
            Optional.of<MottatteOpplysninger>(mottatteOpplysninger)
        )
        Mockito.`when`<Fagsak?>(fagsakService!!.hentFagsak(SAKSNUMMER)).thenReturn(fagsak)
        Mockito.`when`<String?>(persondataFasade!!.hentSammensattNavn(ArgumentMatchers.any<String?>()))
            .thenReturn("Joe Moe")
        Mockito.doReturn(List.of<Fagsak?>(fagsak)).`when`<FagsakService?>(fagsakService)
            .hentFagsakerMedAktør(Aktoersroller.BRUKER, BRUKER_AKTØR_ID)
        Mockito.doReturn(List.of<Fagsak?>(fagsak)).`when`<FagsakService?>(fagsakService)
            .hentFagsakerMedOrgnr(Aktoersroller.VIRKSOMHET, ORGNR)
    }

    private fun mockFagsakMedBehandling(behandlingID: Long) {
        val fagsak = SaksbehandlingDataFactory.lagFagsak()
        val behandling = Behandling()
        behandling.setFagsak(fagsak)
        behandling.setId(behandlingID)
        fagsak.leggTilBehandling(behandling)
        mockFagsakController(fagsak, null)
    }

    private fun lagLovvalgsPeriode(): Lovvalgsperiode {
        val lovvalgsperiode = Lovvalgsperiode()
        lovvalgsperiode.setFom(FORVENTET_LOVVALGSPERIODE.periode.getFom())
        lovvalgsperiode.setTom(FORVENTET_LOVVALGSPERIODE.periode.getTom())
        lovvalgsperiode.setDekning(Trygdedekninger.FULL_DEKNING_EOSFO)
        lovvalgsperiode.setLovvalgsland(Land_iso2.valueOf(FORVENTET_LOVVALGSPERIODE.lovvalgsland))
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.valueOf(FORVENTET_LOVVALGSPERIODE.lovvalgsbestemmelse))
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.valueOf(FORVENTET_LOVVALGSPERIODE.tilleggBestemmelse))
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.valueOf(FORVENTET_LOVVALGSPERIODE.innvilgelsesResultat))
        lovvalgsperiode.setMedlemskapstype(Medlemskapstyper.valueOf(FORVENTET_LOVVALGSPERIODE.medlemskapstype))
        lovvalgsperiode.setMedlPeriodeID(FORVENTET_LOVVALGSPERIODE.medlemskapsperiodeID.toLong())

        return lovvalgsperiode
    }

    private fun lagFagsakDto(fagsak: Fagsak): FagsakDto {
        val resultat = FagsakDto()
        resultat.setEndretDato(fagsak.getEndretDato())
        resultat.setGsakSaksnummer(fagsak.gsakSaksnummer)
        resultat.setRegistrertDato(fagsak.getRegistrertDato())
        resultat.setSaksnummer(fagsak.saksnummer)
        resultat.setSakstema(fagsak.tema)
        resultat.setSakstype(fagsak.type)
        resultat.setSaksstatus(fagsak.status)
        resultat.setHovedpartRolle(fagsak.hovedpartRolle)
        return resultat
    }
}
