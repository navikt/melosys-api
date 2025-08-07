package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.brev.DokgenBrevbestilling
import no.nav.melosys.domain.brev.InnvilgelseFtrlYrkesaktivFrivilligBrevbestilling
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.DokgenTestData
import no.nav.melosys.service.dokument.brev.mapper.InnvilgelseFtrlPensjonistMapperTest.Companion.BEGRUNNELSE_FRITEKST
import no.nav.melosys.service.dokument.brev.mapper.InnvilgelseFtrlPensjonistMapperTest.Companion.Case
import no.nav.melosys.service.dokument.brev.mapper.InnvilgelseFtrlPensjonistMapperTest.Companion.INNLEDNING_FRITEKST
import no.nav.melosys.service.dokument.brev.mapper.InnvilgelseFtrlPensjonistMapperTest.Companion.SAKSBEHANDLER_NAVN
import no.nav.melosys.service.dokument.brev.mapper.InnvilgelseFtrlPensjonistMapperTest.Companion.TRYGDEAVGIFT_FRITEKST
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.anyArray
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class InformasjonTrygdeavgiftMapperTest {

    @MockK
    private lateinit var mockDokgenMapperDatahenter: DokgenMapperDatahenter

    @MockK
    private lateinit var mockBehandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var mockTrygdeavgiftsberegningService: TrygdeavgiftsberegningService

    @MockK
    private lateinit var mockHelseutgiftDekkesPeriodeService: HelseutgiftDekkesPeriodeService

    @MockK
    private lateinit var mockTrygdeavgiftMottakerService: TrygdeavgiftMottakerService

    private lateinit var informasjonTrygdeavgiftMapper: InformasjonTrygdeavgiftMapper

    @BeforeEach
    fun setup() {
        informasjonTrygdeavgiftMapper = InformasjonTrygdeavgiftMapper(
            mockDokgenMapperDatahenter,
            mockHelseutgiftDekkesPeriodeService,
            mockTrygdeavgiftMottakerService,
            mockTrygdeavgiftsberegningService,
        )
    }

    @Test
    fun mapInformasjonTrygdeavgift_populererFelter() {
        val behandlingsresultat = lagBehandlingsResultat().apply {
            behandling.mottatteOpplysninger = DokgenTestData.lagMottatteOpplysningerSøknadUtenforEØS()
        }

        every { mockHelseutgiftDekkesPeriodeService.hentHelseutgiftDekkesPeriode(any()) } returns behandlingsresultat.helseutgiftDekkesPeriode
        every { mockTrygdeavgiftMottakerService.getTrygdeavgiftMottaker(any<List<Trygdeavgiftsperiode>>()) } returns Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
        every { mockDokgenMapperDatahenter.hentBehandlingsresultat(ofType()) } returns behandlingsresultat
        every { mockBehandlingsresultatService.hentBehandlingsresultat(ofType()) } returns behandlingsresultat

        informasjonTrygdeavgiftMapper.mapInformasjonTrygdeavgift(lagBrevbestilling()).shouldNotBeNull().apply {
            fomDato shouldBe behandlingsresultat.helseutgiftDekkesPeriode.fomDato
            tomDato shouldBe behandlingsresultat.helseutgiftDekkesPeriode.tomDato
            trygdeavgiftMottaker shouldBe Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
            betalingsvalg shouldBe Betalingstype.TREKK
            bostedLand shouldBe "Danmark"
            erNordisk shouldBe true
            avgiftsperioder shouldHaveSize 2
        }
    }

    private fun lagBrevbestilling(): DokgenBrevbestilling {
        return DokgenBrevbestilling.Builder()
            .medBehandling(DokgenTestData.lagBehandling())
            .medPersonDokument(DokgenTestData.lagPersondata())
            .medPersonMottaker(DokgenTestData.lagPersondata())
            .medForsendelseMottatt(Instant.EPOCH)
            .medSaksbehandlerNavn(SAKSBEHANDLER_NAVN)
            .build()
    }

    private fun lagGrunnlagInntektsperiode(): Inntektsperiode =
        Inntektsperiode().apply {
            type = Inntektskildetype.PENSJON
            isArbeidsgiversavgiftBetalesTilSkatt = true
            avgiftspliktigMndInntekt = Penger(0.0)
        }


    private fun lagTrygdeavgiftsperioder(helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode): MutableSet<Trygdeavgiftsperiode> {
        val inntektsperioder = listOf(lagGrunnlagInntektsperiode().apply {
            fomDato = LocalDate.EPOCH.plusMonths(1)
            tomDato = LocalDate.EPOCH.plusMonths(4)
        })
        val skatteforholdTilNorge =
            listOf(SkatteforholdTilNorge().apply { skatteplikttype = Skatteplikttype.SKATTEPLIKTIG })


        return mutableSetOf(
            Trygdeavgiftsperiode(
                periodeFra = LocalDate.EPOCH.plusMonths(1),
                periodeTil = LocalDate.EPOCH.plusMonths(4),
                trygdesats = BigDecimal.ZERO,
                trygdeavgiftsbeløpMd = Penger(0.0),
                grunnlagHelseutgiftDekkesPeriode = helseutgiftDekkesPeriode,
                grunnlagInntekstperiode = inntektsperioder[0],
                grunnlagSkatteforholdTilNorge = skatteforholdTilNorge[0]
            ),
            Trygdeavgiftsperiode(
                periodeFra = LocalDate.EPOCH.plusMonths(5),
                periodeTil = LocalDate.EPOCH.plusMonths(8),
                trygdesats = BigDecimal(0.05),
                trygdeavgiftsbeløpMd = Penger(500.0),
                grunnlagHelseutgiftDekkesPeriode = helseutgiftDekkesPeriode,
                grunnlagInntekstperiode = inntektsperioder[0],
                grunnlagSkatteforholdTilNorge = skatteforholdTilNorge[0]
            )
        )
    }

    private fun lagHelseutgiftDekkesPeriode(behandlingsresultat: Behandlingsresultat): HelseutgiftDekkesPeriode {
        val helseutgiftDekkesPeriode =
            HelseutgiftDekkesPeriode(behandlingsresultat, LocalDate.EPOCH.plusMonths(1), LocalDate.EPOCH.plusMonths(4), Land_iso2.DK)
        helseutgiftDekkesPeriode.trygdeavgiftsperioder = lagTrygdeavgiftsperioder(helseutgiftDekkesPeriode)

        return helseutgiftDekkesPeriode
    }


    private fun lagBehandlingsResultat(): Behandlingsresultat {
        return Behandlingsresultat().apply {
            id = 1L
            helseutgiftDekkesPeriode = lagHelseutgiftDekkesPeriode(this)
            behandling = DokgenTestData.lagBehandling()
        }
    }

}
