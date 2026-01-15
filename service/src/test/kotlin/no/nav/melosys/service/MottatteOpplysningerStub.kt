package no.nav.melosys.service

import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.domain.mottatteopplysninger.mottatteOpplysningerForTest
import no.nav.melosys.domain.mottatteopplysninger.soeknadForTest

object MottatteOpplysningerStub {

    fun lagMottatteOpplysninger(
        selvstendigeForetak: List<String>,
        foretakUtland: List<ForetakUtland>,
        ekstraArbeidsgivere: List<String>
    ): MottatteOpplysninger = mottatteOpplysningerForTest {
        type = Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS
        mottatteOpplysningerData = soeknadForTest {
            selvstendigeForetak.forEach { selvstendigForetak(it) }
            fysiskeArbeidssted { landkode = "DE" }
            ekstraArbeidsgivere.forEach { ekstraArbeidsgiver(it) }
            foretakUtland.forEach { foretak -> foretakUtland(foretak) }
            landkoder("DE")
        }
    }
}
