package no.nav.melosys.service.ftrl

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import org.junit.jupiter.api.Test

class GyldigeInnvilgelsesResultatTest {

    @Test
    fun hentInnvilgelsesResultat_behandlingstypeManglendeInnbetaling_returnererKorrektListe() {
        GyldigeInnvilgelsesResultat.hentInnvilgelsesResultat(Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT)
            .shouldNotBeNull()
            .shouldHaveSize(3)
            .shouldContainExactly(
                InnvilgelsesResultat.INNVILGET,
                InnvilgelsesResultat.AVSLAATT,
                InnvilgelsesResultat.OPPHØRT
            )
    }

    @Test
    fun hentInnvilgelsesResultat_behandlingstypeIkkeManglendeInnbetaling_returnererKorrektListe() {
        GyldigeInnvilgelsesResultat.hentInnvilgelsesResultat(Behandlingstyper.FØRSTEGANG)
            .shouldNotBeNull()
            .shouldHaveSize(2)
            .shouldContainExactly(
                InnvilgelsesResultat.INNVILGET,
                InnvilgelsesResultat.AVSLAATT
            )
    }
}
