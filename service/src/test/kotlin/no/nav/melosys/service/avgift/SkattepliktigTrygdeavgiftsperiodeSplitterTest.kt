package no.nav.melosys.service.avgift

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.medlemskapsperiodeForTest
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SkattepliktigTrygdeavgiftsperiodeSplitterTest {

    @Test
    fun `periode innenfor ett år gir én trygdeavgiftsperiode`() {
        val periode = medlemskapsperiodeForTest {
            fom = LocalDate.of(2024, 3, 1)
            tom = LocalDate.of(2024, 11, 30)
        }

        val resultat = SkattepliktigTrygdeavgiftsperiodeSplitter.splittPåÅr(periode)

        resultat.shouldHaveSize(1)
        resultat.first().apply {
            periodeFra.shouldBe(LocalDate.of(2024, 3, 1))
            periodeTil.shouldBe(LocalDate.of(2024, 11, 30))
            assertSkattepliktigSkatteforhold(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 11, 30))
            grunnlagMedlemskapsperiode.shouldBe(periode)
        }
    }

    @Test
    fun `periode over to år gir to trygdeavgiftsperioder`() {
        val periode = medlemskapsperiodeForTest {
            fom = LocalDate.of(2024, 3, 1)
            tom = LocalDate.of(2025, 6, 30)
        }

        val resultat = SkattepliktigTrygdeavgiftsperiodeSplitter.splittPåÅr(periode)

        resultat.shouldHaveSize(2)

        resultat[0].apply {
            periodeFra.shouldBe(LocalDate.of(2024, 3, 1))
            periodeTil.shouldBe(LocalDate.of(2024, 12, 31))
            assertSkattepliktigSkatteforhold(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 12, 31))
            grunnlagMedlemskapsperiode.shouldBe(periode)
        }

        resultat[1].apply {
            periodeFra.shouldBe(LocalDate.of(2025, 1, 1))
            periodeTil.shouldBe(LocalDate.of(2025, 6, 30))
            assertSkattepliktigSkatteforhold(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30))
            grunnlagMedlemskapsperiode.shouldBe(periode)
        }
    }

    @Test
    fun `periode over tre år gir tre trygdeavgiftsperioder`() {
        val periode = medlemskapsperiodeForTest {
            fom = LocalDate.of(2023, 7, 1)
            tom = LocalDate.of(2025, 3, 31)
        }

        val resultat = SkattepliktigTrygdeavgiftsperiodeSplitter.splittPåÅr(periode)

        resultat.shouldHaveSize(3)
        resultat[0].periodeFra.shouldBe(LocalDate.of(2023, 7, 1))
        resultat[0].periodeTil.shouldBe(LocalDate.of(2023, 12, 31))
        resultat[1].periodeFra.shouldBe(LocalDate.of(2024, 1, 1))
        resultat[1].periodeTil.shouldBe(LocalDate.of(2024, 12, 31))
        resultat[2].periodeFra.shouldBe(LocalDate.of(2025, 1, 1))
        resultat[2].periodeTil.shouldBe(LocalDate.of(2025, 3, 31))
        resultat.forEach { it.grunnlagMedlemskapsperiode.shouldBe(periode) }
    }

    @Test
    fun `periode som dekker ett helt år gir én periode med 1 jan til 31 des`() {
        val periode = medlemskapsperiodeForTest {
            fom = LocalDate.of(2024, 1, 1)
            tom = LocalDate.of(2024, 12, 31)
        }

        val resultat = SkattepliktigTrygdeavgiftsperiodeSplitter.splittPåÅr(periode)

        resultat.shouldHaveSize(1)
        resultat.first().apply {
            periodeFra.shouldBe(LocalDate.of(2024, 1, 1))
            periodeTil.shouldBe(LocalDate.of(2024, 12, 31))
        }
    }

    @Test
    fun `fraOgMedÅr filtrerer bort perioder i tidligere år`() {
        val periode = medlemskapsperiodeForTest {
            fom = LocalDate.of(2024, 3, 1)
            tom = LocalDate.of(2025, 6, 30)
        }

        val resultat = SkattepliktigTrygdeavgiftsperiodeSplitter.splittPåÅr(periode, fraOgMedÅr = 2025)

        resultat.shouldHaveSize(1)
        resultat.first().apply {
            periodeFra.shouldBe(LocalDate.of(2025, 1, 1))
            periodeTil.shouldBe(LocalDate.of(2025, 6, 30))
            grunnlagMedlemskapsperiode.shouldBe(periode)
        }
    }

    @Test
    fun `fraOgMedÅr lik periodens startår beholder alle perioder`() {
        val periode = medlemskapsperiodeForTest {
            fom = LocalDate.of(2024, 3, 1)
            tom = LocalDate.of(2025, 6, 30)
        }

        val resultat = SkattepliktigTrygdeavgiftsperiodeSplitter.splittPåÅr(periode, fraOgMedÅr = 2024)

        resultat.shouldHaveSize(2)
    }

    @Test
    fun `fraOgMedÅr etter periodens sluttår gir tom liste`() {
        val periode = medlemskapsperiodeForTest {
            fom = LocalDate.of(2024, 3, 1)
            tom = LocalDate.of(2024, 11, 30)
        }

        val resultat = SkattepliktigTrygdeavgiftsperiodeSplitter.splittPåÅr(periode, fraOgMedÅr = 2025)

        resultat.shouldBeEmpty()
    }

    @Test
    fun `alle trygdeavgiftsperioder har trygdesats null og beløp null`() {
        val periode = medlemskapsperiodeForTest {
            fom = LocalDate.of(2024, 1, 1)
            tom = LocalDate.of(2025, 12, 31)
        }

        SkattepliktigTrygdeavgiftsperiodeSplitter.splittPåÅr(periode).forEach {
            it.trygdesats.shouldBe(java.math.BigDecimal.ZERO)
            it.trygdeavgiftsbeløpMd.shouldBe(no.nav.melosys.domain.avgift.Penger(java.math.BigDecimal.ZERO))
            it.grunnlagInntekstperiode.shouldBe(null)
        }
    }

    private fun no.nav.melosys.domain.avgift.Trygdeavgiftsperiode.assertSkattepliktigSkatteforhold(
        forventetFom: LocalDate,
        forventetTom: LocalDate
    ) {
        grunnlagSkatteforholdTilNorge.shouldNotBeNull().run {
            fomDato.shouldBe(forventetFom)
            tomDato.shouldBe(forventetTom)
            skatteplikttype.shouldBe(Skatteplikttype.SKATTEPLIKTIG)
        }
    }
}
