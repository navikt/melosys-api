package no.nav.melosys.domain.avgift

import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TrygdeavgiftsperiodeTest {

    @Test
    fun `harSammenslåtteInntektskilder er false når én inntekt er splittet på to skatteforholdsperioder`() {
        val trygdeavgiftsperiode = Trygdeavgiftsperiode.forTest {
            periodeFra = LocalDate.of(2026, 1, 1)
            periodeTil = LocalDate.of(2026, 12, 31)
        }

        val inntekt = inntektForTest {
            id = 1
            fomDato = LocalDate.of(2026, 1, 1)
            tomDato = LocalDate.of(2026, 12, 31)
            type = Inntektskildetype.UFØRETRYGD
        }

        val skattepliktig = skatteforholdForTest {
            id = 10
            fomDato = LocalDate.of(2026, 1, 1)
            tomDato = LocalDate.of(2026, 6, 30)
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
        }
        val ikkeSkattepliktig = skatteforholdForTest {
            id = 11
            fomDato = LocalDate.of(2026, 7, 1)
            tomDato = LocalDate.of(2026, 12, 31)
            skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
        }

        trygdeavgiftsperiode.leggTilGrunnlag(grunnlag(trygdeavgiftsperiode, 100, inntekt, skattepliktig))
        trygdeavgiftsperiode.leggTilGrunnlag(grunnlag(trygdeavgiftsperiode, 101, inntekt, ikkeSkattepliktig))

        trygdeavgiftsperiode.grunnlagListe.size shouldBe 2
        trygdeavgiftsperiode.harSammenslåtteInntektskilder shouldBe false
    }

    @Test
    fun `harSammenslåtteInntektskilder er true når to ulike inntekter er slått sammen`() {
        val trygdeavgiftsperiode = Trygdeavgiftsperiode.forTest {
            periodeFra = LocalDate.of(2026, 1, 1)
            periodeTil = LocalDate.of(2026, 12, 31)
        }

        val skatteforhold = skatteforholdForTest {
            id = 10
            fomDato = LocalDate.of(2026, 1, 1)
            tomDato = LocalDate.of(2026, 12, 31)
            skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
        }

        val uføretrygd = inntektForTest {
            id = 1
            fomDato = LocalDate.of(2026, 1, 1)
            tomDato = LocalDate.of(2026, 12, 31)
            type = Inntektskildetype.UFØRETRYGD
        }
        val inntektFraUtlandet = inntektForTest {
            id = 2
            fomDato = LocalDate.of(2026, 1, 1)
            tomDato = LocalDate.of(2026, 12, 31)
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
        }

        trygdeavgiftsperiode.leggTilGrunnlag(grunnlag(trygdeavgiftsperiode, 100, uføretrygd, skatteforhold))
        trygdeavgiftsperiode.leggTilGrunnlag(grunnlag(trygdeavgiftsperiode, 101, inntektFraUtlandet, skatteforhold))

        trygdeavgiftsperiode.harSammenslåtteInntektskilder shouldBe true
    }

    @Test
    fun `harSammenslåtteInntektskilder er false ved ett enkelt grunnlag`() {
        val trygdeavgiftsperiode = Trygdeavgiftsperiode.forTest()

        val inntekt = inntektForTest {
            id = 1
            fomDato = LocalDate.of(2026, 1, 1)
            tomDato = LocalDate.of(2026, 12, 31)
        }
        val skatteforhold = skatteforholdForTest {
            id = 10
            fomDato = LocalDate.of(2026, 1, 1)
            tomDato = LocalDate.of(2026, 12, 31)
        }

        trygdeavgiftsperiode.leggTilGrunnlag(grunnlag(trygdeavgiftsperiode, 100, inntekt, skatteforhold))

        trygdeavgiftsperiode.harSammenslåtteInntektskilder shouldBe false
    }

    @Test
    fun `harSammenslåtteInntektskilder er true for to ulike ikke-persisterte inntekter uten id`() {
        val trygdeavgiftsperiode = Trygdeavgiftsperiode.forTest()

        val skatteforhold = skatteforholdForTest {
            fomDato = LocalDate.of(2026, 1, 1)
            tomDato = LocalDate.of(2026, 12, 31)
        }

        val uføretrygd = inntektForTest {
            fomDato = LocalDate.of(2026, 1, 1)
            tomDato = LocalDate.of(2026, 12, 31)
            type = Inntektskildetype.UFØRETRYGD
        }
        val inntektFraUtlandet = inntektForTest {
            fomDato = LocalDate.of(2026, 1, 1)
            tomDato = LocalDate.of(2026, 12, 31)
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
        }

        trygdeavgiftsperiode.leggTilGrunnlag(grunnlag(trygdeavgiftsperiode, 100, uføretrygd, skatteforhold))
        trygdeavgiftsperiode.leggTilGrunnlag(grunnlag(trygdeavgiftsperiode, 101, inntektFraUtlandet, skatteforhold))

        trygdeavgiftsperiode.harSammenslåtteInntektskilder shouldBe true
    }

    private fun grunnlag(
        trygdeavgiftsperiode: Trygdeavgiftsperiode,
        id: Long,
        inntektsperiode: Inntektsperiode,
        skatteforhold: SkatteforholdTilNorge
    ) = TrygdeavgiftsperiodeGrunnlag(
        id = id,
        trygdeavgiftsperiode = trygdeavgiftsperiode,
        inntektsperiode = inntektsperiode,
        skatteforhold = skatteforhold
    )
}
