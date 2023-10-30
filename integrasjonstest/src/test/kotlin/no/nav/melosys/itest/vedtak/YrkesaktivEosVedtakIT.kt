package no.nav.melosys.itest.vedtak

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.getunleash.FakeUnleash
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.itest.JournalfoeringBase
import no.nav.melosys.itest.OAuthMockServer
import no.nav.melosys.melosysmock.medl.MedlRepo
import no.nav.melosys.melosysmock.testdata.TestDataGenerator
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.saksflyt.ProsessinstansRepository
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.avklartefakta.AvklartefaktaDto
import no.nav.melosys.service.avklartefakta.AvklartefaktaService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.journalforing.JournalfoeringService
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import no.nav.melosys.service.vedtak.FattVedtakRequest
import no.nav.melosys.service.vedtak.VedtaksfattingFasade
import no.nav.melosys.service.vilkaar.VilkaarDto
import no.nav.melosys.service.vilkaar.VilkaarsresultatService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.time.LocalDate

@Import(OAuthMockServer::class)
class YrkesaktivEosVedtakIT(
    @Autowired testDataGenerator: TestDataGenerator,
    @Autowired journalføringService: JournalfoeringService,
    @Autowired oppgaveService: OppgaveService,
    @Autowired prosessinstansRepository: ProsessinstansRepository,
    @Autowired private val avklartefaktaService: AvklartefaktaService,
    @Autowired private val behandlingsresultatService: BehandlingsresultatService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val oAuthMockServer: OAuthMockServer,
    @Autowired private val mottatteOpplysningerService: MottatteOpplysningerService,
    @Autowired private val vilkaarsresultatService: VilkaarsresultatService,
    @Autowired private val lovvalgsperiodeService: LovvalgsperiodeService,
    @Autowired private val ferdigbehandlingKontrollFacade: FerdigbehandlingKontrollFacade,
    @Autowired private val oppfriskSaksopplysningerService: OppfriskSaksopplysningerService,
    @Autowired private val vedtaksfattingFasade: VedtaksfattingFasade,
    @Autowired private val unleash: FakeUnleash,
) : JournalfoeringBase(testDataGenerator, journalføringService, oppgaveService, prosessinstansRepository) {

    @BeforeEach
    fun setup() {
        oAuthMockServer.start()
        unleash.enableAll()
        MedlRepo.repo.clear()
    }

    @AfterEach
    fun afterEach() {
        oAuthMockServer.stop()
        MedlRepo.repo.clear()
    }

    @Test
    fun `yrkesaktiv vedtak - eøs - innvigelse med bestemmelse TODO`() {
        val behandling = journalførOgVentTilProsesserErFerdige(
            defaultJournalføringDto().apply {
                fagsak.sakstype = Sakstyper.EU_EOS.kode
                fagsak.sakstema = Sakstemaer.MEDLEMSKAP_LOVVALG.kode
                behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
                behandlingstemaKode = Behandlingstema.UTSENDT_ARBEIDSTAKER.kode
            },
            waitFor = ProsessType.JFR_NY_SAK_BRUKER,
            alsoWaitForprosessType = listOf(ProsessType.OPPRETT_OG_DISTRIBUER_BREV)
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
            vilkaar = "ART12_1_FORUTGAAENDE_MEDLEMSKAP"
            isOppfylt = true
        }
        val vesentlingVirksomhet = VilkaarDto().apply {
            vilkaar = "ART12_1_VESENTLIG_VIRKSOMHET"
            isOppfylt = true
        }
        val art12_1 = VilkaarDto().apply {
            vilkaar = "FO_883_2004_ART12_1"
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
            .medBehandlingsresultat(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medBestillersId("komponent test")
            .build()


        executeAndWait(
            waitForprosessType = ProsessType.IVERKSETT_VEDTAK_EOS,
            alsoWaitForprosessType = listOf(ProsessType.SEND_BREV)
        ) {
            vedtaksfattingFasade.fattVedtak(behandling.id, vedtakRequest)
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

    private val Any.toJsonNode: JsonNode
        get() {
            return jacksonObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .registerModule(JavaTimeModule())
                .valueToTree(this)
        }
}
