package no.nav.melosys.service.avgift.aarsavregning

import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.TrygdeavgiftsperiodeTestFactory
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.medlemskapsperiodeForTest
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

    fun lagTidligereBehandlingsresultat(
        init: BehandlingsresultatTestFactory.Builder.() -> Unit = {}
    ) = Behandlingsresultat.forTest {
        id = 1L
        type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
        vedtakMetadata {
            vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK
        }
        init()
    }

    protected fun BehandlingsresultatTestFactory.Builder.medlemskapsperiode(
        start: String,
        slutt: String,
        innvilgelsesResultat: InnvilgelsesResultat = InnvilgelsesResultat.INNVILGET,
        medTrygdeavgift: Boolean = true,
        init: MedlemskapsperiodeTestFactory.Builder.() -> Unit = {}
    ) {
        medlemskapsperioder.add(
            lagMedlemskapsperiode(start, slutt, innvilgelsesResultat, medTrygdeavgift, init)
        )
    }

    protected fun lagMedlemskapsperiode(
        start: String,
        slutt: String,
        innvilgelsesResultat: InnvilgelsesResultat = InnvilgelsesResultat.INNVILGET,
        medTrygdeavgift: Boolean = true,
        init: MedlemskapsperiodeTestFactory.Builder.() -> Unit = {}
    ) = medlemskapsperiodeForTest {
        fom = LocalDate.parse(start)
        tom = LocalDate.parse(slutt)
        trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
        innvilgelsesresultat = innvilgelsesResultat
        medlemskapstype = Medlemskapstyper.FRIVILLIG
        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8

        if (medTrygdeavgift) {
            trygdeavgiftsperiode(start, slutt)
        }
        init()
    }

    protected fun MedlemskapsperiodeTestFactory.Builder.trygdeavgiftsperiode(
        start: String,
        slutt: String,
        init: TrygdeavgiftsperiodeTestFactory.Builder.() -> Unit = {}
    ) {
        trygdeavgiftsperiode {  // Call the builder method
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
            init()
        }
    }

    protected fun lagSkatteforholdTilNorge(start: String, slutt: String): SkatteforholdTilNorge = SkatteforholdTilNorge().apply {
        fomDato = LocalDate.parse(start)
        tomDato = LocalDate.parse(slutt)
        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
    }
}
