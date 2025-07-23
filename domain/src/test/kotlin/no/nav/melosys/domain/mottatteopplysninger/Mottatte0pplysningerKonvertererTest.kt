package no.nav.melosys.domain.mottatteopplysninger

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper
import kotlin.test.Test

class MottatteOpplysningerKonvertererTest {

    @Test
    fun `skal fungere å konverte fra json til dto og tilbake til json`() {
        val json = javaClass.classLoader.getResource("soeknad/soeknad.json")?.readText()
            ?: throw IllegalArgumentException("Kunne ikke lese json fra 'soeknad/soeknad.json'")

        MottatteOpplysninger().apply {
            type = Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS
            jsonData = json
        }.also { mottatteOpplysninger ->
            MottatteOpplysningerKonverterer.lastMottatteOpplysninger(mottatteOpplysninger)

            mottatteOpplysninger.mottatteOpplysningerData.shouldBeInstanceOf<Soeknad>()
            mottatteOpplysninger.jsonData = null

            MottatteOpplysningerKonverterer.oppdaterMottatteOpplysninger(mottatteOpplysninger)
            mottatteOpplysninger.jsonData shouldEqualJson json
        }
    }
}
