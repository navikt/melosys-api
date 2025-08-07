package no.nav.melosys.service.registeropplysninger

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.BehandlingTestFactory
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class RegisteropplysningerPeriodeFactoryKtTest {

    private val arbeidsforholdhistorikkAntallMåneder = 6
    private val medlemskaphistorikkAntallÅr = 5
    private val inntektshistorikkAntallMåneder = 6
    private val behandlingAvSøknad = lagBehandling(true)
    private val mottakAvSed = lagBehandling(false)

    private lateinit var factory: RegisteropplysningerPeriodeFactory

    @BeforeEach
    fun setUp() {
        factory = RegisteropplysningerPeriodeFactory(
            arbeidsforholdhistorikkAntallMåneder,
            medlemskaphistorikkAntallÅr,
            inntektshistorikkAntallMåneder
        )
    }

    @Test
    fun `hentPeriodeForArbeidsforhold returnerer korrekt periode`() {
        val fom = LocalDate.now().minusMonths(1)
        val tom = LocalDate.now()

        val periode = factory.hentPeriodeForArbeidsforhold(fom, tom)

        periode.fom shouldBe fom.minusMonths(arbeidsforholdhistorikkAntallMåneder.toLong())
        periode.tom shouldBe LocalDate.now()
    }

    @Test
    fun `hentPeriodeForArbeidsforhold fremtidig periode`() {
        val fom = LocalDate.now().plusYears(1)
        val tom = LocalDate.now().plusYears(2)

        val periode = factory.hentPeriodeForArbeidsforhold(fom, tom)

        periode.fom shouldBe LocalDate.now().minusMonths(arbeidsforholdhistorikkAntallMåneder.toLong())
        periode.tom shouldBe LocalDate.now()
    }

    @Test
    fun `hentPeriodeForArbeidsforhold åpen periode mottak SED`() {
        val fom = LocalDate.now().minusYears(2)
        val tom = null

        val periode = factory.hentPeriodeForArbeidsforhold(fom, tom)

        periode.fom shouldBe fom.minusMonths(arbeidsforholdhistorikkAntallMåneder.toLong())
        periode.tom shouldBe LocalDate.now()
    }

    @Test
    fun `hentPeriodeForArbeidsforhold åpen periode behandling søknad`() {
        val idag = LocalDate.now()
        val fom = idag.minusYears(2)
        val tom = null

        val periode = factory.hentPeriodeForArbeidsforhold(fom, tom)

        periode.fom shouldBe fom.minusMonths(arbeidsforholdhistorikkAntallMåneder.toLong())
        periode.tom shouldBe idag
    }

    @Test
    fun `hentPeriodeForMedlemskap åpen periode behandling søknad`() {
        val idag = LocalDate.now()
        val fom = idag.minusYears(3)
        val tom = null

        val periode = factory.hentPeriodeForMedlemskap(fom, tom, behandlingAvSøknad)

        periode.fom shouldBe fom.minusYears(medlemskaphistorikkAntallÅr.toLong())
        periode.tom shouldBe idag
    }

    @Test
    fun `hentPeriodeForMedlemskap åpen periode mottak SED`() {
        val fom = LocalDate.now().minusYears(1)
        val tom = null

        val periode = factory.hentPeriodeForMedlemskap(fom, tom, mottakAvSed)

        periode.fom shouldBe fom.minusYears(medlemskaphistorikkAntallÅr.toLong())
        periode.tom.shouldBeNull()
    }

    @Test
    fun `hentPeriodeForYtelser periode påbegynt verifiser inntekt periode`() {
        val fom = LocalDate.now().minusYears(1)
        val tom = LocalDate.now().plusYears(1)

        val periode = factory.hentPeriodeForInntekt(fom, tom, mottakAvSed)

        periode.fom shouldBe YearMonth.from(fom.minusMonths(2))
        periode.tom shouldBe YearMonth.from(LocalDate.now())
    }

    @Test
    fun `hentPeriodeForYtelser periode ikke påbegynt mottak SED verifiser inntekt periode`() {
        val fom = LocalDate.now().plusYears(1)
        val tom = LocalDate.now().plusYears(2)

        val periode = factory.hentPeriodeForInntekt(fom, tom, mottakAvSed)

        periode.fom shouldBe YearMonth.from(LocalDate.now().minusMonths(2))
        periode.tom shouldBe YearMonth.from(LocalDate.now())
    }

    @Test
    fun `hentPeriodeForYtelser åpen periode ikke påbegynt mottak SED verifiser inntekt periode`() {
        val now = LocalDate.now()
        val fom = now.plusYears(1)
        val tom = null

        val periode = factory.hentPeriodeForInntekt(fom, tom, mottakAvSed)

        periode.fom shouldBe YearMonth.from(now.minusMonths(2))
        periode.tom shouldBe YearMonth.from(now)
    }

    @Test
    fun `hentPeriodeForYtelser periode ikke påbegynt behandling søknad verifiser inntekt periode`() {
        val fom = LocalDate.now().plusYears(1)
        val tom = LocalDate.now().plusYears(2)

        val periode = factory.hentPeriodeForInntekt(fom, tom, behandlingAvSøknad)

        periode.fom shouldBe YearMonth.now().minusMonths(inntektshistorikkAntallMåneder.toLong())
        periode.tom shouldBe YearMonth.now()
    }

    @Test
    fun `hentPeriodeForYtelser periode avsluttet verifiser inntekt periode`() {
        val fom = LocalDate.now().minusYears(3)
        val tom = LocalDate.now().minusYears(2)

        val periode = factory.hentPeriodeForInntekt(fom, tom, mottakAvSed)

        periode.fom shouldBe YearMonth.from(fom)
        periode.tom shouldBe YearMonth.from(tom)
    }

    @Test
    fun `hentPeriodeForYtelser åpen periode mottak SED forespør tom til dato`() {
        val now = LocalDate.now()
        val fom = now.minusYears(2)
        val tom = null

        val periode = factory.hentPeriodeForInntekt(fom, tom, mottakAvSed)

        periode.fom shouldBe YearMonth.from(fom.minusMonths(2))
        periode.tom shouldBe YearMonth.from(now)
    }

    @Test
    fun `hentPeriodeForYtelser åpen periode mottak SED forespør tom til dato og dagens dato fom`() {
        val now = LocalDate.now()
        val fom = LocalDate.now()
        val tom = null

        val periode = factory.hentPeriodeForInntekt(fom, tom, mottakAvSed)

        periode.fom shouldBe YearMonth.from(fom.minusMonths(2))
        periode.tom shouldBe YearMonth.from(now)
    }

    @Test
    fun `hentPeriodeForYtelser åpen periode behandling søknad forespør tom til dato`() {
        val idag = LocalDate.now()
        val fom = idag.minusYears(2)
        val tom = null

        val periode = factory.hentPeriodeForInntekt(fom, tom, behandlingAvSøknad)

        periode.fom shouldBe YearMonth.from(fom.minusMonths(inntektshistorikkAntallMåneder.toLong()))
        periode.tom shouldBe YearMonth.from(idag)
    }

    companion object {
        private fun lagBehandling(erBehandlingAvSøknad: Boolean): Behandling {
            return BehandlingTestFactory.builderWithDefaults()
                .medTema(
                    if (erBehandlingAvSøknad)
                        Behandlingstema.UTSENDT_ARBEIDSTAKER
                    else
                        Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
                )
                .medFagsak(FagsakTestFactory.lagFagsak())
                .build()
        }
    }
}