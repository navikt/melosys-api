package no.nav.melosys.itest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.finn.unleash.FakeUnleash
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikkeyrkesaktivsituasjontype
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb
import no.nav.melosys.domain.mottatteopplysninger.SoeknadIkkeYrkesaktiv
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.domain.saksflyt.ProsessType
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.melosysmock.medl.MedlRepo
import no.nav.melosys.melosysmock.testdata.TestDataGenerator
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.ProsessinstansRepository
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import no.nav.melosys.service.vedtak.FattVedtakRequest
import no.nav.melosys.service.vedtak.VedtaksfattingFasade
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.time.LocalDate

@Import(OAuthMockServer::class)
class IkkeYrkesaktivVedtakIT(
    @Autowired testDataGenerator: TestDataGenerator,
    @Autowired journalføringService: JournalfoeringService,
    @Autowired oppgaveService: OppgaveService,
    @Autowired prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val behandlingsresultatService: BehandlingsresultatService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val unleash: FakeUnleash,
    @Autowired private val oAuthMockServer: OAuthMockServer,
    @Autowired private val mottatteOpplysningerService: MottatteOpplysningerService,
    @Autowired private val lovvalgsperiodeService: LovvalgsperiodeService,
    @Autowired private val ferdigbehandlingKontrollFacade: FerdigbehandlingKontrollFacade,
    @Autowired private val oppfriskSaksopplysningerService: OppfriskSaksopplysningerService,
    @Autowired private val vedtaksfattingFasade: VedtaksfattingFasade
) : JournalfoeringBase(testDataGenerator, journalføringService, oppgaveService, prosessinstansRepository) {

    @BeforeEach
    fun setup() {
        oAuthMockServer.start()
        unleash.resetAll()
        unleash.enable(ToggleName.IKKEYRKESAKTIV_FLYT)

        mockServer.stubFor(
            WireMock.post("/api/v1/mal/ikke_yrkesaktiv_vedtaksbrev/lag-pdf?somKopi=false&utkast=false").willReturn(
                WireMock.aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(ByteArray(0))
            )
        )
        MedlRepo.repo.clear()
    }

    @AfterEach
    fun afterEach() {
        oAuthMockServer.stop()
    }

    @Test
    fun `ikke yrkesaktiv vedtak - eøs - innvigelse med bestemmelse FO_883_2004_ART11_2`() {
        val behandling = journalførOgVentTilProsesserErFerdige(
            journalfoeringOpprettDto = defaultJournalføringDto().apply {
                fagsak.sakstype = Sakstyper.EU_EOS.kode
                fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode
                behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
                behandlingstemaKode = Behandlingstema.IKKE_YRKESAKTIV.kode
            },
            waitFor = ProsessType.JFR_NY_SAK_BRUKER,
            alsoWaitForprosessType = listOf(ProsessType.OPPRETT_OG_DISTRIBUER_BREV)
        ).behandling

        val mottatteOpplysninger =
            mottatteOpplysningerService.hentEllerOpprettMottatteOpplysninger(behandling.id, false)
                .shouldNotBeNull()
                .mottatteOpplysningerData.apply {
                    periode = Periode(
                        LocalDate.of(2022, 1, 1),
                        LocalDate.of(2022, 2, 1),
                    )
                    soeknadsland = Soeknadsland(listOf(Landkoder.DE.kode), false)
                }

        mottatteOpplysningerService.oppdaterMottatteOpplysninger(behandling.id, mottatteOpplysninger.toJsonNode)
        oppfriskSaksopplysningerService.oppfriskSaksopplysning(behandling.id, false)

        behandlingsresultatService.oppdaterUtfallRegistreringUnntak(behandling.id, Utfallregistreringunntak.GODKJENT)
        behandlingsresultatService.oppdaterFritekster(behandling.id, "begrunnelse", "innledning")

        lovvalgsperiodeService.lagreLovvalgsperioder(behandling.id, listOf(Lovvalgsperiode().apply {
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            bestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_2
            lovvalgsland = Land_iso2.NO
            medlemskapstype = Medlemskapstyper.PLIKTIG
            dekning = Trygdedekninger.FULL_DEKNING

            fom = LocalDate.of(2022, 1, 1)
            tom = LocalDate.of(2022, 2, 1)
        }))

        ferdigbehandlingKontrollFacade.kontroller(behandling.id, false, null, setOf())

        val vedtakRequest = FattVedtakRequest.Builder()
            .medBehandlingsresultat(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medBestillersId("komponent test")
            .build()


        executeAndWait(
            waitForprosessType = ProsessType.IVERKSETT_VEDTAK_IKKE_YRKESAKTIV,
            alsoWaitForprosessType = listOf(ProsessType.OPPRETT_OG_DISTRIBUER_BREV)
        ) {
            vedtaksfattingFasade.fattVedtak(behandling.id, vedtakRequest)
        }


        behandlingsresultatService.hentBehandlingsresultat(behandling.id).apply {
            type.shouldBe(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND)
            behandlingsmåte.shouldBe(Behandlingsmaate.MANUELT)
            begrunnelseFritekst.shouldBe("begrunnelse")
            utfallRegistreringUnntak.shouldBe(Utfallregistreringunntak.GODKJENT)
            fastsattAvLand.shouldBe(Land_iso2.NO)
        }
        lovvalgsperiodeService.hentLovvalgsperiode(behandling.id).apply {
            innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
            bestemmelse.shouldBe(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_2)
            lovvalgsland.shouldBe(Land_iso2.NO)
            medlemskapstype.shouldBe(Medlemskapstyper.PLIKTIG)
            dekning.shouldBe(Trygdedekninger.FULL_DEKNING)
            fom.shouldBe(LocalDate.of(2022, 1, 1))
            tom.shouldBe(LocalDate.of(2022, 2, 1))
        }
        behandlingRepository.findById(behandling.id).orElse(null)
            .shouldNotBeNull().apply {
                withClue("Behandlingsstatus skal være AVSLUTTET") {
                    status.shouldBe(Behandlingsstatus.AVSLUTTET)
                }
                fagsak.apply {
                    withClue("Saksstatus skal være LOVVALG_AVKLART") {
                        status.shouldBe(Saksstatuser.LOVVALG_AVKLART)
                    }
                }
            }

        MedlRepo.repo.values
            .shouldHaveSize(1)
            .first()
            .apply {
                fraOgMed.shouldBe(LocalDate.of(2022, 1, 1))
                tilOgMed.shouldBe(LocalDate.of(2022, 2, 1))
                status.shouldBe("GYLD")
                dekning.shouldBe("Full")
                medlem.shouldBe(true)
                lovvalgsland.shouldBe("NOR")
                lovvalg.shouldBe("ENDL")
                grunnlag.shouldBe("FO_11_2")
                sporingsinformasjon?.kildedokument.shouldBe("Henv_Soknad")
            }
    }

    @Test
    fun `ikke yrkesaktiv vedtak - eøs - innvigelse med bestemmelse FO_883_2004_ART11_3E - student`() {
        val behandling = journalførOgVentTilProsesserErFerdige(
            journalfoeringOpprettDto = defaultJournalføringDto().apply {
                fagsak.sakstype = Sakstyper.EU_EOS.kode
                fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode
                behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
                behandlingstemaKode = Behandlingstema.IKKE_YRKESAKTIV.kode
            },
            waitFor = ProsessType.JFR_NY_SAK_BRUKER,
            alsoWaitForprosessType = listOf(ProsessType.OPPRETT_OG_DISTRIBUER_BREV)
        ).behandling

        val mottatteOpplysninger =
            mottatteOpplysningerService.hentEllerOpprettMottatteOpplysninger(behandling.id, false)
                .shouldNotBeNull()
                .mottatteOpplysningerData.apply {
                    periode = Periode(
                        LocalDate.of(2022, 1, 1),
                        LocalDate.of(2022, 2, 1),
                    )
                    soeknadsland = Soeknadsland(listOf(Landkoder.DE.kode), false)
                }.apply {
                    (this as SoeknadIkkeYrkesaktiv).ikkeYrkesaktivSituasjontype = Ikkeyrkesaktivsituasjontype.STUDENT
                }

        mottatteOpplysningerService.oppdaterMottatteOpplysninger(behandling.id, mottatteOpplysninger.toJsonNode)
        oppfriskSaksopplysningerService.oppfriskSaksopplysning(behandling.id, false)

        behandlingsresultatService.oppdaterUtfallRegistreringUnntak(behandling.id, Utfallregistreringunntak.GODKJENT)
        behandlingsresultatService.oppdaterFritekster(behandling.id, "begrunnelse", "innledning")

        lovvalgsperiodeService.lagreLovvalgsperioder(behandling.id, listOf(Lovvalgsperiode().apply {
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E
            lovvalgsland = Land_iso2.NO
            medlemskapstype = Medlemskapstyper.PLIKTIG
            dekning = Trygdedekninger.FULL_DEKNING

            fom = LocalDate.of(2022, 1, 1)
            tom = LocalDate.of(2022, 2, 1)
        }))

        ferdigbehandlingKontrollFacade.kontroller(behandling.id, false, null, setOf())

        val vedtakRequest = FattVedtakRequest.Builder()
            .medBehandlingsresultat(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medBestillersId("komponent test")
            .build()


        executeAndWait(
            waitForprosessType = ProsessType.IVERKSETT_VEDTAK_IKKE_YRKESAKTIV,
            alsoWaitForprosessType = listOf(ProsessType.OPPRETT_OG_DISTRIBUER_BREV)
        ) {
            vedtaksfattingFasade.fattVedtak(behandling.id, vedtakRequest)
        }


        behandlingsresultatService.hentBehandlingsresultat(behandling.id).apply {
            type.shouldBe(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND)
            behandlingsmåte.shouldBe(Behandlingsmaate.MANUELT)
            begrunnelseFritekst.shouldBe("begrunnelse")
            utfallRegistreringUnntak.shouldBe(Utfallregistreringunntak.GODKJENT)
            fastsattAvLand.shouldBe(Land_iso2.NO)
        }
        lovvalgsperiodeService.hentLovvalgsperiode(behandling.id).apply {
            innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
            bestemmelse.shouldBe(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E)
            lovvalgsland.shouldBe(Land_iso2.NO)
            medlemskapstype.shouldBe(Medlemskapstyper.PLIKTIG)
            dekning.shouldBe(Trygdedekninger.FULL_DEKNING)
            fom.shouldBe(LocalDate.of(2022, 1, 1))
            tom.shouldBe(LocalDate.of(2022, 2, 1))
        }
        behandlingRepository.findById(behandling.id).orElse(null)
            .shouldNotBeNull().apply {
                withClue("Behandlingsstatus skal være AVSLUTTET") {
                    status.shouldBe(Behandlingsstatus.AVSLUTTET)
                }
                fagsak.apply {
                    withClue("Saksstatus skal være LOVVALG_AVKLART") {
                        status.shouldBe(Saksstatuser.LOVVALG_AVKLART)
                    }
                }
            }

        MedlRepo.repo.values
            .shouldHaveSize(1)
            .first()
            .apply {
                fraOgMed.shouldBe(LocalDate.of(2022, 1, 1))
                tilOgMed.shouldBe(LocalDate.of(2022, 2, 1))
                status.shouldBe("GYLD")
                dekning.shouldBe("Full")
                medlem.shouldBe(true)
                lovvalgsland.shouldBe("NOR")
                lovvalg.shouldBe("ENDL")
                grunnlag.shouldBe("FO_11_3_e")
                sporingsinformasjon?.kildedokument.shouldBe("Henv_Soknad")
            }

    }

    @Test
    fun `ikke yrkesaktiv vedtak - trygdeavtale - Storbritania medl`() {

        val behandling = journalførOgVentTilProsesserErFerdige(
            journalfoeringOpprettDto = defaultJournalføringDto().apply {
                fagsak.sakstype = Sakstyper.TRYGDEAVTALE.kode
                fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode
                behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
                behandlingstemaKode = Behandlingstema.IKKE_YRKESAKTIV.kode
            },
            waitFor = ProsessType.JFR_NY_SAK_BRUKER,
            alsoWaitForprosessType = listOf(ProsessType.OPPRETT_OG_DISTRIBUER_BREV)
        ).behandling

        val mottatteOpplysninger =
            mottatteOpplysningerService.hentEllerOpprettMottatteOpplysninger(behandling.id, false)
                .shouldNotBeNull()
                .mottatteOpplysningerData.apply {
                    periode = Periode(
                        LocalDate.of(2022, 1, 1),
                        LocalDate.of(2022, 2, 1),
                    )
                    soeknadsland = Soeknadsland(listOf(Land_iso2.GB.kode), false)
                }
        mottatteOpplysningerService.oppdaterMottatteOpplysninger(behandling.id, mottatteOpplysninger.toJsonNode)
        oppfriskSaksopplysningerService.oppfriskSaksopplysning(behandling.id, false)

        behandlingsresultatService.oppdaterUtfallRegistreringUnntak(behandling.id, Utfallregistreringunntak.GODKJENT)
        behandlingsresultatService.oppdaterFritekster(behandling.id, "begrunnelse", "innledning")

        lovvalgsperiodeService.lagreLovvalgsperioder(behandling.id, listOf(Lovvalgsperiode().apply {
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            bestemmelse = Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART5_4
            lovvalgsland = Land_iso2.NO
            medlemskapstype = Medlemskapstyper.PLIKTIG
            dekning = Trygdedekninger.FULL_DEKNING

            fom = LocalDate.of(2022, 1, 1)
            tom = LocalDate.of(2022, 2, 1)
        }))

        ferdigbehandlingKontrollFacade.kontroller(behandling.id, false, null, setOf())

        val vedtakRequest = FattVedtakRequest.Builder()
            .medBehandlingsresultat(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medBestillersId("komponent test")
            .build()

        executeAndWait(
            waitForprosessType = ProsessType.IVERKSETT_VEDTAK_IKKE_YRKESAKTIV,
            alsoWaitForprosessType = listOf(ProsessType.OPPRETT_OG_DISTRIBUER_BREV)
        ) {
            vedtaksfattingFasade.fattVedtak(behandling.id, vedtakRequest)
        }


        behandlingsresultatService.hentBehandlingsresultat(behandling.id).apply {
            type.shouldBe(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND)
            behandlingsmåte.shouldBe(Behandlingsmaate.MANUELT)
            begrunnelseFritekst.shouldBe("begrunnelse")
            utfallRegistreringUnntak.shouldBe(Utfallregistreringunntak.GODKJENT)
            fastsattAvLand.shouldBe(Land_iso2.NO)
        }
        lovvalgsperiodeService.hentLovvalgsperiode(behandling.id).apply {
            innvilgelsesresultat.shouldBe(InnvilgelsesResultat.INNVILGET)
            bestemmelse.shouldBe(Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART5_4)
            lovvalgsland.shouldBe(Land_iso2.NO)
            medlemskapstype.shouldBe(Medlemskapstyper.PLIKTIG)
            dekning.shouldBe(Trygdedekninger.FULL_DEKNING)
            fom.shouldBe(LocalDate.of(2022, 1, 1))
            tom.shouldBe(LocalDate.of(2022, 2, 1))
        }
        behandlingRepository.findById(behandling.id).orElse(null)
            .shouldNotBeNull().apply {
                withClue("Behandlingsstatus skal være AVSLUTTET") {
                    status.shouldBe(Behandlingsstatus.AVSLUTTET)
                }
                fagsak.apply {
                    withClue("Saksstatus skal være LOVVALG_AVKLART") {
                        status.shouldBe(Saksstatuser.LOVVALG_AVKLART)
                    }
                }
            }

        MedlRepo.repo.values
            .shouldHaveSize(1)
            .first()
            .apply {
                println(toJsonNode.toPrettyString())
                fraOgMed.shouldBe(LocalDate.of(2022, 1, 1))
                tilOgMed.shouldBe(LocalDate.of(2022, 2, 1))
                status.shouldBe("GYLD")
                dekning.shouldBe("Full")
                medlem.shouldBe(true)
                lovvalgsland.shouldBe("NOR")
                lovvalg.shouldBe("ENDL")
                grunnlag.shouldBe("Storbrit_NIrland_5_4")
                sporingsinformasjon?.kildedokument.shouldBe("Henv_Soknad")
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
