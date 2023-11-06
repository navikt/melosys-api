package no.nav.melosys.domain.dokument.organisasjon

import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.dokument.DokumentFactory
import no.nav.melosys.domain.dokument.KonverteringTest
import no.nav.melosys.domain.dokument.XsltTemplatesFactory
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig
import no.nav.melosys.domain.dokument.organisasjon.adresse.Gateadresse
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.IOException
import java.time.LocalDate
import java.util.*

// Denne konverteringen testes også i DokumentFactoryTest, uten strukturert adresse

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class Ereg4KonverteringTest : KonverteringTest {
    @Test
    @Throws(Exception::class)
    fun testAdresse() {
        val test = getSaksopplysning(EREG_4_0_MOCK)

        // Test semistrukturert adresse...
        val dokument = test!!.dokument as OrganisasjonDokument
        val postadresse = dokument.getOrganisasjonDetaljer()!!.postadresse[0] as SemistrukturertAdresse
        Assertions.assertThat(postadresse.adresselinje1).isEqualTo("Skuteviksbodene 1")
        Assertions.assertThat(postadresse.postnr).isEqualTo("5035")
        Assertions.assertThat(postadresse.kommunenr).isEqualTo("1201")

        // Test strukturert adresse...
        val forretningsadresse = dokument.getOrganisasjonDetaljer()!!.forretningsadresse[0] as Gateadresse
        Assertions.assertThat(forretningsadresse.gatenavn).isEqualTo("Gatenavn")
        Assertions.assertThat(forretningsadresse.landkode).isEqualTo("NO")

        // Test perioder:
        // Forretningsadresse har ingen perioder satt...
        Assertions.assertThat(forretningsadresse.bruksperiode).isNull()
        Assertions.assertThat(forretningsadresse.gyldighetsperiode).isNull()
        // Postadresse har fom-dato på begge periodene, men ikke tom-dato...
        Assertions.assertThat(postadresse.gyldighetsperiode!!.fom).isEqualTo(LocalDate.of(2011, 9, 14))
        Assertions.assertThat(postadresse.gyldighetsperiode!!.tom).isNull()
        Assertions.assertThat(postadresse.bruksperiode!!.fom).isEqualTo(LocalDate.of(2015, 2, 23))
        Assertions.assertThat(postadresse.bruksperiode!!.tom).isNull()
    }

    @Test
    @Throws(IOException::class)
    fun testJuridiskEnhet() {
        val saksopplysning = getSaksopplysning(EREG_4_0_MOCK)
        val dokument = saksopplysning!!.dokument as OrganisasjonDokument
        Assertions.assertThat(dokument.sektorkode).isNotBlank()
        Assertions.assertThat(dokument.getOrganisasjonDetaljer()!!.naering).isNotEmpty()
        Assertions.assertThat(dokument.oppstartsdato).isNull()
        Assertions.assertThat(dokument.enhetstype).isNotBlank()
    }

    @Test
    @Throws(IOException::class)
    fun testOrgledd() {
        val ressurs = "organisasjon/974652366.xml"
        val saksopplysning = getSaksopplysning(ressurs)
        val dokument = saksopplysning!!.getDokument() as OrganisasjonDokument
        Assertions.assertThat(dokument.sektorkode).isNotBlank()
        Assertions.assertThat(dokument.getOrganisasjonDetaljer()!!.naering).isNotEmpty()
        Assertions.assertThat(dokument.oppstartsdato).isNull()
        Assertions.assertThat(dokument.enhetstype).isEmpty()
    }

    @Test
    @Throws(IOException::class)
    fun testVirksomhet() {
        val ressurs = "organisasjon/975270211.xml"
        val saksopplysning = getSaksopplysning(ressurs)
        val dokument = saksopplysning!!.dokument as OrganisasjonDokument
        Assertions.assertThat(dokument.sektorkode).isEmpty()
        Assertions.assertThat(dokument.getOrganisasjonDetaljer()!!.naering).isNotEmpty()
        Assertions.assertThat(dokument.oppstartsdato).isNotNull()
        Assertions.assertThat(dokument.enhetstype).isEmpty()
    }

    @Throws(IOException::class)
    override fun getSaksopplysning(ressurs: String?): Saksopplysning? {
        val kilde = javaClass.getClassLoader().getResourceAsStream(ressurs)
        Objects.requireNonNull(kilde)
        return konverter(kilde, factory, SaksopplysningType.ORG, "4.0")
    }

    companion object {
        private const val EREG_4_0_MOCK: String = "organisasjon/org_med_strukturert_adresse.xml"
        private var factory: DokumentFactory? = null
        @BeforeAll
        @JvmStatic
        fun setUp() {
            val marshaller = JaxbConfig.getJaxb2Marshaller()
            val xsltTemplatesFactory = XsltTemplatesFactory()
            factory = DokumentFactory(marshaller, xsltTemplatesFactory)
        }
    }
}
