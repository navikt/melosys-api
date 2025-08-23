package no.nav.melosys.service.altinn

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBElement
import jakarta.xml.bind.JAXBException
import no.nav.melosys.domain.kodeverk.Flyvningstyper.INTERNASJONAL
import no.nav.melosys.domain.kodeverk.Innretningstyper
import no.nav.melosys.domain.kodeverk.begrunnelser.Fartsomrader.INNENRIKS
import no.nav.melosys.domain.kodeverk.begrunnelser.Fartsomrader.UTENRIKS
import no.nav.melosys.domain.mottatteopplysninger.data.UtenlandskIdent
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.ArbeidsstedType
import no.nav.melosys.soknad_altinn.MedlemskapArbeidEOSM
import no.nav.melosys.soknad_altinn.ObjectFactory
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class SoeknadMapperTest {

    @Test
    fun `test søknad mapping`() {
        val medlemskapArbeidEOSM = parseSøknadXML()


        val soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM).apply {
            soeknadsland.landkoder shouldContain "FI"
            periode.fom shouldBe LocalDate.of(2019, 8, 5)
            periode.tom shouldBe LocalDate.of(2019, 8, 6)
            personOpplysninger.utenlandskIdent shouldContain UtenlandskIdent("utenlandskIDnummer", "FI")
            personOpplysninger.foedestedOgLand?.foedested shouldBe "Oslo"
            personOpplysninger.foedestedOgLand?.foedeland shouldBe "NO"
            arbeidPaaLand.fysiskeArbeidssteder.shouldNotBeEmpty()
            arbeidPaaLand.erFastArbeidssted shouldBe false
            arbeidPaaLand.erHjemmekontor shouldBe true
        }

        soeknad.arbeidPaaLand.fysiskeArbeidssteder[0].run {
            virksomhetNavn shouldBe "Firmaet"
            adresse.gatenavn shouldBe "Gaten 1"
            adresse.husnummerEtasjeLeilighet.shouldBeNull()
            adresse.postnummer shouldBe "1234"
            adresse.poststed shouldBe "Stedet"
            adresse.region shouldBe "Region"
            adresse.landkode shouldBe "BE"
        }

        soeknad.loennOgGodtgjoerelse.shouldNotBeNull().run {
            norskArbgUtbetalerLoenn shouldBe true
            erArbeidstakerAnsattHelePerioden shouldBe true
            utlArbgUtbetalerLoenn shouldBe true
            utlArbTilhoererSammeKonsern shouldBe false
            bruttoLoennPerMnd shouldBe BigDecimal("2000.00")
            bruttoLoennUtlandPerMnd shouldBe BigDecimal("1000.00")
            mottarNaturalytelser shouldBe false
            samletVerdiNaturalytelser shouldBe BigDecimal("10000.50")
            erArbeidsgiveravgiftHelePerioden shouldBe true
            erTrukketTrygdeavgift shouldBe true
        }

        soeknad.foretakUtland[0].run {
            navn shouldBe "Virskomheten i utlandet"
            orgnr shouldBe "XYZ123456789"
            adresse.gatenavn shouldBe "gatenavn med mer"
            adresse.poststed shouldBe "testbyen"
            adresse.postnummer shouldBe "UTLAND-1234"
            adresse.landkode shouldBe "BE"
        }

        soeknad.utenlandsoppdraget.run {
            erErstatningTidligereUtsendte shouldBe false
            samletUtsendingsperiode.shouldNotBeNull()
            samletUtsendingsperiode.fom.shouldBeNull()
            erUtsendelseForOppdragIUtlandet shouldBe false
            erFortsattAnsattEtterOppdraget.shouldBeNull()
            erAnsattForOppdragIUtlandet shouldBe false
            erDrattPaaEgetInitiativ shouldBe false
        }
    }

    @Test
    fun `test mapping arbeidsgiver`() {
        val medlemskapArbeidEOSM = parseSøknadXML()
        medlemskapArbeidEOSM.innhold.arbeidsgiver.setOffentligVirksomhet(true)


        val soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM)


        soeknad.juridiskArbeidsgiverNorge.run {
            erOffentligVirksomhet shouldBe true
            antallAnsatte.shouldBeNull()
            antallAdmAnsatte.shouldBeNull()
            antallUtsendte.shouldBeNull()
            andelOmsetningINorge.shouldBeNull()
            andelOppdragINorge.shouldBeNull()
            andelKontrakterINorge.shouldBeNull()
            andelRekruttertINorge.shouldBeNull()
            ekstraArbeidsgivere.shouldBeEmpty()
        }

        medlemskapArbeidEOSM.innhold.arbeidsgiver.setOffentligVirksomhet(false)


        val soeknad2 = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM)


        soeknad2.juridiskArbeidsgiverNorge.run {
            erOffentligVirksomhet shouldBe false
            antallAnsatte shouldBe 100
            antallAdmAnsatte shouldBe 10
            antallUtsendte shouldBe 10
            andelOmsetningINorge shouldBe BigDecimal(90)
            andelOppdragINorge shouldBe BigDecimal(90)
            andelKontrakterINorge shouldBe BigDecimal(90)
            andelRekruttertINorge shouldBe BigDecimal(90)
            ekstraArbeidsgivere shouldContain "910825569"
        }
    }

    @Test
    fun `test mapping offshore arbeidssteder`() {
        val medlemskapArbeidEOSM = parseSøknadXML()
        medlemskapArbeidEOSM.innhold.midlertidigUtsendt.arbeidssted.typeArbeidssted = ArbeidsstedType.OFFSHORE.toString()


        val soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM)


        soeknad.maritimtArbeid.shouldHaveSize(3).first().run {
            enhetNavn shouldBe "Landplattform"
            innretningstype shouldBe Innretningstyper.PLATTFORM
            innretningLandkode shouldBe "CH"
        }
    }

    @Test
    fun `test mapping skipsfart`() {
        val medlemskapArbeidEOSM = parseSøknadXML()
        medlemskapArbeidEOSM.innhold.midlertidigUtsendt.arbeidssted.typeArbeidssted = ArbeidsstedType.SKIPSFART.toString()


        val soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM)


        soeknad.maritimtArbeid.shouldHaveSize(2).first().run {
            enhetNavn shouldBe "abcd"
            fartsomradeKode shouldBe INNENRIKS
            territorialfarvannLandkode shouldBe "BG"
        }
        val maritimtArbeidUtenriks = soeknad.maritimtArbeid[1]
        maritimtArbeidUtenriks.run {
            fartsomradeKode shouldBe UTENRIKS
            flaggLandkode shouldBe "FO"
        }
    }

    @Test
    fun `test mapping luftfart baser`() {
        val medlemskapArbeidEOSM = parseSøknadXML()
        medlemskapArbeidEOSM.innhold.midlertidigUtsendt.arbeidssted.typeArbeidssted = ArbeidsstedType.LUFTFART.toString()


        val soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM)


        soeknad.luftfartBaser.shouldHaveSize(2).first().run {
            hjemmebaseNavn shouldBe "koti"
            hjemmebaseLand shouldBe "FI"
            typeFlyvninger shouldBe INTERNASJONAL
        }
    }

    @Test
    fun `test mapping samlet utsendingsperiode`() {
        val medlemskapArbeidEOSM = parseSøknadXML()
        medlemskapArbeidEOSM.innhold.midlertidigUtsendt.utenlandsoppdraget.setErstatterTidligereUtsendte(true)


        val soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM)


        soeknad.utenlandsoppdraget.run {
            erErstatningTidligereUtsendte shouldBe true
            samletUtsendingsperiode.shouldNotBeNull()
            samletUtsendingsperiode.fom shouldBe LocalDate.of(2019, 8, 1)
            samletUtsendingsperiode.tom shouldBe LocalDate.of(2019, 8, 6)
        }
    }

    @Test
    fun `test arbeidssituasjon og oevrig`() {
        val medlemskapArbeidEOSM = parseSøknadXML()


        val soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM)


        soeknad.arbeidssituasjonOgOevrig.run {
            harLoennetArbeidMinstEnMndFoerUtsending shouldBe true
            beskrivelseArbeidSisteMnd shouldBe "Arbeid siste mnd"
            harAndreArbeidsgivereIUtsendingsperioden shouldBe false
            beskrivelseAnnetArbeid shouldBe "Annet arbeid"
            erSkattepliktig shouldBe true
            mottarYtelserNorge shouldBe false
            mottarYtelserUtlandet shouldBe false
        }
    }

    private fun parseSøknadXML(): MedlemskapArbeidEOSM {
        val jaxbContext = JAXBContext.newInstance(ObjectFactory::class.java)
        val url = javaClass.classLoader.getResource("altinn/NAV_MedlemskapArbeidEOS.xml")
        return try {
            (jaxbContext.createUnmarshaller().unmarshal(url) as JAXBElement<MedlemskapArbeidEOSM>).value
        } catch (e: JAXBException) {
            throw IllegalStateException(e)
        }
    }
}
