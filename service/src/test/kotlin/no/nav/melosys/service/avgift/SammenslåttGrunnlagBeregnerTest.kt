package no.nav.melosys.service.avgift

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.TrygdeavgiftsperiodeGrunnlag
import no.nav.melosys.domain.avgift.forTest
import no.nav.melosys.domain.avgift.inntektForTest
import no.nav.melosys.domain.avgift.skatteforholdForTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class SammenslåttGrunnlagBeregnerTest {

    private val årsstart: LocalDate = LocalDate.of(2024, 1, 1)
    private val årsslutt: LocalDate = LocalDate.of(2024, 12, 31)

    @Test
    fun `bruttoinntekt summerer samtidige inntekter`() {
        val periode = periodeMedGrunnlag(
            inntekt(månedsbeløp = 9000, fom = årsstart, tom = årsslutt),
            inntekt(månedsbeløp = 2000, fom = årsstart, tom = årsslutt)
        )

        SammenslåttGrunnlagBeregner.bruttoinntektPerMd(periode, verdiAvrundet = true)
            .compareTo(BigDecimal(11000)) shouldBe 0
    }

    @Test
    fun `bruttoinntekt vekter sekvensielle inntekter etter varighet`() {
        val periode = periodeMedGrunnlag(
            inntekt(månedsbeløp = 9000, fom = årsstart, tom = LocalDate.of(2024, 6, 30)),
            inntekt(månedsbeløp = 2000, fom = LocalDate.of(2024, 7, 1), tom = årsslutt)
        )

        // (9 000 x 6 + 2 000 x 6) / 12 = 5 500, ikke 11 000
        SammenslåttGrunnlagBeregner.bruttoinntektPerMd(periode, verdiAvrundet = true)
            .compareTo(BigDecimal(5500)) shouldBe 0
    }

    @Test
    fun `bruttoinntekt teller samme inntekt kun en gang selv om den er periodisert`() {
        val inntektsperiode = inntekt(månedsbeløp = 9000, fom = årsstart, tom = årsslutt, id = 1L)
        val periode = periodeMedGrunnlag(inntektsperiode, inntektsperiode)

        SammenslåttGrunnlagBeregner.bruttoinntektPerMd(periode, verdiAvrundet = true)
            .compareTo(BigDecimal(9000)) shouldBe 0
    }

    @Test
    fun `bruttoinntekt faller tilbake til legacy-feltet naar grunnlagslista er tom`() {
        val periode = Trygdeavgiftsperiode.forTest {
            periodeFra = årsstart
            periodeTil = årsslutt
            grunnlagInntekstperiode { avgiftspliktigMndInntekt = Penger(BigDecimal(7000)) }
        }

        SammenslåttGrunnlagBeregner.bruttoinntektPerMd(periode, verdiAvrundet = true)
            .compareTo(BigDecimal(7000)) shouldBe 0
    }

    @Test
    fun `bruttoinntekt bruker maanedsbeloepet direkte naar raden har en enkelt inntekt`() {
        // Beregningen kan gi en rad som ikke overlapper inntektsperioden den stammer fra. Med kun én
        // inntekt er raden den inntekten, og beløpet skal brukes som det er — ikke vektes ned til null.
        val periode = periodeMedGrunnlag(
            inntekt(månedsbeløp = 15000, fom = årsstart, tom = LocalDate.of(2024, 7, 31))
        )

        SammenslåttGrunnlagBeregner.bruttoinntektPerMd(periode, verdiAvrundet = true)
            .compareTo(BigDecimal(15000)) shouldBe 0
    }

    @Test
    fun `arbeidsgiveravgift returnerer verdien naar alle inntektene er enige`() {
        val periode = periodeMedGrunnlag(
            inntekt(månedsbeløp = 9000, fom = årsstart, tom = årsslutt, agaBetales = true),
            inntekt(månedsbeløp = 2000, fom = årsstart, tom = årsslutt, agaBetales = true)
        )

        SammenslåttGrunnlagBeregner.arbeidsgiversavgiftBetales(periode) shouldBe true
    }

    @Test
    fun `arbeidsgiveravgift returnerer null naar inntektene spriker`() {
        val periode = periodeMedGrunnlag(
            inntekt(månedsbeløp = 9000, fom = årsstart, tom = årsslutt, agaBetales = true),
            inntekt(månedsbeløp = 2000, fom = årsstart, tom = årsslutt, agaBetales = false)
        )

        SammenslåttGrunnlagBeregner.arbeidsgiversavgiftBetales(periode) shouldBe null
    }

    @Test
    fun `totalinntekt summerer maanedsinntekten over alle radene`() {
        val periode = periodeMedGrunnlag(
            inntekt(månedsbeløp = 9000, fom = årsstart, tom = årsslutt),
            inntekt(månedsbeløp = 2000, fom = årsstart, tom = årsslutt)
        )

        // 11 000 kr/md x 12 md
        SammenslåttGrunnlagBeregner.totalinntekt(listOf(periode))
            .compareTo(BigDecimal(132000)) shouldBe 0
    }

    @Test
    fun `totalinntekt behandler totalbeloep som et aarsbeloep, ikke som et maanedsbeloep`() {
        val periode = Trygdeavgiftsperiode.forTest {
            periodeFra = årsstart
            periodeTil = årsslutt
            grunnlagInntekstperiode {
                avgiftspliktigMndInntekt = null
                avgiftspliktigTotalinntekt = Penger(BigDecimal(120000))
            }
        }

        SammenslåttGrunnlagBeregner.totalinntekt(listOf(periode))
            .compareTo(BigDecimal(120000)) shouldBe 0
    }

    private fun inntekt(
        månedsbeløp: Int,
        fom: LocalDate,
        tom: LocalDate,
        agaBetales: Boolean = false,
        id: Long? = null
    ): Inntektsperiode = inntektForTest {
        this.id = id
        this.fomDato = fom
        this.tomDato = tom
        this.avgiftspliktigMndInntekt = Penger(BigDecimal(månedsbeløp))
        this.arbeidsgiversavgiftBetalesTilSkatt = agaBetales
    }

    /** Bygger en trygdeavgiftsperiode for hele 2024 med de oppgitte inntektene i grunnlagslista. */
    private fun periodeMedGrunnlag(vararg inntektsperioder: Inntektsperiode): Trygdeavgiftsperiode {
        val skatteforhold: SkatteforholdTilNorge = skatteforholdForTest {
            fomDato = årsstart
            tomDato = årsslutt
        }

        val trygdeavgiftsperiode = Trygdeavgiftsperiode.forTest {
            periodeFra = årsstart
            periodeTil = årsslutt
        }

        inntektsperioder.forEach { inntektsperiode ->
            trygdeavgiftsperiode.leggTilGrunnlag(
                TrygdeavgiftsperiodeGrunnlag(
                    trygdeavgiftsperiode = trygdeavgiftsperiode,
                    inntektsperiode = inntektsperiode,
                    skatteforhold = skatteforhold
                )
            )
        }

        return trygdeavgiftsperiode
    }
}
