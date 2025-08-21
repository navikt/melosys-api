package no.nav.melosys.domain.jpa

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.melosys.domain.kodeverk.Mottatteopplysningertyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.SøknadNorgeEllerUtenforEØS
import no.nav.melosys.domain.readResourceAsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.URISyntaxException

internal class MottatteOpplysningerListenerTest {

    private val mottatteOpplysningerListener = MottatteOpplysningerListener()
    private lateinit var mottatteOpplysninger: MottatteOpplysninger

    @BeforeEach
    fun setup() {
        mottatteOpplysninger = MottatteOpplysninger()
    }

    @Test
    @Throws(URISyntaxException::class, IOException::class)
    fun lastMottatteOpplysninger_erSøknadFtrl_forventTypeSøknadNorgeEllerUtenforEØS() {
        val json = readResourceAsString("soeknad/soeknad.json")

        mottatteOpplysninger.apply {
            jsonData = json
            type = Mottatteopplysningertyper.SØKNAD_YRKESAKTIVE_NORGE_ELLER_UTENFOR_EØS
        }


        mottatteOpplysningerListener.lastMottatteOpplysninger(mottatteOpplysninger)


        mottatteOpplysninger.run {
            mottatteOpplysningerData.shouldNotBeNull()
            mottatteOpplysningerData.shouldBeInstanceOf<SøknadNorgeEllerUtenforEØS>()
        }
    }

    @Test
    @Throws(URISyntaxException::class, IOException::class)
    fun lastMottatteOpplysninger_erSøknadTrygdeavtale_forventTypeSøknadNorgeEllerUtenforEØS() {
        val json = readResourceAsString("soeknad/soeknad.json")
        mottatteOpplysninger.apply {
            jsonData = json
            type = Mottatteopplysningertyper.SØKNAD_YRKESAKTIVE_NORGE_ELLER_UTENFOR_EØS
        }


        mottatteOpplysningerListener.lastMottatteOpplysninger(mottatteOpplysninger)


        mottatteOpplysninger.run {
            mottatteOpplysningerData.shouldNotBeNull()
            mottatteOpplysningerData.shouldBeInstanceOf<SøknadNorgeEllerUtenforEØS>()
        }
    }

    @Test
    @Throws(URISyntaxException::class, IOException::class)
    fun lastMottatteOpplysninger_erSøknad_forventTypeSoeknad() {
        val json = readResourceAsString("soeknad/soeknad.json")
        mottatteOpplysninger.apply {
            jsonData = json
            type = Mottatteopplysningertyper.SØKNAD_A1_YRKESAKTIVE_EØS
        }


        mottatteOpplysningerListener.lastMottatteOpplysninger(mottatteOpplysninger)


        mottatteOpplysninger.mottatteOpplysningerData
            .shouldNotBeNull()
            .shouldBeInstanceOf<Soeknad>()
    }
}
