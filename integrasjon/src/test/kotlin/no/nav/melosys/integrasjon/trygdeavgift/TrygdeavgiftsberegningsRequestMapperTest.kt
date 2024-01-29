package no.nav.melosys.integrasjon.trygdeavgift

import io.kotest.mpp.log
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.*
import java.time.LocalDate
import kotlin.test.Test

class TrygdeavgiftsberegningsRequestMapperTest {

    @Test
    fun test_MapperKorrekt() {
        val mapper = TrygdeavgiftsberegningsRequestMapper()

        val medlemskapsperioder = listOf(
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
            })

        val skatteforholdTilNorge = listOf(
            SkatteforholdTilNorge().apply {
                id = 1L
                fomDato = LocalDate.of(2022, 1, 1)
                tomDato = LocalDate.of(2023, 1, 1)
                skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
            },
        )

        val inntektsperioder = listOf(
            Inntektsperiode().apply {
                id = 1L
                fomDato = LocalDate.of(2022, 1, 1)
                tomDato = LocalDate.of(2023, 1, 1)
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
            }
        )

        val (request, mapsList) = mapper.map(medlemskapsperioder, skatteforholdTilNorge, inntektsperioder)
        log { "Request: $request" }
        log { "Maps: $mapsList" }

        assert(request.medlemskapsperioder.first().avgiftsdekninger.containsAll(listOf(Avgiftsdekning.HELSEDEL_MED_SYKEPENGER,
            Avgiftsdekning.PENSJONSDEL_MED_YRKESSKADETRYGD)))
        assert(request.medlemskapsperioder.first().periode.fom == medlemskapsperioder.get(0).fom)
        assert(request.medlemskapsperioder.first().periode.tom == medlemskapsperioder.get(0).tom)


    }
}
