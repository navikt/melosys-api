package no.nav.melosys.itest

import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.optional.shouldBePresent
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.integrasjon.trygdeavgift.dto.DatoPeriodeDto
import no.nav.melosys.itest.vedtak.EøsPensjonistTrygdeavgiftsberegningTransformer
import no.nav.melosys.repository.BehandlingRepository
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.service.avgift.EøsPensjonistTrygdeavgiftsberegningService
import no.nav.melosys.service.avgift.IverksettTrygdeavgiftService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class EøsPensjonistIverksettIT(
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingsresultatService: BehandlingsresultatService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val oppfriskSaksopplysningerService: OppfriskSaksopplysningerService,
    @Autowired private val helseutgiftDekkesPeriodeService: HelseutgiftDekkesPeriodeService,
    @Autowired private val iverksettTrygdeavgiftService: IverksettTrygdeavgiftService,
    @Autowired private val eøsPensjonistTrygdeavgiftsberegningService: EøsPensjonistTrygdeavgiftsberegningService,
    @Autowired private val melosysHendelseKafkaConsumer: MelosysHendelseKafkaConsumer,
) : AvgiftFaktureringTestBase(
    EøsPensjonistTrygdeavgiftsberegningTransformer()
) {
    override val fakturaserieReferanse: String = "01J17B5NTTDYKFB5DZTSSQEHJ0"

    @AfterEach
    fun afterEach() {
        melosysHendelseKafkaConsumer.clear()
    }

    @Test
    fun `Opprett førstegangsbehandling og iverksett pensjonist EØS`() {
        val behandling = lagFørstegangsbehandling(Skatteplikttype.IKKE_SKATTEPLIKTIG, false)

        fagsakRepository.findBySaksnummer(behandling.fagsak.saksnummer)
            .shouldBePresent().run {
                behandlinger.shouldHaveSize(1)
            }

        behandlingsresultatService.hentBehandlingsresultat(behandling.id).run {
            type shouldBe Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            behandlingsmåte shouldBe Behandlingsmaate.MANUELT
        }
        behandlingRepository.findById(behandling.id).shouldBePresent()
            .run {
                withClue("Behandlingsstatus skal være AVSLUTTET") {
                    status shouldBe Behandlingsstatus.AVSLUTTET
                }
                fagsak.apply {
                    withClue("Saksstatus skal være TRYGDEAVGIFT_AVKLART") {
                        status shouldBe Saksstatuser.TRYGDEAVGIFT_AVKLART
                    }
                }
            }

        mockServer.verify(
            1,
            WireMock.postRequestedFor(WireMock.urlEqualTo("/api/v1/mal/trygdeavgift_informasjonsbrev/lag-pdf?somKopi=false&utkast=false"))
        )
    }

    fun lagFørstegangsbehandling(skatteplikttype: Skatteplikttype, arbeidsgiversavgiftBetales: Boolean): Behandling {
        val behandling = journalførOgVentTilProsesserErFerdige(
            defaultJournalføringDto().apply {
                fagsak.sakstype = Sakstyper.EU_EOS.name
                fagsak.sakstema = Sakstemaer.TRYGDEAVGIFT.name
                behandlingstypeKode = Behandlingstyper.FØRSTEGANG.kode
                behandlingstemaKode = Behandlingstema.PENSJONIST.name
            },
            mapOf(
                ProsessType.JFR_NY_SAK_BRUKER to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        ).behandling.shouldNotBeNull()

        helseutgiftDekkesPeriodeService.opprettHelseutgiftDekkesPeriode(
            behandling.id,
            LocalDate.of(2023, 1, 1),
            LocalDate.of(2023, 2, 1),
            Land_iso2.DK
        )

        oppfriskSaksopplysningerService.oppdaterRegisteropplysningerOgTilbakestillBehandlingsresultat(behandling.id, false)
        setupTrygdeavgiftBeregning(behandling.id, skatteplikttype, arbeidsgiversavgiftBetales)
        fagsakService.lagreBetalingsvalg(behandling.fagsak.saksnummer, Betalingstype.FAKTURA)

        executeAndWait(
            mapOf(
                ProsessType.IVERKSETT_EOS_PENSJONIST_AVGIFT to 1,
                ProsessType.OPPRETT_OG_DISTRIBUER_BREV to 1
            )
        ) {
            iverksettTrygdeavgiftService.opprettProsessIverksettTrygdeavgiftPensjonist(
                behandling.id,
                Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT,
                Vedtakstyper.FØRSTEGANGSVEDTAK
            )
        }

        return behandling
    }

    private fun setupTrygdeavgiftBeregning(behandlingId: Long, skatteplikttype: Skatteplikttype, arbeidsgiversavgiftBetales: Boolean) {

        val periode = DatoPeriodeDto(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 2, 1))

        val skattefordholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = periode.fom
                tomDato = periode.tom
                this.skatteplikttype = skatteplikttype
            }
        )
        val inntektsforholdsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = periode.fom
                tomDato = periode.tom
                this.type = Inntektskildetype.UFØRETRYGD
                isArbeidsgiversavgiftBetalesTilSkatt = arbeidsgiversavgiftBetales
                avgiftspliktigMndInntekt = Penger(10000.toBigDecimal())
                avgiftspliktigTotalinntekt = Penger(10000.toBigDecimal())
            }
        )

        eøsPensjonistTrygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(behandlingId, skattefordholdsperioder, inntektsforholdsperioder)


        val skatteforholdTilNorge = SkatteforholdTilNorge().apply {
            fomDato = LocalDate.of(2023, 1, 1)
            tomDato = LocalDate.of(2023, 2, 1)
            this@apply.skatteplikttype = skatteplikttype
        }

        val inntektsperiode = Inntektsperiode().apply {
            fomDato = LocalDate.of(2023, 1, 1)
            tomDato = LocalDate.of(2023, 2, 1)
            type = Inntektskildetype.PENSJON_UFØRETRYGD
            isArbeidsgiversavgiftBetalesTilSkatt = arbeidsgiversavgiftBetales
            avgiftspliktigMndInntekt = Penger(10000.toBigDecimal(), "nok")
        }

        val trygdeavgiftsperioder = HashSet<Trygdeavgiftsperiode>()
        trygdeavgiftsperioder.add(
            Trygdeavgiftsperiode(
                periodeFra = LocalDate.of(2023, 1, 1),
                periodeTil = LocalDate.of(2023, 2, 1),
                trygdesats = 6.8.toBigDecimal(),
                trygdeavgiftsbeløpMd = Penger(1000.toBigDecimal(), "nok"),
                grunnlagSkatteforholdTilNorge = skatteforholdTilNorge,
                grunnlagInntekstperiode = inntektsperiode
            )
        )
        val helseutgiftDekkesPeriode = helseutgiftDekkesPeriodeService.finnHelseutgiftDekkesPeriode(behandlingId)!!
        helseutgiftDekkesPeriode.trygdeavgiftsperioder = trygdeavgiftsperioder
    }
}
