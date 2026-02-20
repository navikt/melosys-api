package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.inspectors.forExactly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContainInOrder
import jakarta.xml.bind.JAXBElement
import no.nav.dok.melosysbrev._000067.AdresseType
import no.nav.dok.melosysbrev._000116.BrevdataType
import no.nav.dok.melosysbrev._000116.Fag
import no.nav.dok.melosysbrev._000116.ObjectFactory
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles
import no.nav.dok.melosysbrev.felles.melosys_vedlegg.VedleggType
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Maritimtyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper
import no.nav.melosys.domain.lovvalgsperiode
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.LuftfartBase
import no.nav.melosys.domain.person.Personopplysninger
import no.nav.melosys.domain.person.Statsborgerskap
import no.nav.melosys.service.dokument.brev.BrevData
import no.nav.melosys.service.dokument.brev.BrevDataA1
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils
import no.nav.melosys.service.dokument.brev.BrevDataUtils
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FlyvendeArbeidssted
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.MaritimtArbeidssted
import no.nav.melosys.service.persondata.PersonopplysningerObjectFactory
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class A1MapperTest {

    private val mapper = A1Mapper()
    private val easyRandom = EasyRandomConfigurer.randomForDokProd()

    private fun lagDefaultBehandlingsresultat() = Behandlingsresultat.forTest {
        lovvalgsperiode {
            lovvalgsland = Land_iso2.NO
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2
            tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5
            fom = LocalDate.now()
            tom = LocalDate.now()
        }
    }

    private fun lagDefaultBehandling() = Behandling.forTest {
        id = 1L
    }

    private fun lagDefaultBostedsadresse() = StrukturertAdresse(
        gatenavn = "Bogata",
        husnummerEtasjeLeilighet = "12B",
        postnummer = "0165",
        poststed = "Poststed",
        region = "Region",
        landkode = Landkoder.NO.kode
    )

    private fun lagDefaultBrevData(
        init: BrevDataA1.() -> Unit = {}
    ): BrevDataA1 {
        val strukturertAdresse = BrevDataTestUtils.lagStrukturertAdresse()
        val virksomhet = AvklartVirksomhet(
            "Jarlsberg",
            "123456789",
            strukturertAdresse,
            Yrkesaktivitetstyper.LOENNET_ARBEID
        )
        val utenlandskVirksomhet = AvklartVirksomhet(
            "JARLSBERG INTERNATIONAL",
            "123456789",
            strukturertAdresse,
            Yrkesaktivitetstyper.LOENNET_ARBEID
        )
        val fysiskArbeidssted = FysiskArbeidssted("JARLSBERG INTERNATIONAL", "123456789", strukturertAdresse)
        val maritimtArbeidsstedSkip = BrevDataTestUtils.lagMaritimtArbeidssted(Maritimtyper.SKIP)
        val maritimtArbeidsstedSokkel = BrevDataTestUtils.lagMaritimtArbeidssted(Maritimtyper.SOKKEL) as MaritimtArbeidssted
        return BrevDataA1(
            yrkesgruppe = Yrkesgrupper.ORDINAER,
            bostedsadresse = lagDefaultBostedsadresse(),
            arbeidssteder = listOf(fysiskArbeidssted, maritimtArbeidsstedSkip, maritimtArbeidsstedSokkel),
            arbeidsland = listOf(Land_iso2.SE),
            person = PersonopplysningerObjectFactory.lagPersonopplysninger(),
            hovedvirksomhet = virksomhet,
            bivirksomheter = listOf(utenlandskVirksomhet)
        ).apply(init)
    }

    private fun lagDefaultFellesType() = FellesType().apply {
        fagsaksnummer = "MELTEST-4"
    }

    private fun lagDefaultMelosysNAVFelles(): MelosysNAVFelles =
        easyRandom.nextObject(MelosysNAVFelles::class.java).apply {
            mottaker.mottakeradresse = BrevDataUtils.lagNorskPostadresse()
            kontaktinformasjon = BrevDataUtils.lagKontaktInformasjon()
        }

    @Test
    fun mapTilBrevXML() {
        val behandling = lagDefaultBehandling()
        val behandlingsresultat = lagDefaultBehandlingsresultat()
        val brevData = lagDefaultBrevData()
        mapTilBrevXML(brevData, behandling, behandlingsresultat).shouldNotBeNull()
    }

    @Test
    fun mapTilBrevXML_hovedVirksomhetUtenOrgnr_fyll4_2MedMellomrom() {
        val behandling = lagDefaultBehandling()
        val behandlingsresultat = lagDefaultBehandlingsresultat()
        val utenlandskForetak = BrevDataTestUtils.lagForetakUtland(false).apply { orgnr = null }
        val brevData = lagDefaultBrevData {
            hovedvirksomhet = AvklartVirksomhet(utenlandskForetak)
            arbeidsland = Land_iso2.entries
        }
        mapper.mapA1(behandling, behandlingsresultat, brevData)
        mapTilBrevXML(brevData, behandling, behandlingsresultat).shouldNotBeNull()
    }

    @Test
    fun mapTilBrevXML_bostedsAdresseIkkeGyldig_settBostedsadresseSinGateAdresseTom() {
        val behandling = lagDefaultBehandling()
        val behandlingsresultat = lagDefaultBehandlingsresultat()
        val brevData = lagDefaultBrevData {
            bostedsadresse = lagDefaultBostedsadresse().apply { gatenavn = null }
        }
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.person.bostedsadresse.gatenavn.shouldBe(" ")
        mapTilBrevXML(brevData, behandling, behandlingsresultat).shouldNotBeNull()
    }

    @Test
    fun mapBrevTilXML_arbeidslandUtenFysiskArbeidssted_fyllerPåMedArbeidsland() {
        val behandling = lagDefaultBehandling()
        val behandlingsresultat = lagDefaultBehandlingsresultat()
        val brevData = lagDefaultBrevData {
            arbeidsland = listOf(Land_iso2.SE, Land_iso2.DK, Land_iso2.GB)
        }
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.fysiskArbeidsstedAdresseListe.adresse.forExactly(1) {
            it.adresselinje1.shouldContainInOrder("Danmark", "Sverige")
        }
        mapTilBrevXML(brevData, behandling, behandlingsresultat).shouldNotBeNull()
    }

    @Test
    fun mapBrevTilXML_harEftaTekst_fyllerTekstOmEftaStorbritannia() {
        val behandling = lagDefaultBehandling()
        val behandlingsresultat = Behandlingsresultat.forTest {
            lovvalgsperiode {
                lovvalgsland = Land_iso2.GB
                bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
                fom = LocalDate.now()
                tom = LocalDate.now()
            }
        }
        val brevData = lagDefaultBrevData()

        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.bivirksomhetListe.bivirksomhet.forExactly(1) {
            it.navn.shouldBe("Issued under the EEA EFTA Convention")
        }
    }

    @Test
    fun mapBrevTilXML_harIkkeEftaTekst_11_3A() {
        val behandling = lagDefaultBehandling()
        val behandlingsresultat = Behandlingsresultat.forTest {
            lovvalgsperiode {
                lovvalgsland = Land_iso2.GB
                bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
                fom = LocalDate.now()
                tom = LocalDate.now()
            }
        }
        val brevData = lagDefaultBrevData()

        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.bivirksomhetListe.bivirksomhet.forExactly(1) {
            it.navn.shouldBe("JARLSBERG INTERNATIONAL")
        }
    }


    @Test
    fun mapBrevTilXML_harFlyvendeArbeidssted_fyllerUtHjemmebaseNavnOgLand() {
        val behandling = lagDefaultBehandling()
        val behandlingsresultat = lagDefaultBehandlingsresultat()
        val landkode = Landkoder.FI
        val luftfartBase = LuftfartBase(
            hjemmebaseNavn = "hjemmebaseNav",
            hjemmebaseLand = landkode.kode
        )
        val brevData = lagDefaultBrevData {
            arbeidssteder = listOf(FlyvendeArbeidssted(luftfartBase))
        }
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.bivirksomhetListe.bivirksomhet.forExactly(1) {
            it.navn.shouldBe(luftfartBase.hjemmebaseNavn)
        }
        a1.fysiskArbeidsstedAdresseListe.adresse.forExactly(1) {
            it.adresselinje1.shouldBe(landkode.beskrivelse)
        }
    }

    @Test
    fun mapTilBrevXML_harKortAdressePåArbeidssted_brekkerIkkeAdresseOverFlereLinjer() {
        val behandling = lagDefaultBehandling()
        val behandlingsresultat = lagDefaultBehandlingsresultat()
        val adresse = BrevDataTestUtils.lagStrukturertAdresse()
        val fysiskArbeidssted: Arbeidssted = FysiskArbeidssted("", "", adresse)
        val brevData = lagDefaultBrevData {
            arbeidssteder = listOf(fysiskArbeidssted)
            arbeidsland = emptyList()
        }
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)

        fysiskArbeidssted.lagAdresselinje().length.shouldBeLessThan(A1Mapper.MAKS_ANTALL_TEGN_PER_LINJE_5_2)
        a1.fysiskArbeidsstedAdresseListe.adresse.stream()
            .map { obj: AdresseType -> obj.adresselinje1 }
            .filter { cs: String? -> StringUtils.isNotEmpty(cs) }.toList()
            .shouldHaveSize(1)
    }

    @Test
    fun mapTilBrevXML_harLangAdressePåArbeidssted_brekkerAdresseOverFlereLinjer() {
        val behandling = lagDefaultBehandling()
        val behandlingsresultat = lagDefaultBehandlingsresultat()
        val adresse = StrukturertAdresse(
            gatenavn = "Lorem ipsumdolorsitamet consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua-veien",
            husnummerEtasjeLeilighet = "47",
            postnummer = "4321",
            poststed = "Poststed",
            region = null,
            landkode = Landkoder.BG.kode
        )
        val fysiskArbeidssted = FysiskArbeidssted("", "", adresse)
        fysiskArbeidssted.lagAdresselinje().length.shouldBeGreaterThan(A1Mapper.MAKS_ANTALL_TEGN_PER_LINJE_5_2)
        val brevData = lagDefaultBrevData {
            arbeidssteder = listOf(fysiskArbeidssted)
            arbeidsland = emptyList()
        }
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.fysiskArbeidsstedAdresseListe.adresse
            .map { obj: AdresseType -> obj.adresselinje1 }
            .filter { cs: String? -> StringUtils.isNotEmpty(cs) }
            .shouldHaveAtLeastSize(2)
    }

    @Test
    fun mapTilBrevXML_harUkjentEllerIkkeOppgittArbeidsted_brekkerAdresseOverFlereLinjer() {
        val behandling = lagDefaultBehandling()
        val behandlingsresultat = lagDefaultBehandlingsresultat()
        val brevData = lagDefaultBrevData {
            ukjenteEllerAlleEosLand = true
            arbeidssteder = emptyList()
            arbeidsland = emptyList()
        }
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.fysiskArbeidsstedAdresseListe.adresse
            .map { obj: AdresseType -> obj.adresselinje1 }
            .filter { cs: String? -> StringUtils.isNotEmpty(cs) }
            .shouldContainExactly(A1Mapper.FLERE_UKJENTE_ELLER_IKKE_OPPGITT_LAND)
    }

    @Nested
    inner class Statsborgerskap {
        @Test
        fun `bruker har flere statsborgerskap - forvent Norsk Svensk og Dansk statsborgerskap i alfabetisk rekkefølge`() {
            val behandling = lagDefaultBehandling()
            val behandlingsresultat = lagDefaultBehandlingsresultat()
            val brevData = lagDefaultBrevData()
            val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)

            a1.person.statsborgerskap shouldBe "DK,NO,SE"
        }

        @Test
        fun `CDM 4_3 - bruker med Kosovo som statsborgerskap skal vise tekst UNKNOWN`() {
            val behandling = lagDefaultBehandling()
            val behandlingsresultat = lagDefaultBehandlingsresultat()
            val brevData = lagDefaultBrevData {
                person = lagPersonopplysningerMedStatsborgerskap(listOf(Land.KOSOVO))
            }
            val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)

            a1.person.statsborgerskap shouldBe A1Mapper.UNKNOWN_TEKST
        }

        @Test
        fun `bruker med ukjent som statsborgerskap skal vise tekst UNKNOWN`() {
            val behandling = lagDefaultBehandling()
            val behandlingsresultat = lagDefaultBehandlingsresultat()
            val brevData = lagDefaultBrevData {
                person = lagPersonopplysningerMedStatsborgerskap(listOf(Land.UNKNOWN))
            }
            val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)

            a1.person.statsborgerskap shouldBe A1Mapper.UNKNOWN_TEKST
        }

        @Test
        fun `CDM 4_3 - bruker med Kosovo og Norge som statsborgerskap skal fjerne Kosovo`() {
            val behandling = lagDefaultBehandling()
            val behandlingsresultat = lagDefaultBehandlingsresultat()
            val brevData = lagDefaultBrevData {
                person = lagPersonopplysningerMedStatsborgerskap(listOf(Land.KOSOVO, Land.NORGE))
            }
            val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)

            a1.person.statsborgerskap shouldBe "NO"
        }

        @Test
        fun `CDM 4_4 - bruker med kun Kosovo som statsborgerskap skal vise Kosovo-landkode`() {
            val behandling = lagDefaultBehandling()
            val behandlingsresultat = lagDefaultBehandlingsresultat()
            val brevData = lagDefaultBrevData {
                erCdm44 = true
                person = lagPersonopplysningerMedStatsborgerskap(listOf(Land.KOSOVO))
            }
            val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)

            a1.person.statsborgerskap shouldBe "XK"
        }

        @Test
        fun `CDM 4_4 - bruker med Kosovo og Norge som statsborgerskap skal vise begge`() {
            val behandling = lagDefaultBehandling()
            val behandlingsresultat = lagDefaultBehandlingsresultat()
            val brevData = lagDefaultBrevData {
                erCdm44 = true
                person = lagPersonopplysningerMedStatsborgerskap(listOf(Land.KOSOVO, Land.NORGE))
            }
            val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)

            a1.person.statsborgerskap shouldBe "NO,XK"
        }

        @Test
        fun `bruker med ukjent og Norge som statsborgerskap skal fjerne ukjent`() {
            val behandling = lagDefaultBehandling()
            val behandlingsresultat = lagDefaultBehandlingsresultat()
            val brevData = lagDefaultBrevData {
                person = lagPersonopplysningerMedStatsborgerskap(listOf(Land.UNKNOWN, Land.NORGE))
            }
            val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)

            a1.person.statsborgerskap shouldBe "NO"
        }

        @Test
        fun `om bruker er statsløs bruk statsløs tekst`() {
            val behandling = lagDefaultBehandling()
            val behandlingsresultat = lagDefaultBehandlingsresultat()
            val brevData = lagDefaultBrevData {
                person = PersonopplysningerObjectFactory.lagPersonopplysningerStatløs()
            }

            val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)

            a1.person.statsborgerskap shouldBe A1Mapper.STATSLØS_TEKST
        }

        private fun lagPersonopplysningerMedStatsborgerskap(iso3Landkoder: List<String>): Personopplysninger =
            PersonopplysningerObjectFactory.lagPersonopplysninger().apply {
                statsborgerskap = iso3Landkoder.map {
                    Statsborgerskap(
                        it,
                        null,
                        LocalDate.EPOCH,
                        LocalDate.now(),
                        "PDL",
                        "Dolly",
                        false
                    )
                }
            }
    }



    @Test
    fun mapTilBrevXML_bostedsadresserFraRegister_forventBostedsadresse() {
        val behandling = lagDefaultBehandling()
        val behandlingsresultat = lagDefaultBehandlingsresultat()
        val brevData = lagDefaultBrevData()
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.person.bostedsadresse.run {
            gatenavn.shouldBe("Bogata")
            husnummer.shouldBe("12B")
            postnr.shouldBe("0165")
            poststed.shouldBe("Poststed")
            region.shouldBe("Region")
            landkode.shouldBe("NO")
        }
    }

    @Test
    fun mapTilBrevXML_harFlereAdresserRegistrert_forventUtfylltMidlertidigAdresseMedNyesteRegistrerteDato() {
        val behandling = lagDefaultBehandling()
        val behandlingsresultat = lagDefaultBehandlingsresultat()
        val brevData = lagDefaultBrevData {
            person = PersonopplysningerObjectFactory.lagPersonopplysninger()
        }
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.person.midlertidigOppholdsadresse.gatenavn.shouldBe("gatenavnOppholdsadresseFreg")
    }

    @Test
    fun mapTilBrevXML_harIngenOppholdsadresse_forventUtfylltMidlertidigAdresseMedKontaktAdresse() {
        val behandling = lagDefaultBehandling()
        val behandlingsresultat = lagDefaultBehandlingsresultat()
        val brevData = lagDefaultBrevData {
            person = PersonopplysningerObjectFactory.lagPersonopplysningerUtenOppholdsadresse()
        }
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.person.midlertidigOppholdsadresse.gatenavn.shouldBe("gatenavnKontaktadresseFreg")
    }

    @Test
    fun mapTilBrevXML_harIngenOppholdsadresse_forventUtfylltMidlertidigAdresseMedKontaktAdresseSemistrukturert() {
        val behandling = lagDefaultBehandling()
        val behandlingsresultat = lagDefaultBehandlingsresultat()
        val brevData = lagDefaultBrevData {
            person = PersonopplysningerObjectFactory.lagPersonopplysningerKontaktadresseSemistrukturert(true)
        }
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.person.midlertidigOppholdsadresse.gatenavn.shouldBe("Kranstien 3 0338 Oslo")
    }

    @Test
    fun mapTilBrevXML_harIngenKontaktadresse_forventUtfylltMidlertidigAdresseMedOppholdsadresse() {
        val behandling = lagDefaultBehandling()
        val behandlingsresultat = lagDefaultBehandlingsresultat()
        val brevData = lagDefaultBrevData {
            person = PersonopplysningerObjectFactory.lagPersonopplysningerUtenKontaktadresse()
        }
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.person.midlertidigOppholdsadresse.gatenavn.shouldBe("gatenavnOppholdsadresseFreg")
    }

    @Test
    fun mapTilBrevXML_harIngenAdresserRegistrert() {
        val behandling = lagDefaultBehandling()
        val behandlingsresultat = lagDefaultBehandlingsresultat()
        val brevData = lagDefaultBrevData {
            bostedsadresse = null
            person = PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser()
        }
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.person.run {
            bostedsadresse.shouldBeNull()
            midlertidigOppholdsadresse.shouldBeNull()
        }
    }

    @Test
    fun mapTilBrevXML_brevdataManglerBostedsadresse_bostedsadresseErTom() {
        val behandling = lagDefaultBehandling()
        val behandlingsresultat = lagDefaultBehandlingsresultat()
        val brevData = lagDefaultBrevData {
            bostedsadresse = null
        }
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.person.bostedsadresse.shouldBeNull()
    }

    @Test
    fun `mapperA1vedlegg ved utendlandsadresse er ikke postnr obligatorisk fra melosys`() {
        val behandling = lagDefaultBehandling()
        val behandlingsresultat = lagDefaultBehandlingsresultat()
        val brevData = lagDefaultBrevData {
            bostedsadresse = lagDefaultBostedsadresse().apply {
                postnummer = null
                landkode = "SE"
            }
        }
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.person.bostedsadresse.postnr.shouldBe(" ")
    }

    @Test
    fun `mapperA1vedlegg ved norsk adresse er postnr obligatorisk fra melosys`() {
        val behandling = lagDefaultBehandling()
        val behandlingsresultat = lagDefaultBehandlingsresultat()
        val brevData = lagDefaultBrevData {
            bostedsadresse = lagDefaultBostedsadresse().apply {
                postnummer = null
                landkode = "NO"
            }
        }
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.person.bostedsadresse.postnr.shouldBe(null)
    }

    private fun mapTilBrevXML(brevData: BrevData, behandling: Behandling, behandlingsresultat: Behandlingsresultat): String {
        val xsdLocation = "melosysbrev/melosys_000116.xsd"
        val fag = Fag().apply { vedleggA1 = "true" }
        val vedlegg = VedleggType().apply {
            a1 = mapper.mapA1(behandling, behandlingsresultat, brevData as BrevDataA1)
        }
        val brevdataTypeJAXBElement = mapTilBrevdataType(fag, vedlegg)
        return JaxbHelper.marshalAndValidate(brevdataTypeJAXBElement, xsdLocation)
    }

    private fun mapTilBrevdataType(fag: Fag, vedlegg: VedleggType): JAXBElement<BrevdataType> {
        val factory = ObjectFactory()
        val brevdataType = factory.createBrevdataType().apply {
            this.felles = lagDefaultFellesType()
            this.navFelles = lagDefaultMelosysNAVFelles()
            this.fag = fag
            this.vedlegg = vedlegg
        }
        return factory.createBrevdata(brevdataType)
    }

}
