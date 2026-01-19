package no.nav.melosys.domain.mottatteopplysninger

import no.nav.melosys.domain.MelosysTestDsl
import no.nav.melosys.domain.kodeverk.begrunnelser.Ikkeyrkesaktivsituasjontype
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland

fun søknadIkkeYrkesaktivForTest(init: SøknadIkkeYrkesaktivTestFactory.Builder.() -> Unit = {}): SøknadIkkeYrkesaktiv =
    SøknadIkkeYrkesaktivTestFactory.Builder().apply(init).build()

fun MottatteOpplysningerTestFactory.Builder.søknadIkkeYrkesaktiv(init: SøknadIkkeYrkesaktivTestFactory.Builder.() -> Unit) = apply {
    this.mottatteOpplysningerData = søknadIkkeYrkesaktivForTest(init)
}

object SøknadIkkeYrkesaktivTestFactory {
    @MelosysTestDsl
    class Builder {
        var ikkeYrkesaktivSituasjontype: Ikkeyrkesaktivsituasjontype? = null
        private val soeknadslandkoder = mutableListOf<String>()
        private var oppholdUtlandsLand: Boolean = false

        fun landkoder(vararg landkoder: String) = apply {
            soeknadslandkoder.addAll(landkoder)
        }

        fun oppholdUtlandsLand(oppholdUtlandsLand: Boolean) = apply {
            this.oppholdUtlandsLand = oppholdUtlandsLand
        }

        fun build(): SøknadIkkeYrkesaktiv = SøknadIkkeYrkesaktiv().apply {
            this.ikkeYrkesaktivSituasjontype = this@Builder.ikkeYrkesaktivSituasjontype
            this.soeknadsland = Soeknadsland(this@Builder.soeknadslandkoder, this@Builder.oppholdUtlandsLand)
        }
    }
}
