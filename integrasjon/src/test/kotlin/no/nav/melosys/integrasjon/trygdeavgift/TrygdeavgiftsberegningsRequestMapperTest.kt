package no.nav.melosys.integrasjon.trygdeavgift

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.*
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrygdeavgiftsberegningsRequestMapperTest {

    @Test
    fun `sjekk at mapping fra trygdeavgiftBeregning blir mappet som forventet`() {
        val mapper = TrygdeavgiftsberegningsRequestMapper()

        val medlemskapsperioder = lagMedlemskapsperioder()
        val skatteforholdTilNorge = lagSkatteforholdTilNorge()
        val inntektsperioder = lagInntektsperioder()

        val (request, mapsList) = mapper.map(medlemskapsperioder, skatteforholdTilNorge, inntektsperioder)
        assertTrue(request.medlemskapsperioder.first().avgiftsdekninger.containsAll(
            listOf(Avgiftsdekning.HELSEDEL_MED_SYKEPENGER, Avgiftsdekning.PENSJONSDEL_MED_YRKESSKADETRYGD)))
        assertEquals(request.medlemskapsperioder.first().periode.fom, medlemskapsperioder[0].fom)
        assertEquals(request.medlemskapsperioder.first().periode.tom,  medlemskapsperioder[0].tom)

        assertEquals(request.skatteforholdsperioder.first().periode.fom, skatteforholdTilNorge[0].fomDato)
        assertEquals(request.skatteforholdsperioder.first().periode.tom, skatteforholdTilNorge[0].tomDato)
        assertEquals(request.skatteforholdsperioder.first().skatteforhold.kode,
            skatteforholdTilNorge[0].skatteplikttype.kode)
        assertEquals(request.skatteforholdsperioder.first().skatteforhold.beskrivelse,
            skatteforholdTilNorge[0].skatteplikttype.beskrivelse)

        assertEquals(request.inntektsperioder[0].trygdeavgiftBetalesTilSkatt,
            inntektsperioder[0].isOrdinærTrygdeavgiftBetalesTilSkatt)
        assertEquals(request.inntektsperioder[0].periode.fom, inntektsperioder[0].fomDato)
        assertEquals(request.inntektsperioder[0].periode.tom, inntektsperioder[0].tomDato)
        assertEquals(request.inntektsperioder[0].inntektskilde, inntektsperioder[0].type)
        assertEquals(request.inntektsperioder[0].månedsbeløp?.verdi, inntektsperioder[0].avgiftspliktigInntektMnd.verdi)
        assertEquals(request.inntektsperioder[0].månedsbeløp?.valuta?.kode, inntektsperioder[0].avgiftspliktigInntektMnd.valuta)

        assertTrue(mapsList.size == 3)
        assertTrue { mapsList[0].size == 2 }
        assertTrue { mapsList[1].size == 2 }
        assertTrue { mapsList[2].size == 2 }
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
