package no.nav.melosys.service

import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.ForetakUtland
import no.nav.melosys.domain.mottatteopplysninger.data.SelvstendigForetak
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted

object MottatteOpplysningerStub {

    @JvmStatic
    fun lagMottatteOpplysninger(
        selvstendigeForetak: List<String>,
        foretakUtland: List<ForetakUtland>,
        ekstraArbeidsgivere: List<String>
    ): MottatteOpplysninger {
        val mottatteOpplysninger = MottatteOpplysninger()
        mottatteOpplysninger.type = Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS
        mottatteOpplysninger.mottatteOpplysningerData = lagMottatteOpplysningerdata(
            selvstendigeForetak,
            foretakUtland,
            ekstraArbeidsgivere
        )
        return mottatteOpplysninger
    }

    private fun lagMottatteOpplysningerdata(
        selvstendigeForetak: List<String>,
        foretakUtland: List<ForetakUtland>,
        ekstraArbeidsgivere: List<String>
    ): MottatteOpplysningerData {
        val søknad = Soeknad()

        // Create mutable list for selvstendigForetak
        val selvstendigForetakList = ArrayList<SelvstendigForetak>()
        for (orgnr in selvstendigeForetak) {
            val selvstendigForetakObj = SelvstendigForetak()
            selvstendigForetakObj.orgnr = orgnr
            selvstendigForetakList.add(selvstendigForetakObj)
        }
        søknad.selvstendigArbeid.selvstendigForetak = selvstendigForetakList

        val fysiskArbeidssted = FysiskArbeidssted()
        fysiskArbeidssted.adresse.landkode = "DE"
        val fysiskeArbeidsstederList = ArrayList<FysiskArbeidssted>()
        fysiskeArbeidsstederList.add(fysiskArbeidssted)
        søknad.arbeidPaaLand.fysiskeArbeidssteder = fysiskeArbeidsstederList
        søknad.juridiskArbeidsgiverNorge.ekstraArbeidsgivere = ekstraArbeidsgivere
        søknad.foretakUtland = foretakUtland
        søknad.soeknadsland.landkoder.add("DE")

        return søknad
    }
}
