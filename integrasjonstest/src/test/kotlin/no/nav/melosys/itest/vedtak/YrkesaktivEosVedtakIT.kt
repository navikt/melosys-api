package no.nav.melosys.itest.vedtak

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.Utpekingsperiode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.*
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.itest.JournalfoeringBase
import no.nav.melosys.melosysmock.medl.MedlRepo
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.felles.dto.SoeknadslandDto
import no.nav.melosys.service.journalforing.dto.PeriodeDto
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.sak.OpprettSak
import no.nav.melosys.service.sak.OpprettSakDto
import no.nav.melosys.service.sak.SøknadDto
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import no.nav.melosys.service.utpeking.UtpekingService
import no.nav.melosys.service.vedtak.FattVedtakRequest
import no.nav.melosys.service.vedtak.VedtaksfattingFasade
import no.nav.melosys.service.vilkaar.VilkaarDto
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.UtstedtA1AivenProducer
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.Lovvalgsbestemmelse
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.UtstedtA1Melding
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.time.LocalDate

import no.nav.melosys.melosysmock.config.SoapConfig
@Import(SoapConfig::class)
class YrkesaktivEosVedtakIT(
    @Autowired private val avklartefaktaService: AvklartefaktaService,
    @Autowired private val behandlingsresultatService: BehandlingsresultatService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val mottatteOpplysningerService: MottatteOpplysningerService,
    @Autowired private val vilkaarsresultatService: VilkaarsresultatService,
    @Autowired private val lovvalgsperiodeService: LovvalgsperiodeService,
    @Autowired private val ferdigbehandlingKontrollFacade: FerdigbehandlingKontrollFacade,
    @Autowired private val oppfriskSaksopplysningerService: OppfriskSaksopplysningerService,
    @Autowired private val utpekingService: UtpekingService,
    @Autowired private val vedtaksfattingFasade: VedtaksfattingFasade,
    @Autowired private val opprettSak: OpprettSak
) : JournalfoeringBase() {

    @MockkBean
    private lateinit var utstedtA1AivenProducer: UtstedtA1AivenProducer
    private var originalSubjectHandler: SubjectHandler? = null

    @BeforeEach
    fun setup() {
        originalSubjectHandler = SubjectHandler.getInstance()
        val mockHandler = mockk<SpringSubjectHandler>()
        SubjectHandler.set(mockHandler)
        every { mockHandler.userID } returns "Z123456"
        every { mockHandler.userName } returns "test"

        unleash.enableAll()
        MedlRepo.repo.clear()
    }

    @AfterEach
    fun afterEach() {
        MedlRepo.repo.clear()
        SubjectHandler.set(originalSubjectHandler)

    }

    @Test
    fun `yrkesaktiv vedtak - eøs - innvigelse med bestemmelse FO_883_2004_ART12_1`() {
        val behandling = journalførOgVentTilProsesserErFerdige(
            defaultJournalføringDto().apply {
                fagsak.sakstype = Sakstyper.EU_EOS.kode
                fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode
                behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
                behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
            },
            mapOf(
                ProsessType.JFR_NY_SAK_BRUKER to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        ).behandling

        val mottatteOpplysninger =
            mottatteOpplysningerService.hentEllerOpprettMottatteOpplysninger(behandling.id, true)
                .shouldNotBeNull()
                .mottatteOpplysningerData.apply {
                    periode = Periode(
                        LocalDate.of(2023, 1, 1),
                        LocalDate.of(2023, 2, 1),
                    )
                    soeknadsland = Soeknadsland(listOf(Landkoder.BE.kode), false)
                }
        mottatteOpplysningerService.oppdaterMottatteOpplysninger(behandling.id, mottatteOpplysninger.toJsonNode)
        oppfriskSaksopplysningerService.oppfriskSaksopplysning(behandling.id, false)

        val yrkesgruppe = AvklartefaktaDto(
            listOf("ORDINAER"), "YRKESGRUPPE"
        ).apply {
            avklartefaktaType = Avklartefaktatyper.YRKESGRUPPE
            subjektID = null
            begrunnelseKoder = emptyList()
            begrunnelseFritekst = null
        }
        val virksomhet = AvklartefaktaDto(
            listOf("TRUE"), "VIRKSOMHET"
        ).apply {
            avklartefaktaType = Avklartefaktatyper.VIRKSOMHET
            subjektID = "999999999"
            begrunnelseKoder = emptyList()
            begrunnelseFritekst = null
        }
        val yrkesaktivitet = AvklartefaktaDto(
            listOf("ORDINAER_ARBEIDSTAKER"), "YRKESAKTIVITET"
        ).apply {
            avklartefaktaType = null
            subjektID = null
            begrunnelseKoder = emptyList()
            begrunnelseFritekst = null
        }
        avklartefaktaService.lagreAvklarteFakta(behandling.id, setOf(yrkesgruppe, virksomhet, yrkesaktivitet))

        val forutgåendeMedlemskap = VilkaarDto().apply {
            vilkaar = Vilkaar.FORUTGAAENDE_MEDLEMSKAP.kode
            isOppfylt = true
        }
        val vesentlingVirksomhet = VilkaarDto().apply {
            vilkaar = Vilkaar.VESENTLIG_VIRKSOMHET.kode
            isOppfylt = true
        }
        val art12_1 = VilkaarDto().apply {
            vilkaar = Vilkaar.FO_883_2004_ART12_1.kode
            isOppfylt = true
        }
        vilkaarsresultatService.registrerVilkår(behandling.id, listOf(forutgåendeMedlemskap, vesentlingVirksomhet, art12_1))

        lovvalgsperiodeService.lagreLovvalgsperioder(behandling.id, listOf(Lovvalgsperiode().apply {
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            lovvalgsland = Land_iso2.BE
            medlemskapstype = Medlemskapstyper.PLIKTIG
            dekning = Trygdedekninger.FULL_DEKNING

            fom = LocalDate.of(2023, 1, 1)
            tom = LocalDate.of(2023, 2, 1)
        }))

        ferdigbehandlingKontrollFacade.kontroller(behandling.id, false, null, setOf())

        val vedtakRequest = FattVedtakRequest.Builder()
            .medBehandlingsresultatType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medBestillersId("komponent test")
            .build()

        val utstedtA1MeldingCapturingSlot = slot<UtstedtA1Melding>()
        every { utstedtA1AivenProducer.produserMelding(capture(utstedtA1MeldingCapturingSlot)) } returns mockk<UtstedtA1Melding>()


        executeAndWait(
            mapOf(
                ProsessType.IVERKSETT_VEDTAK_EOS to 1,
                ProsessType.SEND_BREV to 3,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        ) {
            vedtaksfattingFasade.fattVedtak(behandling.id, vedtakRequest)
        }


        verify(exactly = 1) { utstedtA1AivenProducer.produserMelding(any()) }
        utstedtA1MeldingCapturingSlot.captured.apply {
            behandlingId.shouldBe(behandling.id)
            artikkel.shouldBe(Lovvalgsbestemmelse.ART_12_1)
        }

        behandlingsresultatService.hentBehandlingsresultat(behandling.id).apply {
            type shouldBe Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
            behandlingsmåte shouldBe Behandlingsmaate.MANUELT
            fastsattAvLand shouldBe Land_iso2.NO
        }
        lovvalgsperiodeService.hentLovvalgsperiode(behandling.id).apply {
            innvilgelsesresultat shouldBe InnvilgelsesResultat.INNVILGET
            bestemmelse shouldBe Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1
            lovvalgsland shouldBe Land_iso2.BE
            medlemskapstype shouldBe Medlemskapstyper.PLIKTIG
            dekning shouldBe Trygdedekninger.FULL_DEKNING
            fom shouldBe LocalDate.of(2023, 1, 1)
            tom shouldBe LocalDate.of(2023, 2, 1)
        }
        behandlingRepository.findById(behandling.id).orElse(null)
            .shouldNotBeNull().apply {
                withClue("Behandlingsstatus skal være AVSLUTTET") {
                    status shouldBe Behandlingsstatus.AVSLUTTET
                }
                fagsak.apply {
                    withClue("Saksstatus skal være LOVVALG_AVKLART") {
                        status shouldBe Saksstatuser.LOVVALG_AVKLART
                    }
                }
            }

        MedlRepo.repo.values
            .shouldHaveSize(1)
            .first()
            .apply {
                fraOgMed shouldBe LocalDate.of(2023, 1, 1)
                tilOgMed shouldBe LocalDate.of(2023, 2, 1)
                status shouldBe "GYLD"
                dekning shouldBe "Full"
                medlem shouldBe true
                lovvalgsland shouldBe "BEL"
                lovvalg shouldBe "ENDL"
                grunnlag shouldBe "FO_12_1"
                sporingsinformasjon?.kildedokument shouldBe "Henv_Soknad"
            }
    }

    @Test
    fun `yrkesaktiv vedtak - eøs - innvilgelse med selvstendig virksomhet i flere land, artikkel 13`() {
        val opprettSakDto = OpprettSakDto().apply {
            hovedpart = Aktoersroller.BRUKER
            brukerID = "30056928150"
            sakstype = Sakstyper.EU_EOS
            sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG
            behandlingstema = Behandlingstema.ARBEID_FLERE_LAND
            behandlingstype = Behandlingstyper.FØRSTEGANG
            behandlingsaarsakType = Behandlingsaarsaktyper.SØKNAD
            soknadDto = SøknadDto().apply {
                land = SoeknadslandDto().apply {
                    landkoder = listOf("BE", "NO")
                    isFlereLandUkjentHvilke = false
                }
                periode = PeriodeDto(
                    LocalDate.of(2021, 10, 1),
                    LocalDate.of(2021, 10, 2)
                )
            }
            mottaksdato = LocalDate.of(2021, 10, 24)
            skalTilordnes = true
        }

        val behandling = executeAndWait(mapOf(ProsessType.OPPRETT_SAK to 1)) {
            opprettSak.opprettNySakOgBehandling(opprettSakDto)
        }.behandling

        val mottatteOpplysninger =
            mottatteOpplysningerService.hentEllerOpprettMottatteOpplysninger(behandling.id, true).shouldNotBeNull().mottatteOpplysningerData.apply {
                periode = Periode(
                    LocalDate.of(2021, 10, 1),
                    LocalDate.of(2021, 10, 2),
                )
                soeknadsland = Soeknadsland(listOf(Landkoder.BE.kode, Landkoder.NO.kode), false)
            }

        mottatteOpplysningerService.oppdaterMottatteOpplysninger(behandling.id, mottatteOpplysninger.toJsonNode)
        oppfriskSaksopplysningerService.oppfriskSaksopplysning(behandling.id, false)


        val bostedsland = AvklartefaktaDto(listOf("NO"), "BOSTEDSLAND").apply {
            avklartefaktaType = Avklartefaktatyper.BOSTEDSLAND
            subjektID = "NO"
            begrunnelseKoder = emptyList()
            begrunnelseFritekst = null
        }

        val virksomhet = AvklartefaktaDto(
            listOf("TRUE"), "VIRKSOMHET"
        ).apply {
            avklartefaktaType = Avklartefaktatyper.VIRKSOMHET
            subjektID = "999999999"
            begrunnelseKoder = emptyList()
            begrunnelseFritekst = null
        }

        val arbeidsland = AvklartefaktaDto(listOf("BE"), "ARBEIDSLAND").apply {
            avklartefaktaType = Avklartefaktatyper.ARBEIDSLAND
            subjektID = "BE"
            begrunnelseKoder = emptyList()
            begrunnelseFritekst = null
        }

        val arbeidsland2 = AvklartefaktaDto(listOf("NO"), "ARBEIDSLAND").apply {
            avklartefaktaType = Avklartefaktatyper.ARBEIDSLAND
            subjektID = "NO"
            begrunnelseKoder = emptyList()
            begrunnelseFritekst = null
        }

        val arbeidUtforesIOppgittLand = AvklartefaktaDto(listOf("TRUE"), "ARBEID_UTFORES_I_OPPGITT_LAND").apply {
            begrunnelseKoder = emptyList()
        }

        val yrkesaktivitet = AvklartefaktaDto(
            listOf("SELVSTENDIG_NÆRINGSDRIVENDE"), "YRKESAKTIVITET"
        ).apply {
            begrunnelseKoder = emptyList()
        }

        val marginaltArbeid = AvklartefaktaDto(
            listOf("TRUE"), "MARGINALT_ARBEID"
        ).apply {
            avklartefaktaType = Avklartefaktatyper.MARGINALT_ARBEID
            subjektID = "NO"
            begrunnelseKoder = emptyList()
        }

        val aktivitetINorge = AvklartefaktaDto(
            listOf("UNDER_25_PROSENT"), "AKTIVITET_I_NORGE"
        ).apply {
            begrunnelseKoder = emptyList()
        }

        val omfattesILand = AvklartefaktaDto(
            listOf("BE"),
            "OMFATTES_I_LAND"
        ).apply {
            begrunnelseKoder = emptyList()
        }

        avklartefaktaService.lagreAvklarteFakta(
            behandling.id,
            setOf(
                virksomhet, yrkesaktivitet, aktivitetINorge, marginaltArbeid,
                bostedsland, arbeidsland, arbeidsland2, arbeidUtforesIOppgittLand, omfattesILand
            )
        )

        lovvalgsperiodeService.lagreLovvalgsperioder(behandling.id, listOf(Lovvalgsperiode().apply {
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2B
            lovvalgsland = Land_iso2.NO
            medlemskapstype = Medlemskapstyper.PLIKTIG
            dekning = Trygdedekninger.FULL_DEKNING_EOSFO

            fom = LocalDate.of(2021, 10, 1)
            tom = LocalDate.of(2021, 10, 2)
        }))

        utpekingService.lagreUtpekingsperioder(behandling.id, mutableListOf(Utpekingsperiode().apply {
            fom = LocalDate.of(2021, 10, 1)
            tom = LocalDate.of(2021, 10, 2)
            lovvalgsland = Land_iso2.BE
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2B
        }))


        executeAndWait(
            mapOf(
                ProsessType.IVERKSETT_VEDTAK_EOS to 1,
                ProsessType.SEND_BREV to 1
            )
        ) {
            utpekingService.utpekLovvalgsland(behandling.fagsak, mutableSetOf(), null, null)
        }


        behandlingsresultatService.hentBehandlingsresultat(behandling.id).run {
            type shouldBe Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND
            behandlingsmåte shouldBe Behandlingsmaate.MANUELT
            fastsattAvLand shouldBe Land_iso2.NO
        }

        lovvalgsperiodeService.hentLovvalgsperiode(behandling.id).run {
            innvilgelsesresultat shouldBe InnvilgelsesResultat.INNVILGET
            bestemmelse shouldBe Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2B
            lovvalgsland shouldBe Land_iso2.BE
            medlemskapstype shouldBe Medlemskapstyper.UNNTATT
            dekning shouldBe Trygdedekninger.UTEN_DEKNING
            fom shouldBe LocalDate.of(2021, 10, 1)
            tom shouldBe LocalDate.of(2021, 10, 2)
        }

        behandlingRepository.findById(behandling.id).orElse(null)
            .shouldNotBeNull().run {
                withClue("Behandlingsstatus skal være AVSLUTTET") {
                    status shouldBe Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
                }
                fagsak.run {
                    withClue("Saksstatus skal være LOVVALG_AVKLART") {
                        status shouldBe Saksstatuser.OPPRETTET
                    }
                }
            }

        MedlRepo.repo.values
            .shouldHaveSize(1)
            .first()
            .run {
                fraOgMed shouldBe LocalDate.of(2021, 10, 1)
                tilOgMed shouldBe LocalDate.of(2021, 10, 2)
                status shouldBe "UAVK"
                dekning shouldBe "Unntatt"
                medlem shouldBe true
                lovvalgsland shouldBe "BEL"
                lovvalg shouldBe "FORL"
                grunnlag shouldBe "FO_13_2_b"
                sporingsinformasjon?.kildedokument shouldBe "Henv_Soknad"
            }
    }

    private val Any.toJsonNode: JsonNode
        get() {
            return jacksonObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(JavaTimeModule())
                .valueToTree(this)
        }
}
