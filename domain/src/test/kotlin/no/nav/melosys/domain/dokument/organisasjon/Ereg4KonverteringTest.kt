package no.nav.melosys.domain.dokument.organisasjon

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldBeEmpty
import no.nav.melosys.domain.Saksopplysning
import no.nav.melosys.domain.SaksopplysningType
import no.nav.melosys.domain.dokument.DokumentFactory
import no.nav.melosys.domain.dokument.XsltTemplatesFactory
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig
import no.nav.melosys.domain.dokument.organisasjon.adresse.Gateadresse
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse
import no.nav.melosys.domain.dokument.KonverteringTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Ereg4KonverteringTest : KonverteringTest { // Slettes når vi fjerner jaxb

    private lateinit var factory: DokumentFactory

    @BeforeAll
    fun setUp() {
        val marshaller = JaxbConfig.getJaxb2Marshaller()
        val xsltTemplatesFactory = XsltTemplatesFactory()
        factory = DokumentFactory(marshaller, xsltTemplatesFactory)
    }

    @Test
    fun `Adresse test`() {
        val dokument = getSaksopplysning(EREG_4_0_MOCK).dokument as OrganisasjonDokument


        dokument.organisasjonDetaljer.shouldNotBeNull().run {
            val postadresse = postadresse[0] as SemistrukturertAdresse
            postadresse.run {
                adresselinje1.shouldBe("Skuteviksbodene 1")
                postnr.shouldBe("5035")
                kommunenr.shouldBe("1201")
                gyldighetsperiode.shouldNotBeNull().fom.shouldBe(LocalDate.of(2011, 9, 14))
                gyldighetsperiode.shouldNotBeNull().tom.shouldBeNull()
                bruksperiode!!.fom.shouldBe(LocalDate.of(2015, 2, 23))
                bruksperiode!!.tom.shouldBeNull()
            }

            val forretningsadresse = forretningsadresse[0] as Gateadresse
            forretningsadresse.run {
                gatenavn.shouldBe("Gatenavn")
                landkode.shouldBe("NO")
                bruksperiode.shouldBeNull()
                gyldighetsperiode.shouldBeNull()
            }
        }
    }

    @Test
    fun `Juridiskenhet test`() {
        val dokument = getSaksopplysning(EREG_4_0_MOCK).dokument as OrganisasjonDokument


        dokument.run {
            sektorkode.shouldNotBeBlank()
            organisasjonDetaljer.shouldNotBeNull()
                .naering.shouldNotBeEmpty()
            oppstartsdato.shouldBeNull()
            enhetstype.shouldNotBeBlank()
        }
    }

    @Test
    fun `Orgledd test`() {
        val dokument = getSaksopplysning(ORGLEDD_RESSURS).dokument as OrganisasjonDokument


        dokument.run {
            sektorkode.shouldNotBeBlank()
            organisasjonDetaljer.shouldNotBeNull()
                .naering.shouldNotBeEmpty()
            oppstartsdato.shouldBeNull()
            enhetstype.shouldBeEmpty()
        }
    }

    @Test
    fun `Virksomhet test`() {
        val dokument = getSaksopplysning(VIRKSOMHET_RESSURS).dokument as OrganisasjonDokument


        dokument.run {
            sektorkode.shouldBeEmpty()
            organisasjonDetaljer.shouldNotBeNull()
                .naering.shouldNotBeEmpty()
            oppstartsdato.shouldNotBeNull()
            enhetstype.shouldBeEmpty()
        }
    }

    override fun getSaksopplysning(ressurs: String?): Saksopplysning {
        val kilde = javaClass.getClassLoader()
            .getResourceAsStream(ressurs)
            .shouldNotBeNull()
        return konverter(kilde, factory, SaksopplysningType.ORG, "4.0")
    }

    companion object {
        private const val EREG_4_0_MOCK: String = "organisasjon/org_med_strukturert_adresse.xml"
        private const val ORGLEDD_RESSURS: String = "organisasjon/974652366.xml"
        private const val VIRKSOMHET_RESSURS: String = "organisasjon/975270211.xml"
    }
}
