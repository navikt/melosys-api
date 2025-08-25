package no.nav.melosys.service

import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.domain.mottatteopplysninger.data.SelvstendigForetak
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted

object MottatteOpplysningerStub {

    fun lagMottatteOpplysninger(
        selvstendigeForetak: List<String>,
        foretakUtland: List<ForetakUtland>,
        ekstraArbeidsgivere: List<String>
    ) = MottatteOpplysninger().apply {
        type = Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS
        mottatteOpplysningerData = lagMottatteOpplysningerdata(
            selvstendigeForetak,
            foretakUtland,
            ekstraArbeidsgivere
        )
    }

    private fun lagMottatteOpplysningerdata(
        selvstendigeForetak: List<String>,
        foretakUtland: List<ForetakUtland>,
        ekstraArbeidsgivere: List<String>
    ): MottatteOpplysningerData = Soeknad().apply {
        selvstendigArbeid.selvstendigForetak = selvstendigeForetak.map { orgnr ->
            SelvstendigForetak().apply {
                this.orgnr = orgnr
            }
        }

        arbeidPaaLand.fysiskeArbeidssteder = listOf(
            FysiskArbeidssted().apply {
                adresse.landkode = "DE"
            }
        )

        juridiskArbeidsgiverNorge.ekstraArbeidsgivere = ekstraArbeidsgivere
        this.foretakUtland = foretakUtland
        soeknadsland.landkoder.add("DE")
    }
}
