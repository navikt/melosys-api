package no.nav.melosys.integrasjon.trygdeavgift

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.*
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test

class TrygdeavgiftsberegningsRequestMapperTest {

    @Test
    fun `sjekk at mapping fra trygdeavgiftBeregning blir mappet som forventet`() {
        val mapper = TrygdeavgiftsberegningsRequestMapper()

        val medlemskapsperioder = lagMedlemskapsperioder()
        val skatteforholdTilNorge = lagSkatteforholdTilNorge()
        val inntektsperioder = lagInntektsperioder()

        val (request, mapsList) = mapper.map(medlemskapsperioder, skatteforholdTilNorge, inntektsperioder)
        request.medlemskapsperioder.first().run {
            avgiftsdekninger.containsAll(listOf(Avgiftsdekning.HELSEDEL_MED_SYKEPENGER, Avgiftsdekning.PENSJONSDEL_MED_YRKESSKADETRYGD)) shouldBe true
            periode.fom shouldBe medlemskapsperioder[0].fom
            periode.tom shouldBe medlemskapsperioder[0].tom
        }

        request.skatteforholdsperioder.first().run {
            periode.fom shouldBe skatteforholdTilNorge[0].fomDato
            periode.tom shouldBe skatteforholdTilNorge[0].tomDato
            skatteforhold.kode shouldBe skatteforholdTilNorge[0].skatteplikttype.kode
            skatteforhold.beskrivelse shouldBe skatteforholdTilNorge[0].skatteplikttype.beskrivelse
        }

        request.inntektsperioder[0].run {
            trygdeavgiftBetalesTilSkatt shouldBe inntektsperioder[0].isOrdinærTrygdeavgiftBetalesTilSkatt
            periode.fom shouldBe inntektsperioder[0].fomDato
            periode.tom shouldBe inntektsperioder[0].tomDato
            inntektskilde shouldBe inntektsperioder[0].type
            månedsbeløp?.verdi shouldBe inntektsperioder[0].avgiftspliktigInntektMnd.verdi
            månedsbeløp?.valuta?.kode shouldBe inntektsperioder[0].avgiftspliktigInntektMnd.valuta
        }

        mapsList.shouldHaveSize(3)
        mapsList[0].shouldHaveSize(2)
        mapsList[1].shouldHaveSize(2)
        mapsList[2].shouldHaveSize(2)
    }

    private fun lagMedlemskapsperioder(): List<Medlemskapsperiode> {
        return listOf(
            Medlemskapsperiode().apply {
                id = 1L
                fom = LocalDate.of(2022, 1, 1)
                tom = LocalDate.of(2022, 12, 31)
                arbeidsland = "Norway"
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.PLIKTIG
                trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7A
                medlPeriodeID = 1L
            },
            Medlemskapsperiode().apply {
                id = 1L
                fom = LocalDate.of(2020, 1, 1)
                tom = LocalDate.of(2021, 12, 31)
                arbeidsland = "Norway"
                innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT
                medlemskapstype = Medlemskapstyper.PLIKTIG
                trygdedekning = Trygdedekninger.FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_7A
                medlPeriodeID = 1L
            })
    }

    private fun lagSkatteforholdTilNorge(): List<SkatteforholdTilNorge> {
        return listOf(
            SkatteforholdTilNorge().apply {
                id = 1L
                fomDato = LocalDate.of(2022, 1, 1)
                tomDato = LocalDate.of(2023, 1, 1)
                skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
            },
            SkatteforholdTilNorge().apply {
                id = 1L
                fomDato = LocalDate.of(2020, 1, 1)
                tomDato = LocalDate.of(2021, 1, 1)
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            },
        )
    }

    private fun lagInntektsperioder(): List<Inntektsperiode> {
        return listOf(
            Inntektsperiode().apply {
                id = 1L
                fomDato = LocalDate.of(2020, 1, 1)
                tomDato = LocalDate.of(2021, 1, 1)
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                avgiftspliktigInntektMnd = Penger(BigDecimal("1000.00"), "EUR")
            },
            Inntektsperiode().apply {
                id = 1L
                fomDato = LocalDate.of(2022, 1, 1)
                tomDato = LocalDate.of(2023, 1, 1)
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                avgiftspliktigInntektMnd = Penger(BigDecimal("2000.00"), "USD")
            }
        )
    }
}
