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
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
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
import org.hamcrest.Matchers.equalTo
import org.jeasy.random.EasyRandom
import org.jeasy.random.EasyRandomParameters
import org.jeasy.random.FieldPredicates
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import java.time.Instant
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
    lateinit var opprettBehandlingForSak: OpprettBehandlingForSak

    @MockkBean(relaxed = true)
    lateinit var mottatteOpplysningerService: MottatteOpplysningerService

    @MockkBean(relaxed = true)
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockkBean(relaxed = true)
    lateinit var ferdigbehandleService: FerdigbehandleService

    lateinit var behandling: Behandling

    lateinit var fagsak: Fagsak

    @BeforeEach
    fun setUp() {
        random = EasyRandom(
            EasyRandomParameters()
                .overrideDefaultInitialization(true)
                .collectionSizeRange(1, 4)
                .objectPoolSize(100)
                .dateRange(LocalDate.now().minusYears(1), LocalDate.now().plusYears(1))
                .excludeField(
                    FieldPredicates.named("tilleggsinformasjonDetaljer")
                        .and(FieldPredicates.ofType(TilleggsinformasjonDetaljer::class.java))
                        .and(FieldPredicates.inClass(Tilleggsinformasjon::class.java))
                )
                .stringLengthRange(2, 10)
                .randomize(
                    MidlertidigPostadresse::class.java
                ) {
                    if (Math.random() > 0.5) random.nextObject(MidlertidigPostadresseNorge::class.java)
                    else random.nextObject(MidlertidigPostadresseUtland::class.java)
                }
                .randomize(
                    FieldPredicates.named("fnr").and(FieldPredicates.ofType(String::class.java)),
                    NumericStringRandomizer(11)
                )
                .randomize(
                    FieldPredicates.named("orgnummer").and(FieldPredicates.ofType(String::class.java)),
                    NumericStringRandomizer(9)
                )
        )

        fagsak = SaksbehandlingDataFactory.lagFagsak()

        behandling = Behandling.forTest {
            id = BEHANDLING_ID
            tema = Behandlingstema.YRKESAKTIV
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.OPPRETTET
            registrertDato = Instant.now()
        }.apply {
            this.fagsak = fagsak
        }

        fagsak.leggTilBehandling(behandling)
    }

    @Nested
    @DisplayName("GET /fagsaker")
    inner class HentFagsak {

        @Test
        fun `skal hente fagsak`() {
            every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak

            val expectedResponse = FagsakDto().apply {
                endretDato = fagsak.endretDato
                gsakSaksnummer = fagsak.gsakSaksnummer
                registrertDato = fagsak.getRegistrertDato()
                saksnummer = fagsak.saksnummer
                sakstema = fagsak.tema
                sakstype = fagsak.type
                saksstatus = fagsak.status
                betalingsvalg = fagsak.betalingsvalg
                hovedpartRolle = fagsak.hovedpartRolle
            }

            mockMvc.perform(
                MockMvcRequestBuilders.get("$BASE_URL/{saksnr}", FagsakTestFactory.SAKSNUMMER)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath<String>("saksnummer", equalTo(expectedResponse.saksnummer)))
                .andExpect(jsonPath("gsakSaksnummer").value(expectedResponse.gsakSaksnummer))
                .andExpect(jsonPath<String>("sakstema.kode", equalTo(expectedResponse.sakstema.kode)))
                .andExpect(jsonPath<String>("sakstype.kode", equalTo(expectedResponse.sakstype.kode)))
                .andExpect(jsonPath<String>("saksstatus.kode", equalTo(expectedResponse.saksstatus.kode)))
                .andExpect(jsonPath("betalingsvalg", equalTo(expectedResponse.betalingsvalg)))
                .andExpect(jsonPath<String>("registrertDato", equalTo(expectedResponse.registrertDato.toString())))
                .andExpect(jsonPath<String>("endretDato", equalTo(expectedResponse.endretDato.toString())))
                .andExpect(jsonPath<String>("hovedpartRolle", equalTo(expectedResponse.hovedpartRolle.toString())))
        }
    }

    @Nested
    @DisplayName("PUT /fagsaker")
    inner class OppdaterFagsaker {
        @Test
        fun `skal endre sak`() {
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
        fun `skal ferdigbehandle sak`() {
            mockMvc.perform(
                MockMvcRequestBuilders.put("$BASE_URL/{behandlingID}/ferdigbehandle", BEHANDLING_ID)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(MockMvcResultMatchers.status().isNoContent())

            verify { aksesskontroll.autoriserSkriv(BEHANDLING_ID) }
            verify { ferdigbehandleService.ferdigbehandle(BEHANDLING_ID) }
        }
    }

    @Nested
    @DisplayName("POST /fagsaker")
    inner class OpprettNySak {

        @Test
        fun `skal opprette fagsak`() {
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
        fun `skal lage ny behandling`() {
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
    }

    @Nested
    @DisplayName("POST /fagsaker/sok")
    inner class SokFagsaker {

        @Test
        fun hentFagsaker_medFnr_verifiserErMappetKorrekt() {
            mockBehandlingsresultat(lagDefaultBehandlingResultat())
            mockFagsakController(fagsak)
            val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

            performSokAndExpectOk(fagsakSokDto)
                .andExpect(jsonPath("$[0].hovedpartRolle", equalTo(Aktoersroller.BRUKER.toString())))
                .andExpect(jsonPath("$[0].saksnummer", equalTo(FagsakTestFactory.SAKSNUMMER)))
        }

        @Test
        fun hentFagsaker_medBehandlingsresultatOgLovvalgsperiode_verifiserErMappetKorrekt() {
            mockBehandlingsresultat(lagDefaultBehandlingResultat())
            mockFagsakController(fagsak)
            mockMotatteOpplysninger(fagsak.behandlinger[0])
            val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

            performSokAndExpectOk(fagsakSokDto)
                .andExpect(jsonPath("$[0].hovedpartRolle", equalTo(Aktoersroller.BRUKER.toString())))
                .andExpect(jsonPath("$[0].saksnummer", equalTo(FagsakTestFactory.SAKSNUMMER)))
                .andExpect(jsonPath("$[0].behandlingOversikter[0].soknadsperiode.fom", equalTo("2019-01-01")))
                .andExpect(jsonPath("$[0].behandlingOversikter[0].soknadsperiode.tom", equalTo("2019-02-01")))
        }

        @Test
        fun hentFagsaker_medMedlemAvFolketrygdenOgMedlemskapsperioder_verifiserErMappetKorrekt() {
            val medlemskapsperiode = Medlemskapsperiode().apply {
                this.fom = FOM
                this.tom = TOM
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            }

            val behandlingsresultat = Behandlingsresultat().apply {
                this.id = BEHANDLING_ID
                this.medlemskapsperioder = listOf(medlemskapsperiode)
                this.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            }

            mockBehandlingsresultat(behandlingsresultat)
            mockFagsakController(fagsak)
            mockMotatteOpplysninger(fagsak.behandlinger[0])

            val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

            performSokAndExpectOk(fagsakSokDto)
                .andExpect(jsonPath("$[0].hovedpartRolle", equalTo(Aktoersroller.BRUKER.toString())))
                .andExpect(jsonPath("$[0].saksnummer", equalTo(FagsakTestFactory.SAKSNUMMER)))
                .andExpect(jsonPath("$[0].behandlingOversikter[0].soknadsperiode.fom", equalTo("2019-01-01")))
                .andExpect(jsonPath("$[0].behandlingOversikter[0].soknadsperiode.tom", equalTo("2019-02-01")))
        }

        @Test
        fun hentFagsaker_medTomtFnr_verifiserAtNavnErUkjent() {
            val brukerUtenFnr = Aktoer()
            brukerUtenFnr.rolle = Aktoersroller.BRUKER
            val fagsak = Fagsak.forTest {
                aktører(brukerUtenFnr)
            }

            mockFagsakController(fagsak)
            val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

            performSokAndExpectOk(fagsakSokDto)
                .andExpect(jsonPath("$[0].hovedpartRolle", equalTo<String?>(Aktoersroller.BRUKER.toString())))
                .andExpect(jsonPath("$[0].navn", equalTo("UKJENT")))
                .andExpect(jsonPath("$[0].saksnummer", equalTo(FagsakTestFactory.SAKSNUMMER)))
        }

        @Test
        fun hentFagsaker_medOrgnr_verifiserErMappetKorrekt() {
            fagsak = Fagsak.forTest {
                behandlinger = fagsak.behandlinger
                medVirksomhet()
            }

            mockBehandlingsresultat(lagDefaultBehandlingResultat())
            mockFagsakController(fagsak)

            val organisajonsdokument = OrganisasjonDokumentTestFactory.builder()
                .navn("Moe Organisasjon")
                .build()

            every { organisasjonOppslagService.hentOrganisasjon(FagsakTestFactory.ORGNR) } returns organisajonsdokument
            val fagsakSokDto = FagsakSokDto(null, null, FagsakTestFactory.ORGNR)

            performSokAndExpectOk(fagsakSokDto)
                .andExpect(jsonPath("$[0].hovedpartRolle", equalTo(Aktoersroller.VIRKSOMHET.toString())))
                .andExpect(jsonPath("$[0].navn", equalTo("Moe Organisasjon")))
        }

        @Test
        fun hentFagsaker_verifiserAtLandSettesPaaFagsak() {
            mockBehandlingsresultat(lagDefaultBehandlingResultat())
            mockFagsakController(fagsak)
            mockMotatteOpplysninger(fagsak.behandlinger[0])

            val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

            performSokAndExpectOk(fagsakSokDto)
                .andExpect(jsonPath("$[0].land.landkoder[0]", equalTo(Landkoder.DK.kode)))
        }

        @Test
        fun hentFagsaker_med_kun_aarsavregning_verifiserAtTomtLandSettesPaaFagsak() {
            val fagsak = SaksbehandlingDataFactory.lagFagsak().apply {
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                type = Sakstyper.FTRL
            }

            val årsavregning = behandling.apply {
                type = ÅRSAVREGNING
                this.fagsak = fagsak
            }

            val behandlingsresultat = Behandlingsresultat().apply {
                this.id = BEHANDLING_ID
                this.årsavregning = Årsavregning.forTest { aar = 2024 }
                this.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
                this.lovvalgsperioder.add(lagDefaultLovvalgsPeriode())
            }

            fagsak.leggTilBehandling(årsavregning)
            mockBehandlingsresultat(behandlingsresultat)
            mockFagsakController(fagsak)
            mockMotatteOpplysninger(fagsak.behandlinger[0])
            val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

            performSokAndExpectOk(fagsakSokDto)
                .andExpect(jsonPath("$[0].land.landkoder").isArray)
                .andExpect(jsonPath("$[0].land.landkoder").isEmpty)
        }

        @ParameterizedTest
        @EnumSource(Sakstyper::class, names = ["FTRL", "EU_EOS"])
        fun hentFagsaker_verifiserAtPeriodeSettesPaaFagsak(sakstype: Sakstyper) {
            fagsak.apply {
                type = sakstype
            }

            mockBehandlingsresultat(lagDefaultBehandlingResultat())
            mockFagsakController(fagsak)
            val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

            performSokAndExpectOk(fagsakSokDto)
                .andExpect(jsonPath("$[0].periode.fom", equalTo(FOM.toString())))
                .andExpect(jsonPath("$[0].periode.tom", equalTo(TOM.toString())))
        }

        @Test
        fun hentFagsaker_med_forstegangsbehandlingMedVedtak_nyvurderingUtenVedtak_benytter_forstegangsbehandling_for_grunnlag() {
            val nyVurderingBehandling = lagNyDefaultBehandling().apply {
                id = 124L
                this.fagsak = fagsak
                type = Behandlingstyper.NY_VURDERING
            }

            mockBehandlingsresultat(lagDefaultBehandlingResultat().apply { id = BEHANDLING_ID })

            mockBehandlingsresultat(lagDefaultBehandlingResultat().apply { id = nyVurderingBehandling.id })
            mockFagsakController(fagsak)

            val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

            performSokAndExpectOk(fagsakSokDto)
                .andExpect(jsonPath("$[0].periode.fom", equalTo(FORVENTET_LOVVALGSPERIODE.periode.fom.toString())))
                .andExpect(jsonPath("$[0].periode.tom", equalTo(FORVENTET_LOVVALGSPERIODE.periode.tom.toString())))
        }

        @Test
        fun hentFagsaker_verifiserAtNyVurderingMedVedtakBehandling_benyttes_for_periode() {
            fagsak.apply {
                type = Sakstyper.FTRL
            }

            val nyVurderingBehandling = lagNyDefaultBehandling().apply {
                id = 124L
                this.fagsak = fagsak
                type = Behandlingstyper.NY_VURDERING
            }

            val årsavregningBehandling = lagNyDefaultBehandling().apply {
                id = 125L
                this.fagsak = fagsak
                type = ÅRSAVREGNING
            }

            fagsak.leggTilBehandling(nyVurderingBehandling)
            fagsak.leggTilBehandling(årsavregningBehandling)

            val nyvurderingPeriode = Periode(LocalDate.of(2030, 1, 1), LocalDate.of(2030, 2, 1))


            val nyVurderingBehandlingsresultat = lagDefaultBehandlingResultat().apply {
                id = nyVurderingBehandling.id
                lovvalgsperioder = setOf(lagDefaultLovvalgsPeriode().apply {
                    fom = nyvurderingPeriode.fom
                    tom = nyvurderingPeriode.tom
                })

                medlemskapsperioder = setOf(lagDefaultMedlemskapsPeriode().apply {
                    fom = nyvurderingPeriode.fom
                    tom = nyvurderingPeriode.tom
                })
                vedtakMetadata = VedtakMetadata()
            }
            val årsavregningPeriode = Periode(LocalDate.of(2040, 1, 1), LocalDate.of(2040, 2, 1))

            val årsavregningBehandlingsresultat = lagDefaultBehandlingResultat().apply {
                id = årsavregningBehandling.id
                this.lovvalgsperioder = setOf(lagDefaultLovvalgsPeriode().apply {
                    fom = årsavregningPeriode.fom
                    tom = årsavregningPeriode.tom
                })
                this.medlemskapsperioder = setOf(lagDefaultMedlemskapsPeriode().apply {
                    fom = årsavregningPeriode.fom
                    tom = årsavregningPeriode.tom
                })
                this.vedtakMetadata = VedtakMetadata()
                this.årsavregning = Årsavregning.forTest { aar = 2024 }
            }

            mockFagsakController(fagsak)
            mockBehandlingsresultat(lagDefaultBehandlingResultat())
            mockBehandlingsresultat(nyVurderingBehandlingsresultat)
            mockBehandlingsresultat(årsavregningBehandlingsresultat)
            val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

            performSokAndExpectOk(fagsakSokDto)
                .andExpect(jsonPath("$[0].periode.fom", equalTo(nyvurderingPeriode.fom.toString())))
                .andExpect(jsonPath("$[0].periode.tom", equalTo(nyvurderingPeriode.tom.toString())))
        }

        @Test
        fun hentFagsaker_verifiserAtTittelSettesPaaFagsakBehandling() {
            mockBehandlingsresultat(lagDefaultBehandlingResultat())
            mockFagsakController(fagsak)
            mockMotatteOpplysninger(behandling)
            val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

            performSokAndExpectOk(fagsakSokDto)
                .andExpect(jsonPath("$[0].behandlingOversikter[0].tittel", equalTo("Førstegangsbehandling")))
        }

        @Test
        fun hentFagsaker_NårBehandlingErÅrsavregningVerifiserAtTittelSettesPaaFagsakBehandling() {
            behandling.apply {
                this.type = ÅRSAVREGNING
            }

            val behandlingsresultat = lagDefaultBehandlingResultat().apply {
                årsavregning = Årsavregning.forTest { aar = 2024 }
            }

            mockBehandlingsresultat(behandlingsresultat)
            mockFagsakController(fagsak)
            val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

            performSokAndExpectOk(fagsakSokDto)
                .andExpect(jsonPath("$[0].behandlingOversikter[0].tittel", equalTo("Årsavregning 2024")))
        }

        @Test
        fun hentFagsaker_medTomtOrgnr_verifiserAtNavnErUkjent() {
            val aktoer = Aktoer()
            aktoer.rolle = Aktoersroller.VIRKSOMHET

            fagsak = Fagsak.forTest {
                aktører(aktoer)
            }

            mockBehandlingsresultat(lagDefaultBehandlingResultat())
            mockFagsakController(fagsak)
            val fagsakSokDto = FagsakSokDto(null, null, FagsakTestFactory.ORGNR)

            performSokAndExpectOk(fagsakSokDto)
                .andExpect(jsonPath("$[0].hovedpartRolle", equalTo(Aktoersroller.VIRKSOMHET.toString())))
                .andExpect(jsonPath("$[0].navn", equalTo("UKJENT")))
        }

        @Test
        fun hentFagsaker_medSaksnummer_finnerIkkeSakMottarTomListe() {
            val fagsakSokDto = FagsakSokDto(null, "NEI-123", null)

            performSokAndExpectOk(fagsakSokDto)
                .andExpect(jsonPath("$.length()", equalTo(0)))
        }

        private fun performSokAndExpectOk(fagsakSokDto: FagsakSokDto): ResultActions {
            return mockMvc.perform(
                MockMvcRequestBuilders.post("$BASE_URL/sok")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(fagsakSokDto))
            )
                .andExpect(MockMvcResultMatchers.status().isOk())
        }

        @Test
        fun `hentFagsaker med eøs pensjonist behandling henter periode og land fra helseutgift dekkes periode`() {
            val fagsak = SaksbehandlingDataFactory.lagFagsak().apply {
                tema = Sakstemaer.TRYGDEAVGIFT
                type = Sakstyper.EU_EOS
            }

            val behandling = behandling.apply {
                status = Behandlingsstatus.AVSLUTTET
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.PENSJONIST
                this.fagsak = fagsak
            }

            fagsak.leggTilBehandling(behandling)
            mockBehandlingsresultat(lagDefaultBehandlingResultatForEøsPensjonist())
            mockFagsakController(fagsak)
            mockMotatteOpplysninger(fagsak.behandlinger[0])

            val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)

            performSokAndExpectOk(fagsakSokDto)
                .andExpect(jsonPath("$[0].land.landkoder[0]", equalTo("BE")))
                .andExpect(jsonPath("$[0].periode.fom", equalTo(FOM.plusDays(1).toString())))
                .andExpect(jsonPath("$[0].periode.tom", equalTo(TOM.plusDays(2).toString())))
        }

        @Test
        fun `hentFagsaker henter lovvalgsperiode og riktig land - MEDLEMSKAP_LOVVALG, EU_EOS`() {
            val fagsak = SaksbehandlingDataFactory.lagFagsak().apply {
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                type = Sakstyper.EU_EOS
            }

            val førstegangsBehandling = behandling.apply {
                status = Behandlingsstatus.AVSLUTTET
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.YRKESAKTIV
                this.fagsak = fagsak
            }

            val behandlingsresultat = lagDefaultBehandlingResultat().apply {
                lovvalgsperioder = setOf(lagDefaultLovvalgsPeriode().apply {
                    fom = FOM
                    tom = TOM
                })
                medlemskapsperioder = setOf(lagDefaultMedlemskapsPeriode().apply {
                    fom = FOM.plusDays(1)
                    tom = TOM.plusDays(2)
                })
            }

            fagsak.leggTilBehandling(førstegangsBehandling)

            mockBehandlingsresultat(behandlingsresultat)
            mockFagsakController(fagsak)
            mockMotatteOpplysninger(fagsak.behandlinger[0])

            val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)


            performSokAndExpectOk(fagsakSokDto)
                .andExpect(jsonPath("$[0].land.landkoder[0]", equalTo("DK")))
                .andExpect(jsonPath("$[0].periode.fom", equalTo(FOM.toString())))
                .andExpect(jsonPath("$[0].periode.tom", equalTo(TOM.toString())))

        }

        @Test
        fun `hentFagsaker henter medlemskapsperiode og riktig land - MEDLEMSKAP_LOVVALG, FTRL`() {
            val fagsak = SaksbehandlingDataFactory.lagFagsak().apply {
                tema = Sakstemaer.MEDLEMSKAP_LOVVALG
                type = Sakstyper.FTRL
            }

            val førstegangsBehandling = behandling.apply {
                status = Behandlingsstatus.AVSLUTTET
                type = Behandlingstyper.FØRSTEGANG
                tema = Behandlingstema.YRKESAKTIV
                this.fagsak = fagsak
            }

            val behandlingsresultat = lagDefaultBehandlingResultat().apply {
                lovvalgsperioder = setOf(lagDefaultLovvalgsPeriode().apply {
                    fom = FOM
                    tom = TOM
                })
                medlemskapsperioder = setOf(lagDefaultMedlemskapsPeriode().apply {
                    fom = FOM.plusDays(1)
                    tom = TOM.plusDays(2)
                })
            }

            fagsak.leggTilBehandling(førstegangsBehandling)

            mockBehandlingsresultat(behandlingsresultat)
            mockFagsakController(fagsak)
            mockMotatteOpplysninger(fagsak.behandlinger[0])

            val fagsakSokDto = FagsakSokDto(FagsakTestFactory.BRUKER_AKTØR_ID, null, null)


            performSokAndExpectOk(fagsakSokDto)
                .andExpect(jsonPath("$[0].land.landkoder[0]", equalTo("DK")))
                .andExpect(jsonPath("$[0].periode.fom", equalTo(FOM.plusDays(1).toString())))
                .andExpect(jsonPath("$[0].periode.tom", equalTo(TOM.plusDays(2).toString())))

        }

        private fun mockFagsakController(fagsak: Fagsak) {
            every { fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER) } returns fagsak
            every { persondataFasade.hentSammensattNavn(any()) } returns "Joe Moe"
            every { fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, FagsakTestFactory.BRUKER_AKTØR_ID) } returns listOf(fagsak)
            every { fagsakService.hentFagsakerMedOrgnr(Aktoersroller.VIRKSOMHET, FagsakTestFactory.ORGNR) } returns listOf(fagsak)
        }

        private fun mockMotatteOpplysninger(behandling: Behandling) {
            val søknadDokument = SaksbehandlingDataFactory.lagSøknadDokument()
            val mottatteOpplysninger = MottatteOpplysninger().apply {
                this.behandling = behandling
                this.mottatteOpplysningerData = søknadDokument
            }

            every { mottatteOpplysningerService.finnMottatteOpplysninger(behandling.id) } returns Optional.of(mottatteOpplysninger)
        }

        private fun mockBehandlingsresultat(behandlingsresultat: Behandlingsresultat) {
            every { behandlingsresultatService.hentBehandlingsresultat(behandlingsresultat.id) } returns behandlingsresultat
            every { behandlingsresultatService.hentResultatMedMedlemskapOgLovvalg(behandlingsresultat.id) } returns behandlingsresultat
        }

        private fun lagNyDefaultBehandling() = Behandling.forTest {
            tema = Behandlingstema.YRKESAKTIV
            type = Behandlingstyper.FØRSTEGANG
            status = Behandlingsstatus.OPPRETTET
            registrertDato = Instant.now()
        }

        private fun lagDefaultBehandlingResultat() = Behandlingsresultat().apply {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            lovvalgsperioder = setOf(lagDefaultLovvalgsPeriode())
            medlemskapsperioder = setOf(lagDefaultMedlemskapsPeriode())
            vedtakMetadata = VedtakMetadata()
        }


        private fun lagDefaultBehandlingResultatForEøsPensjonist() = Behandlingsresultat().apply {
            id = BEHANDLING_ID
            type = Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            helseutgiftDekkesPeriode =
                HelseutgiftDekkesPeriode(this, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), Land_iso2.BE)
            vedtakMetadata = VedtakMetadata()
        }

        private fun lagDefaultLovvalgsPeriode() = Lovvalgsperiode().apply {
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

        private fun lagDefaultMedlemskapsPeriode() = Medlemskapsperiode().apply {
            fom = FORVENTET_LOVVALGSPERIODE.periode.fom
            tom = FORVENTET_LOVVALGSPERIODE.periode.tom
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
    }

    @Nested
    @DisplayName("PUT /fagsaker/{saksnr}/betalingsvalg")
    inner class LagreBetalingsvalg {

        @ParameterizedTest
        @EnumSource(Betalingstype::class)
        fun lagreBetalingsvalg(betalingsvalg: Betalingstype) {
            mockMvc.perform(
                MockMvcRequestBuilders.put("$BASE_URL/{saksnr}/betalingsvalg", FagsakTestFactory.SAKSNUMMER)
                    .content(objectMapper.writeValueAsString(betalingsvalg))
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(MockMvcResultMatchers.status().isNoContent())

            verify { aksesskontroll.autoriserSakstilgang(FagsakTestFactory.SAKSNUMMER) }
            verify {
                fagsakService.lagreBetalingsvalg(
                    FagsakTestFactory.SAKSNUMMER,
                    betalingsvalg
                )
            }
        }
    }


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
}
