package no.nav.melosys.tjenester.gui.util

import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import java.time.LocalDate

object SaksbehandlingDataFactory {

    fun lagFagsak(): Fagsak = Fagsak.forTest {
        medBruker()
        medGsakSaksnummer()
    }

    fun lagSøknadDokument(): Soeknad = Soeknad().apply {
        soeknadsland.landkoder.add(Landkoder.DK.kode)
        soeknadsland.isFlereLandUkjentHvilke = false
        arbeidPaaLand.fysiskeArbeidssteder = mutableListOf<FysiskArbeidssted>().apply {
            add(FysiskArbeidssted().apply {
                adresse.landkode = "SE"
            })
        }
        (oppholdUtland.oppholdslandkoder as MutableList<String>).add("FI")
        periode = no.nav.melosys.domain.mottatteopplysninger.data.Periode(
            LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1)
        )
    }
}
