package no.nav.melosys.service.avgift.aarsavregning

import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.forTest
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.repository.AarsavregningRepository
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

@ExtendWith(MockKExtension::class)
abstract class ÅrsavregningServiceTestBase {

    @RelaxedMockK
    protected lateinit var aarsavregningRepository: AarsavregningRepository

    @RelaxedMockK
    protected lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    protected lateinit var fagsakService: FagsakService

    @RelaxedMockK
    protected lateinit var trygdeavgiftService: TrygdeavgiftService

    protected lateinit var årsavregningService: ÅrsavregningService

    @BeforeEach
    fun setup() {
        årsavregningService = ÅrsavregningService(
            aarsavregningRepository,
            behandlingsresultatService,
            fagsakService,
            trygdeavgiftService
        )
        SpringSubjectHandler.set(TestSubjectHandler())
    }

    fun lagTidligereBehandlingsresultat(): Behandlingsresultat = Behandlingsresultat.forTest {
        id = 1L
        type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
        vedtakMetadata {
            vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
        }
    }.apply {
        medlemskapsperioder = mutableSetOf(
            lagMedlemskapsperiode("2022-01-01", "2022-08-31"),
            lagMedlemskapsperiode("2022-09-01", "2023-05-31"),
            lagMedlemskapsperiode("2023-07-01", "2023-08-31", InnvilgelsesResultat.AVSLAATT)
        )
    }

    protected fun lagMedlemskapsperiode(
        start: String,
        slutt: String,
        innvilgelsesResultat: InnvilgelsesResultat = InnvilgelsesResultat.INNVILGET,
        medTrygdeavgift: Boolean = true,
        forskuddsvisFaktura: Boolean = true
    ): Medlemskapsperiode {
        return medlemskapsperiodeForTest {
            fom = LocalDate.parse(start)
            tom = LocalDate.parse(slutt)
            trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
            innvilgelsesresultat = innvilgelsesResultat
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8
        }.apply {
            trygdeavgiftsperioder = if (medTrygdeavgift) {
                mutableSetOf(lagTrygdeavgift(start, slutt, forskuddsvisFaktura))
            } else {
                mutableSetOf()
            }
        }
    }

    protected fun lagTrygdeavgift(start: String, slutt: String, forskuddsvisFaktura: Boolean = true): Trygdeavgiftsperiode =
        Trygdeavgiftsperiode.forTest {
            periodeFra = LocalDate.parse(start)
            periodeTil = LocalDate.parse(slutt)
            trygdeavgiftsbeløpMd = BigDecimal(5000.0)
            trygdesats = BigDecimal(3.5)
            grunnlagInntekstperiode {
                type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                avgiftspliktigMndInntekt = Penger(5000.0)
            }
            grunnlagSkatteforholdTilNorge {
                skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
            }
        } // Note: forskuddsvisFaktura parameter is ignored as the DSL doesn't support it yet and the default value is true

    protected fun lagInntektsperiode(start: String, slutt: String): Inntektsperiode = Inntektsperiode().apply {
        fomDato = LocalDate.parse(start)
        tomDato = LocalDate.parse(slutt)
        avgiftspliktigMndInntekt = Penger(5000.0)
        avgiftspliktigTotalinntekt = Penger(5000.0)
        type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
    }

    protected fun lagSkatteforholdTilNorge(start: String, slutt: String): SkatteforholdTilNorge = SkatteforholdTilNorge().apply {
        fomDato = LocalDate.parse(start)
        tomDato = LocalDate.parse(slutt)
        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
    }

    protected fun lagVedtakMetadata(vedtaksdato: String) = VedtakMetadata().apply {
        this.vedtaksdato = LocalDate.parse(vedtaksdato).atStartOfDay().toInstant(ZoneOffset.UTC)
    }
}
