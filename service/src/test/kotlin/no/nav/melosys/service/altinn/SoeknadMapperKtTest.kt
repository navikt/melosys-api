package no.nav.melosys.service.altinn

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
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

class SoeknadMapperKtTest {

    @Test
    fun `test søknad mapping`() {
        val medlemskapArbeidEOSM = parseSøknadXML()

        val soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM)

        soeknad.soeknadsland.landkoder shouldContain "FI"
        soeknad.periode.fom shouldBe LocalDate.of(2019, 8, 5)
        soeknad.periode.tom shouldBe LocalDate.of(2019, 8, 6)
        soeknad.personOpplysninger.utenlandskIdent shouldContain UtenlandskIdent("utenlandskIDnummer", "FI")
        soeknad.personOpplysninger.foedestedOgLand?.foedested shouldBe "Oslo"
        soeknad.personOpplysninger.foedestedOgLand?.foedeland shouldBe "NO"
        soeknad.arbeidPaaLand.fysiskeArbeidssteder.shouldNotBeEmpty()
        soeknad.arbeidPaaLand.erFastArbeidssted shouldBe false
        soeknad.arbeidPaaLand.erHjemmekontor shouldBe true

        val fysiskArbeidssted = soeknad.arbeidPaaLand.fysiskeArbeidssteder[0]
        fysiskArbeidssted.virksomhetNavn shouldBe "Firmaet"
        fysiskArbeidssted.adresse.gatenavn shouldBe "Gaten 1"
        fysiskArbeidssted.adresse.husnummerEtasjeLeilighet.shouldBeNull()
        fysiskArbeidssted.adresse.postnummer shouldBe "1234"
        fysiskArbeidssted.adresse.poststed shouldBe "Stedet"
        fysiskArbeidssted.adresse.region shouldBe "Region"
        fysiskArbeidssted.adresse.landkode shouldBe "BE"

        val loennOgGodtgjoerelse = soeknad.loennOgGodtgjoerelse
        loennOgGodtgjoerelse?.norskArbgUtbetalerLoenn shouldBe true
        loennOgGodtgjoerelse?.erArbeidstakerAnsattHelePerioden shouldBe true
        loennOgGodtgjoerelse?.utlArbgUtbetalerLoenn shouldBe true
        loennOgGodtgjoerelse?.utlArbTilhoererSammeKonsern shouldBe false
        loennOgGodtgjoerelse?.bruttoLoennPerMnd shouldBe BigDecimal("2000.00")
        loennOgGodtgjoerelse?.bruttoLoennUtlandPerMnd shouldBe BigDecimal("1000.00")
        loennOgGodtgjoerelse?.mottarNaturalytelser shouldBe false
        loennOgGodtgjoerelse?.samletVerdiNaturalytelser shouldBe BigDecimal("10000.50")
        loennOgGodtgjoerelse?.erArbeidsgiveravgiftHelePerioden shouldBe true
        loennOgGodtgjoerelse?.erTrukketTrygdeavgift shouldBe true

        val foretakUtland = soeknad.foretakUtland[0]
        foretakUtland.navn shouldBe "Virskomheten i utlandet"
        foretakUtland.orgnr shouldBe "XYZ123456789"
        foretakUtland.adresse.gatenavn shouldBe "gatenavn med mer"
        foretakUtland.adresse.poststed shouldBe "testbyen"
        foretakUtland.adresse.postnummer shouldBe "UTLAND-1234"
        foretakUtland.adresse.landkode shouldBe "BE"

        val utenlandsoppdraget = soeknad.utenlandsoppdraget
        utenlandsoppdraget.erErstatningTidligereUtsendte shouldBe false
        utenlandsoppdraget.samletUtsendingsperiode.shouldNotBeNull()
        utenlandsoppdraget.samletUtsendingsperiode.fom.shouldBeNull()
        utenlandsoppdraget.erUtsendelseForOppdragIUtlandet shouldBe false
        utenlandsoppdraget.erFortsattAnsattEtterOppdraget.shouldBeNull()
        utenlandsoppdraget.erAnsattForOppdragIUtlandet shouldBe false
        utenlandsoppdraget.erDrattPaaEgetInitiativ shouldBe false
    }

    @Test
    fun `test mapping arbeidsgiver`() {
        val medlemskapArbeidEOSM = parseSøknadXML()
        medlemskapArbeidEOSM.innhold.arbeidsgiver.setOffentligVirksomhet(true)

        val soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM)

        val juridiskArbeidsgiverNorge = soeknad.juridiskArbeidsgiverNorge
        juridiskArbeidsgiverNorge.erOffentligVirksomhet shouldBe true
        juridiskArbeidsgiverNorge.antallAnsatte.shouldBeNull()
        juridiskArbeidsgiverNorge.antallAdmAnsatte.shouldBeNull()
        juridiskArbeidsgiverNorge.antallUtsendte.shouldBeNull()
        juridiskArbeidsgiverNorge.andelOmsetningINorge.shouldBeNull()
        juridiskArbeidsgiverNorge.andelOppdragINorge.shouldBeNull()
        juridiskArbeidsgiverNorge.andelKontrakterINorge.shouldBeNull()
        juridiskArbeidsgiverNorge.andelRekruttertINorge.shouldBeNull()
        juridiskArbeidsgiverNorge.ekstraArbeidsgivere.shouldBeEmpty()

        medlemskapArbeidEOSM.innhold.arbeidsgiver.setOffentligVirksomhet(false)

        val soeknad2 = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM)

        val juridiskArbeidsgiverNorge2 = soeknad2.juridiskArbeidsgiverNorge
        juridiskArbeidsgiverNorge2.erOffentligVirksomhet shouldBe false
        juridiskArbeidsgiverNorge2.antallAnsatte shouldBe 100
        juridiskArbeidsgiverNorge2.antallAdmAnsatte shouldBe 10
        juridiskArbeidsgiverNorge2.antallUtsendte shouldBe 10
        juridiskArbeidsgiverNorge2.andelOmsetningINorge shouldBe BigDecimal(90)
        juridiskArbeidsgiverNorge2.andelOppdragINorge shouldBe BigDecimal(90)
        juridiskArbeidsgiverNorge2.andelKontrakterINorge shouldBe BigDecimal(90)
        juridiskArbeidsgiverNorge2.andelRekruttertINorge shouldBe BigDecimal(90)
        juridiskArbeidsgiverNorge2.ekstraArbeidsgivere shouldContain "910825569"
    }

    @Test
    fun `test mapping offshore arbeidssteder`() {
        val medlemskapArbeidEOSM = parseSøknadXML()
        medlemskapArbeidEOSM.innhold.midlertidigUtsendt.arbeidssted.typeArbeidssted = ArbeidsstedType.OFFSHORE.toString()

        val soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM)

        soeknad.maritimtArbeid.shouldNotBeEmpty()
        val maritimtArbeid = soeknad.maritimtArbeid[0]
        maritimtArbeid.enhetNavn shouldBe "Landplattform"
        maritimtArbeid.innretningstype shouldBe Innretningstyper.PLATTFORM
        maritimtArbeid.innretningLandkode shouldBe "CH"
    }

    @Test
    fun `test mapping skipsfart`() {
        val medlemskapArbeidEOSM = parseSøknadXML()
        medlemskapArbeidEOSM.innhold.midlertidigUtsendt.arbeidssted.typeArbeidssted = ArbeidsstedType.SKIPSFART.toString()

        val soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM)

        soeknad.maritimtArbeid.shouldNotBeEmpty()
        val maritimtArbeidInnenriks = soeknad.maritimtArbeid[0]
        maritimtArbeidInnenriks.enhetNavn shouldBe "abcd"
        maritimtArbeidInnenriks.fartsomradeKode shouldBe INNENRIKS
        maritimtArbeidInnenriks.territorialfarvannLandkode shouldBe "BG"
        val maritimtArbeidUtenriks = soeknad.maritimtArbeid[1]
        maritimtArbeidUtenriks.fartsomradeKode shouldBe UTENRIKS
        maritimtArbeidUtenriks.flaggLandkode shouldBe "FO"
    }

    @Test
    fun `test mapping luftfart baser`() {
        val medlemskapArbeidEOSM = parseSøknadXML()
        medlemskapArbeidEOSM.innhold.midlertidigUtsendt.arbeidssted.typeArbeidssted = ArbeidsstedType.LUFTFART.toString()

        val soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM)

        soeknad.luftfartBaser.shouldNotBeEmpty()
        val luftfartBase = soeknad.luftfartBaser[0]
        luftfartBase.hjemmebaseNavn shouldBe "koti"
        luftfartBase.hjemmebaseLand shouldBe "FI"
        luftfartBase.typeFlyvninger shouldBe INTERNASJONAL
    }

    @Test
    fun `test mapping samlet utsendingsperiode`() {
        val medlemskapArbeidEOSM = parseSøknadXML()
        medlemskapArbeidEOSM.innhold.midlertidigUtsendt.utenlandsoppdraget.setErstatterTidligereUtsendte(true)

        val soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM)

        val utenlandsoppdraget = soeknad.utenlandsoppdraget
        utenlandsoppdraget.erErstatningTidligereUtsendte shouldBe true
        utenlandsoppdraget.samletUtsendingsperiode.shouldNotBeNull()
        utenlandsoppdraget.samletUtsendingsperiode.fom shouldBe LocalDate.of(2019, 8, 1)
        utenlandsoppdraget.samletUtsendingsperiode.tom shouldBe LocalDate.of(2019, 8, 6)
    }

    @Test
    fun `test arbeidssituasjon og oevrig`() {
        val medlemskapArbeidEOSM = parseSøknadXML()

        val soeknad = SoeknadMapper.lagSoeknad(medlemskapArbeidEOSM)

        val arbeidssituasjonOgOevrig = soeknad.arbeidssituasjonOgOevrig
        arbeidssituasjonOgOevrig.harLoennetArbeidMinstEnMndFoerUtsending shouldBe true
        arbeidssituasjonOgOevrig.beskrivelseArbeidSisteMnd shouldBe "Arbeid siste mnd"
        arbeidssituasjonOgOevrig.harAndreArbeidsgivereIUtsendingsperioden shouldBe false
        arbeidssituasjonOgOevrig.beskrivelseAnnetArbeid shouldBe "Annet arbeid"
        arbeidssituasjonOgOevrig.erSkattepliktig shouldBe true
        arbeidssituasjonOgOevrig.mottarYtelserNorge shouldBe false
        arbeidssituasjonOgOevrig.mottarYtelserUtlandet shouldBe false
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
