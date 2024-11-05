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
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
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
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Maritimtyper
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class A1MapperTest {

    @MockK
    private lateinit var behandlingsresultat: Behandlingsresultat

    @MockK
    private lateinit var behandling: Behandling

    lateinit var melosysNAVFelles: MelosysNAVFelles
    lateinit var brevData: BrevDataA1
    lateinit var fellesType: FellesType
    private var mapper = A1Mapper()
    private var easyRandom = EasyRandomConfigurer.randomForDokProd()


    @BeforeEach
    fun setup() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            lovvalgsland = Land_iso2.NO
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2
            tilleggsbestemmelse = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5
            fom = LocalDate.now()
            tom = LocalDate.now()
        }

        every { behandlingsresultat.registrertDato } returns Instant.now()
        every { behandlingsresultat.lovvalgsperioder } returns setOf(lovvalgsperiode)
        every { behandlingsresultat.hentLovvalgsperiode() } returns lovvalgsperiode

        every { behandling.registrertDato } returns Instant.now()
        every { behandling.fagsak } returns FagsakTestFactory.lagFagsak()
        every { behandling.id } returns 1L

        val boAdresse = StrukturertAdresse().apply {
            husnummerEtasjeLeilighet = "12B"
            gatenavn = "Bogata"
            postnummer = "0165"
            poststed = "Poststed"
            region = "Region"
            landkode = Landkoder.NO.kode
        }
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
        brevData = BrevDataA1().apply {
            yrkesgruppe = Yrkesgrupper.ORDINAER
            bostedsadresse = boAdresse
            arbeidssteder = listOf(fysiskArbeidssted, maritimtArbeidsstedSkip, maritimtArbeidsstedSokkel)
            arbeidsland = listOf(Land_iso2.SE)
            person = PersonopplysningerObjectFactory.lagPersonopplysninger()
            hovedvirksomhet = virksomhet
            bivirksomheter = listOf(utenlandskVirksomhet)
        }
        fellesType = FellesType().apply {
            fagsaksnummer = "MELTEST-4"
        }
        melosysNAVFelles = easyRandom.nextObject(MelosysNAVFelles::class.java).apply {
            mottaker.mottakeradresse = BrevDataUtils.lagNorskPostadresse()
            kontaktinformasjon = BrevDataUtils.lagKontaktInformasjon()
        }
    }

    @Test
    fun mapTilBrevXML() {
        mapTilBrevXML(brevData).shouldNotBeNull()
    }

    @Test
    fun mapTilBrevXML_hovedVirksomhetUtenOrgnr_fyll4_2MedMellomrom() {
        val utenlandskForetak = BrevDataTestUtils.lagForetakUtland(false)
        utenlandskForetak.orgnr = null
        brevData.hovedvirksomhet = AvklartVirksomhet(utenlandskForetak)
        brevData.arbeidsland = listOf(*Land_iso2.values()) // List.of(Landkoder.GB, Landkoder.SE);
        mapper.mapA1(behandling, behandlingsresultat, brevData)
        mapTilBrevXML(brevData).shouldNotBeNull()
    }

    @Test
    fun mapTilBrevXML_bostedsAdresseIkkeGyldig_settBostedsadresseSinGateAdresseTom() {
        brevData.bostedsadresse?.gatenavn = null
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.person.bostedsadresse.gatenavn.shouldBe(" ")
        mapTilBrevXML(brevData).shouldNotBeNull()
    }

    @Test
    fun mapBrevTilXML_arbeidslandUtenFysiskArbeidssted_fyllerPåMedArbeidsland() {
        brevData.arbeidsland = listOf(Land_iso2.SE, Land_iso2.DK, Land_iso2.GB)
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.fysiskArbeidsstedAdresseListe.adresse.forExactly(1) {
            it.adresselinje1.shouldContainInOrder("Danmark", "Sverige")
        }
        mapTilBrevXML(brevData).shouldNotBeNull()
    }

    @Test
    fun mapBrevTilXML_harEftaTekst_fyllerTekstOmEftaStorbritannia() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            lovvalgsland = Land_iso2.GB
            bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
            fom = LocalDate.now()
            tom = LocalDate.now()
        }

        every { behandlingsresultat.hentLovvalgsperiode() } returns lovvalgsperiode

        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.bivirksomhetListe.bivirksomhet.forExactly(1) {
            it.navn.shouldBe("Issued under the EEA EFTA Convention")
        }
    }

    @Test
    fun mapBrevTilXML_harIkkeEftaTekst_11_3A() {
        val lovvalgsperiode = Lovvalgsperiode().apply {
            lovvalgsland = Land_iso2.GB
            bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3A
            fom = LocalDate.now()
            tom = LocalDate.now()
        }

        every { behandlingsresultat.hentLovvalgsperiode() } returns lovvalgsperiode

        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.bivirksomhetListe.bivirksomhet.forExactly(1) {
            it.navn.shouldBe("JARLSBERG INTERNATIONAL")
        }
    }


    @Test
    fun mapBrevTilXML_harFlyvendeArbeidssted_fyllerUtHjemmebaseNavnOgLand() {
        val landkode = Landkoder.FI
        val luftfartBase = LuftfartBase().apply {
            hjemmebaseNavn = "hjemmebaseNav"
            hjemmebaseLand = landkode.kode
        }
        brevData.arbeidssteder = listOf<Arbeidssted>(FlyvendeArbeidssted(luftfartBase))
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
        val adresse = BrevDataTestUtils.lagStrukturertAdresse()
        val fysiskArbeidssted: Arbeidssted = FysiskArbeidssted("", "", adresse)
        brevData.arbeidssteder = listOf(fysiskArbeidssted)
        brevData.arbeidsland = emptyList()
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)


        fysiskArbeidssted.lagAdresselinje().length.shouldBeLessThan(A1Mapper.MAKS_ANTALL_TEGN_PER_LINJE_5_2)
        a1.fysiskArbeidsstedAdresseListe.adresse.stream()
            .map { obj: AdresseType -> obj.adresselinje1 }
            .filter { cs: String? -> StringUtils.isNotEmpty(cs) }.toList()
            .shouldHaveSize(1)
    }

    @Test
    fun mapTilBrevXML_harLangAdressePåArbeidssted_brekkerAdresseOverFlereLinjer() {
        val adresse = BrevDataTestUtils.lagStrukturertAdresse()
        adresse.gatenavn =
            "Lorem ipsumdolorsitamet consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua-veien"
        adresse.husnummerEtasjeLeilighet = "47"
        val fysiskArbeidssted = FysiskArbeidssted("", "", adresse)
        fysiskArbeidssted.lagAdresselinje().length.shouldBeGreaterThan(A1Mapper.MAKS_ANTALL_TEGN_PER_LINJE_5_2)
        brevData.arbeidssteder = listOf(fysiskArbeidssted)
        brevData.arbeidsland = emptyList()
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.fysiskArbeidsstedAdresseListe.adresse
            .map { obj: AdresseType -> obj.adresselinje1 }
            .filter { cs: String? -> StringUtils.isNotEmpty(cs) }
            .shouldHaveAtLeastSize(2)
    }

    @Test
    fun mapTilBrevXML_harUkjentEllerIkkeOppgittArbeidsted_brekkerAdresseOverFlereLinjer() {
        brevData.ukjenteEllerAlleEosLand = true
        brevData.arbeidssteder = emptyList()
        brevData.arbeidsland = emptyList()
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
            val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)

            a1.person.statsborgerskap shouldBe "DK,NO,SE"
        }

        @Test
        fun `bruker med Kosovo som statsborgerskap skal vise tekst UNKNOWN`() {
            setBrevDataPersonStatsborgerskap(
                listOf(
                    "XXK"
                )
            )
            val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)

            a1.person.statsborgerskap shouldBe A1Mapper.UNKNOWN_TEKST
        }

        @Test
        fun `bruker med ukjent som statsborgerskap skal vise tekst UNKNOWN`() {
            setBrevDataPersonStatsborgerskap(
                listOf(
                    "XXK"
                )
            )
            val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)

            a1.person.statsborgerskap shouldBe A1Mapper.UNKNOWN_TEKST
        }

        @Test
        fun `bruker med Kosovo og Norge som statsborgerskap skal fjerne Kosovo`() {
            setBrevDataPersonStatsborgerskap(
                listOf(
                    "XXK",
                    "NOR"
                )
            )
            val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)

            a1.person.statsborgerskap shouldBe "NO"
        }

        @Test
        fun `bruker med ukjent og Norge som statsborgerskap skal fjerne ukjent`() {
            setBrevDataPersonStatsborgerskap(
                listOf(
                    "XUK",
                    "NOR"
                )
            )
            val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)

            a1.person.statsborgerskap shouldBe "NO"
        }

        @Test
        fun `om bruker er statsløs bruk statsløs tekst`() {
            brevData.person = PersonopplysningerObjectFactory.lagPersonopplysningerStatløs()

            val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)

            a1.person.statsborgerskap shouldBe A1Mapper.STATSLØS_TEKST
        }

        private fun setBrevDataPersonStatsborgerskap(iso3Landkode: List<String>) {
            (brevData.person as Personopplysninger).statsborgerskap = iso3Landkode.map {
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
        brevData.person = PersonopplysningerObjectFactory.lagPersonopplysninger()
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.person.midlertidigOppholdsadresse.gatenavn.shouldBe("gatenavnOppholdsadresseFreg")
    }

    @Test
    fun mapTilBrevXML_harIngenOppholdsadresse_forventUtfylltMidlertidigAdresseMedKontaktAdresse() {
        brevData.person = PersonopplysningerObjectFactory.lagPersonopplysningerUtenOppholdsadresse()
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.person.midlertidigOppholdsadresse.gatenavn.shouldBe("gatenavnKontaktadresseFreg")
    }

    @Test
    fun mapTilBrevXML_harIngenOppholdsadresse_forventUtfylltMidlertidigAdresseMedKontaktAdresseSemistrukturert() {
        brevData.person = PersonopplysningerObjectFactory.lagPersonopplysningerKontaktadresseSemistrukturert(true)
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.person.midlertidigOppholdsadresse.gatenavn.shouldBe("Kranstien 3 0338 Oslo")
    }

    @Test
    fun mapTilBrevXML_harIngenKontaktadresse_forventUtfylltMidlertidigAdresseMedOppholdsadresse() {
        brevData.person = PersonopplysningerObjectFactory.lagPersonopplysningerUtenKontaktadresse()
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.person.midlertidigOppholdsadresse.gatenavn.shouldBe("gatenavnOppholdsadresseFreg")
    }

    @Test
    fun mapTilBrevXML_harIngenAdresserRegistrert() {
        brevData.bostedsadresse = null
        brevData.person = PersonopplysningerObjectFactory.lagPersonopplysningerUtenAdresser()
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.person.run {
            bostedsadresse.shouldBeNull()
            midlertidigOppholdsadresse.shouldBeNull()
        }
    }

    @Test
    fun mapTilBrevXML_brevdataManglerBostedsadresse_bostedsadresseErTom() {
        brevData.bostedsadresse = null
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.person.bostedsadresse.shouldBeNull()
    }

    @Test
    fun `mapperA1vedlegg ved utendlandsadresse er ikke postnr obligatorisk fra melosys`() {
        brevData.bostedsadresse?.postnummer = null
        brevData.bostedsadresse?.landkode = "SE"
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.person.bostedsadresse.postnr.shouldBe(" ")
    }

    @Test
    fun `mapperA1vedlegg ved norsk adresse er postnr obligatorisk fra melosys`() {
        brevData.bostedsadresse?.postnummer = null
        brevData.bostedsadresse?.landkode = "NO"
        val a1 = mapper.mapA1(behandling, behandlingsresultat, brevData)
        a1.person.bostedsadresse.postnr.shouldBe(null)
    }

    fun mapTilBrevXML(brevData: BrevData): String {
        val xsdLocation = "melosysbrev/melosys_000116.xsd"
        val fag = mapFag()
        val vedlegg = VedleggType().apply {
            a1 = mapper.mapA1(behandling, behandlingsresultat, brevData as BrevDataA1)
        }
        val brevdataTypeJAXBElement = mapTilBrevdataType(fag, vedlegg)
        return JaxbHelper.marshalAndValidate(brevdataTypeJAXBElement, xsdLocation)
    }

    private fun mapFag(): Fag {
        return Fag().apply {
            vedleggA1 = "true"
        }
    }

    private fun mapTilBrevdataType(fag: Fag, vedlegg: VedleggType): JAXBElement<BrevdataType> {
        val factory = ObjectFactory()
        val brevdataType = factory.createBrevdataType().apply {
            this.felles = fellesType
            this.navFelles = melosysNAVFelles
            this.fag = fag
            this.vedlegg = vedlegg
        }
        return factory.createBrevdata(brevdataType)
    }

}
