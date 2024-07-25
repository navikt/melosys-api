package no.nav.melosys.service.avgift

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.VedtakMetadata
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.BeregnTotalBeløpDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaseriePeriodeDto
import no.nav.melosys.repository.AarsavregningRepository
import no.nav.melosys.service.avgift.aarsavregning.MedlemskapsperiodeForAvgift
import no.nav.melosys.service.avgift.aarsavregning.Trygdeavgiftsgrunnlag
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningModel
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
internal class ÅrsavregningServiceTest {

    @RelaxedMockK
    private lateinit var aarsavregningRepository: AarsavregningRepository

    @RelaxedMockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    private lateinit var faktureringskomponentenConsumer: FaktureringskomponentenConsumer

    @RelaxedMockK
    private lateinit var trygdeavgiftService: TrygdeavgiftService

    private lateinit var årsavregningService: ÅrsavregningService

    val SAKSBEHANDLER_IDENT = "Z990007"

    @BeforeEach
    fun setup() {
        årsavregningService = ÅrsavregningService(
            aarsavregningRepository,
            behandlingsresultatService,
            faktureringskomponentenConsumer,
            trygdeavgiftService
        )
        SpringSubjectHandler.set(TestSubjectHandler())
    }

    @Test
    fun `opprettNyÅrsavregning kaster exception når flere Aarsavregninger eksisterer for samme år på samme Fagsak`() {
        val årsavregningEntity1 = Årsavregning().apply {
            aar = 2023
            behandlingsresultat = Behandlingsresultat()
        }
        val eksisterendeBehandling = Behandling().apply { id = 1L }
        every { aarsavregningRepository.findById(1L) }.returns(Optional.of(årsavregningEntity1))
        every { aarsavregningRepository.finnAntallÅrsavregningerPåFagsakForÅr(1, 2023) }.returns(1)
        every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(Behandlingsresultat().apply { behandling = eksisterendeBehandling })

        shouldThrow<FunksjonellException> {
            årsavregningService.opprettÅrsavregning(1, 2023)
        }
    }

    @Test
    fun `test beregner totalbeløp for 1 år`() {
        val fakturaseriePeriodeDto = FakturaseriePeriodeDto(
            enhetsprisPerManed = BigDecimal(100), startDato = LocalDate.now().minusYears(1), sluttDato = LocalDate.now(), beskrivelse = "test"
        )
        val dto = BeregnTotalBeløpDto(listOf(fakturaseriePeriodeDto, fakturaseriePeriodeDto, fakturaseriePeriodeDto))
        årsavregningService.beregnTotalbeløpForPeriode(dto)

        verify(exactly = 1) { faktureringskomponentenConsumer.hentTotalTrygdeavgiftForPeriode((eq(dto)), eq(SAKSBEHANDLER_IDENT)) }
    }

    @Test
    fun `finnÅrsavregning for ny årsavregning uten info i Melosys`() {
        val behandlingsresultat = Behandlingsresultat().apply {
            behandling = Behandling().apply {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
            }
        }
        val årsavregningEntity = Årsavregning().apply {
            aar = 2023
            this.behandlingsresultat = behandlingsresultat
        }
        behandlingsresultat.Årsavregning = årsavregningEntity
        every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)

        årsavregningService.finnÅrsavregning(1) shouldBe ÅrsavregningModel(
            år = 2023,
            tidligereGrunnlag = null,
            tidligereAvgift = emptyList(),
            nyttGrunnlag = null,
            endeligAvgift = emptyList(),
            tidligereFakturertBeloep = null,
            nyttTotalbeloep = null,
            tilFaktureringBeloep = null
        )
    }

    @Test
    fun `hentÅrsavregning for ny årsavregning, grunnlag finnes i Melosys`() {
        val behandlingsresultat = Behandlingsresultat().apply {
            behandling = Behandling().apply {
                id = 1L
                type = Behandlingstyper.ÅRSAVREGNING
            }
        }
        val årsavregningEntity = Årsavregning().apply {
            aar = 2023
            this.behandlingsresultat = behandlingsresultat
            tidligereBehandlingsresultat = lagTidligereBehandlingsresultat()
        }
        behandlingsresultat.Årsavregning = årsavregningEntity
        every { behandlingsresultatService.hentBehandlingsresultat(1L) }.returns(behandlingsresultat)

        årsavregningService.finnÅrsavregning(1) shouldBe ÅrsavregningModel(
            år = 2023,
            tidligereGrunnlag = Trygdeavgiftsgrunnlag(
                listOf(
                    MedlemskapsperiodeForAvgift(
                        fom = LocalDate.of(2022, 12, 31),
                        tom = LocalDate.of(2023, 5, 31),
                        dekning = Trygdedekninger.FULL_DEKNING_FTRL,
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
                    )
                ),
                listOf(
                    lagSkatteforholdTilNorge("2023-01-01", "2023-05-01")
                ),
                listOf(
                    lagInntektsperiode("2023-01-01", "2023-05-01")
                )
            ),
            tidligereAvgift = listOf(
                lagTrygdeavgift("2023-01-01", "2023-05-01")
            ),
            nyttGrunnlag = null,
            endeligAvgift = emptyList(),
            tidligereFakturertBeloep = null,
            nyttTotalbeloep = null,
            tilFaktureringBeloep = null
        )
    }

    fun lagTidligereBehandlingsresultat(): Behandlingsresultat = Behandlingsresultat().apply {
        id = 1L
        type = Behandlingsresultattyper.FERDIGBEHANDLET
        vedtakMetadata = VedtakMetadata()
        vedtakMetadata.vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
        medlemskapsperioder = listOf(lagMedlemskapsperiode("2022-01-01", "2022-08-31"), lagMedlemskapsperiode("2022-12-31", "2023-05-31"))
    }

    private fun lagMedlemskapsperiode(start: String, slutt: String): Medlemskapsperiode {
        return Medlemskapsperiode().apply {
            trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            fom = LocalDate.parse(start)
            tom = LocalDate.parse(slutt)
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
            trygdeavgiftsperioder = setOf(lagTrygdeavgift("2022-01-01", "2022-08-31"), lagTrygdeavgift("2023-01-01", "2023-05-01"))
        }
    }

    private fun lagTrygdeavgift(start: String, slutt: String): Trygdeavgiftsperiode = Trygdeavgiftsperiode().apply {
        periodeFra = LocalDate.parse(start)
        periodeTil = LocalDate.parse(slutt)
        trygdeavgiftsbeløpMd = Penger(5000.0)
        trygdesats = BigDecimal(3.5)
        grunnlagInntekstperiode = lagInntektsperiode(start, slutt)
        grunnlagSkatteforholdTilNorge = lagSkatteforholdTilNorge(start, slutt)
    }

    private fun lagInntektsperiode(start: String, slutt: String): Inntektsperiode = Inntektsperiode().apply {
        fomDato = LocalDate.parse(start)
        tomDato = LocalDate.parse(slutt)
        avgiftspliktigInntektMnd = Penger(5000.0)
    }

    private fun lagSkatteforholdTilNorge(start: String, slutt: String): SkatteforholdTilNorge = SkatteforholdTilNorge().apply {
        fomDato = LocalDate.parse(start)
        tomDato = LocalDate.parse(slutt)
        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
    }
}
