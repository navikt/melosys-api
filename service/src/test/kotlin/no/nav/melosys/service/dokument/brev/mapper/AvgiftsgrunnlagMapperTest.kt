package no.nav.melosys.service.dokument.brev.mapper

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.TrygdeavgiftsperiodeGrunnlag
import no.nav.melosys.domain.avgift.forTest
import no.nav.melosys.domain.avgift.inntektForTest
import no.nav.melosys.domain.avgift.skatteforholdForTest
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.domain.medlemskapsperiodeForTest
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.integrasjon.dokgen.dto.Avgiftsgrunnlag
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class AvgiftsgrunnlagMapperTest {

    private val unleash = FakeUnleash()
    private val mapper = AvgiftsgrunnlagMapper(unleash)

    @BeforeEach
    fun setup() {
        unleash.enable(ToggleName.MELOSYS_VEDLEGG_AVGIFTSGRUNNLAG)
    }

    @Test
    fun `gir ingenting når toggle er av`() {
        unleash.disableAll()

        mapper.mapNårAvgiftBetalesTilNav(listOf(periode(2026)), Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV).shouldBeNull()
    }

    @Test
    fun `gir ingenting når all avgift betales til Skatteetaten`() {
        mapper.mapNårAvgiftBetalesTilNav(listOf(periode(2026)), Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT).shouldBeNull()
        mapper.mapNårAvgiftBetalesTilNav(listOf(periode(2026)), null).shouldBeNull()
    }

    @Test
    fun `viser skatteforhold og inntekter sortert og uten duplikater`() {
        val grunnlag = mapper.mapNårAvgiftBetalesTilNav(
            listOf(periode(2027), periode(2026), periode(2026)),
            Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
        ).shouldNotBeNull()

        grunnlag.visArbeidsgiversavgift shouldBe true
        grunnlag.skatteforholdsperioder.shouldContainExactly(
            Avgiftsgrunnlag.Skatteforholdsperiode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), Skatteplikttype.IKKE_SKATTEPLIKTIG),
            Avgiftsgrunnlag.Skatteforholdsperiode(LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31), Skatteplikttype.IKKE_SKATTEPLIKTIG),
        )
        grunnlag.inntektsperioder.map { it.fom.year } shouldBe listOf(2026, 2027)
        grunnlag.inntektsperioder.first().run {
            inntektskildetype shouldBe Inntektskildetype.INNTEKT_FRA_UTLANDET
            arbeidsgiversavgiftBetalesTilSkatt shouldBe false
            avgiftspliktigInntekt shouldBe BigDecimal("50000.0")
            erMaanedsbelop shouldBe true
        }
    }

    @Test
    fun `tar med alle inntekter som er slått sammen i én trygdeavgiftsperiode`() {
        val sammenslått = periode(2026).apply {
            leggTilGrunnlag(grunnlag(this, Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE))
            leggTilGrunnlag(grunnlag(this, Inntektskildetype.INNTEKT_FRA_UTLANDET))
        }

        mapper.mapNårAvgiftBetalesTilNav(listOf(sammenslått), Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV).shouldNotBeNull().inntektsperioder.map { it.inntektskildetype }
            .shouldContainExactly(Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE, Inntektskildetype.INNTEKT_FRA_UTLANDET)
    }

    @Test
    fun `arbeidsgiveravgift er ikke relevant ved pliktig medlemskap, og bruttoinntekt ikke når skattepliktig og arbeidsinntekt fra Norge`() {
        val pliktig = periode(
            2026,
            medlemskapstype = Medlemskapstyper.PLIKTIG,
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG,
            inntektskildetype = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE,
        )

        mapper.mapNårAvgiftBetalesTilNav(listOf(pliktig), Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV).shouldNotBeNull().inntektsperioder.single().run {
            arbeidsgiversavgiftBetalesTilSkatt.shouldBeNull()
            avgiftspliktigInntekt.shouldBeNull()
        }
    }

    @Test
    fun `bruttoinntekt er relevant for inntekt fra utlandet når arbeidsgiveravgift ikke betales, selv om skattepliktig`() {
        val frivillig = periode(2026, skatteplikttype = Skatteplikttype.SKATTEPLIKTIG)

        mapper.mapNårAvgiftBetalesTilNav(listOf(frivillig), Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV).shouldNotBeNull().inntektsperioder.single().avgiftspliktigInntekt shouldBe BigDecimal("50000.0")
    }

    @Test
    fun `arbeidsgiveravgift er ikke relevant for misjonærinntekt`() {
        val misjonær = periode(2026, inntektskildetype = Inntektskildetype.MISJONÆR)

        mapper.mapNårAvgiftBetalesTilNav(listOf(misjonær), Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV).shouldNotBeNull().inntektsperioder.single().arbeidsgiversavgiftBetalesTilSkatt.shouldBeNull()
    }

    private fun periode(
        år: Int,
        medlemskapstype: Medlemskapstyper = Medlemskapstyper.FRIVILLIG,
        skatteplikttype: Skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG,
        inntektskildetype: Inntektskildetype = Inntektskildetype.INNTEKT_FRA_UTLANDET,
    ): Trygdeavgiftsperiode = Trygdeavgiftsperiode.forTest {
        periodeFra = LocalDate.of(år, 1, 1)
        periodeTil = LocalDate.of(år, 12, 31)
        medlemskapsperiode = medlemskapsperiodeForTest { this.medlemskapstype = medlemskapstype }
        grunnlagSkatteforholdTilNorge { this.skatteplikttype = skatteplikttype }
        grunnlagInntekstperiode {
            type = inntektskildetype
            avgiftspliktigMndInntekt = Penger(50000.0)
        }
    }

    private fun grunnlag(trygdeavgiftsperiode: Trygdeavgiftsperiode, inntektskildetype: Inntektskildetype) =
        TrygdeavgiftsperiodeGrunnlag(
            trygdeavgiftsperiode = trygdeavgiftsperiode,
            medlemskapsperiode = trygdeavgiftsperiode.grunnlagMedlemskapsperiode,
            inntektsperiode = inntektForTest {
                fomDato = LocalDate.of(2026, 1, 1)
                tomDato = LocalDate.of(2026, 12, 31)
                type = inntektskildetype
            },
            skatteforhold = skatteforholdForTest {
                fomDato = LocalDate.of(2026, 1, 1)
                tomDato = LocalDate.of(2026, 12, 31)
            },
        )
}
