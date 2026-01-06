package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import jakarta.xml.bind.JAXBElement
import no.nav.dok.melosysbrev._000115.BostedsadresseTypeKode
import no.nav.dok.melosysbrev._000116.BrevdataType
import no.nav.dok.melosysbrev._000116.Fag
import no.nav.dok.melosysbrev._000116.ObjectFactory
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles
import no.nav.dok.melosysbrev.felles.melosys_vedlegg.VedleggType
import no.nav.melosys.domain.Anmodningsperiode
import no.nav.melosys.domain.UtenlandskMyndighet
import no.nav.melosys.domain.VilkaarBegrunnelse
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.dokument.personDokumentForTest
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser.UTSENDELSE_MELLOM_24_MN_OG_5_AAR
import no.nav.melosys.domain.kodeverk.begrunnelser.Direkte_til_anmodning_begrunnelser.SJOEMANNSKIRKEN
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.service.dokument.brev.BrevDataA001
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils
import no.nav.melosys.service.dokument.brev.BrevDataUtils
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted
import org.jeasy.random.EasyRandom
import org.junit.jupiter.api.Test
import java.time.LocalDate

class A001MapperTest {

    private val mapper = A001Mapper()
    private val easyRandom: EasyRandom = EasyRandomConfigurer.randomForDokProd()

    @Test
    fun `mapTilBrevXML skal fungere med Art16 begrunnelser`() {
        val brevData = lagBrevDataA001 {
            anmodningUtenArt12Begrunnelser = emptySet()
        }
        val (fellesType, navFelles) = lagFellesTypeMedNavFelles()

        val xml = mapTilBrevXML(fellesType, navFelles, brevData)

        xml.shouldNotBeNull()
    }

    @Test
    fun `mapTilBrevXML skal fungere med Art16 uten Art12 begrunnelser`() {
        val brevData = lagBrevDataA001 {
            anmodningBegrunnelser = emptySet()
        }
        val (fellesType, navFelles) = lagFellesTypeMedNavFelles()

        val xml = mapTilBrevXML(fellesType, navFelles, brevData)

        xml.shouldNotBeNull()
    }

    @Test
    fun `mapTilBrevXML skal fungere uten selvstendig virksomhet`() {
        val brevData = lagBrevDataA001()
        val (fellesType, navFelles) = lagFellesTypeMedNavFelles()

        val xml = mapTilBrevXML(fellesType, navFelles, brevData)

        xml.shouldNotBeNull()
    }

    @Test
    fun `mapSEDA001 skal sette korrekt adressetype for kontaktadresse`() {
        val brevData = lagBrevDataA001 {
            bostedsadresseTypeKode = BostedsadresseTypeKode.KONTAKTADRESSE
        }

        val seda001 = mapper.mapSEDA001(brevData)

        seda001.person.bostedsadresse.adresseType shouldBe BostedsadresseTypeKode.KONTAKTADRESSE
    }

    @Test
    fun `mapSEDA001 skal sette korrekt adressetype naar ingen adressetype er gitt`() {
        val brevData = lagBrevDataA001 {
            bostedsadresseTypeKode = null
        }

        val seda001 = mapper.mapSEDA001(brevData)

        seda001.person.bostedsadresse.adresseType shouldBe BostedsadresseTypeKode.BOSTEDSLAND
    }

    @Test
    fun `mapSEDA001 skal mappe Storbritannia korrekt`() {
        val anmodningsperiode = lagAnmodningsperiode(
            bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1,
            unntakFraBestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3D
        )
        val brevData = lagBrevDataA001 {
            anmodningsperioder = mutableSetOf(anmodningsperiode)
            ytterligereInformasjon = "Fritekst fra saksbehandler."
        }

        val seda001 = mapper.mapSEDA001(brevData)

        seda001.lovvalgsbestemmelse.value() shouldBe Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E.kode
        seda001.ytterligereInformasjon shouldBe "Issued under the EEA EFTA Convention. Fritekst fra saksbehandler."
    }

    // --- Helper functions ---

    private fun lagFellesTypeMedNavFelles(): Pair<FellesType, MelosysNAVFelles> {
        val fellesType = FellesType().apply {
            fagsaksnummer = "MELTEST-2"
        }
        val navFelles = easyRandom.nextObject(MelosysNAVFelles::class.java).apply {
            mottaker.mottakeradresse = BrevDataUtils.lagNorskPostadresse()
            kontaktinformasjon = BrevDataUtils.lagKontaktInformasjon()
        }
        return fellesType to navFelles
    }

    private fun lagBrevDataA001(init: BrevDataA001Builder.() -> Unit = {}): BrevDataA001 =
        BrevDataA001Builder().apply(init).build()

    private class BrevDataA001Builder {
        var bostedsadresseTypeKode: BostedsadresseTypeKode? = null
        var anmodningsperioder: Collection<Anmodningsperiode>? = null
        var anmodningBegrunnelser: Set<VilkaarBegrunnelse>? = null
        var anmodningUtenArt12Begrunnelser: Set<VilkaarBegrunnelse>? = null
        var ytterligereInformasjon: String? = null

        fun build(): BrevDataA001 {
            val strukturertAdresse = BrevDataTestUtils.lagStrukturertAdresse()
            val person = personDokumentForTest {
                kjønn = no.nav.melosys.domain.dokument.person.KjoennsType("K")
                fornavn = "Ola"
                etternavn = "Nordmann"
                fødselsdato = LocalDate.now()
                fnr = "123456789"
                statsborgerskap = no.nav.melosys.domain.dokument.felles.Land(no.nav.melosys.domain.dokument.felles.Land.NORGE)
            }
            val boAdresse = StrukturertAdresse(
                gatenavn = "Gatenavn",
                husnummerEtasjeLeilighet = "23A",
                postnummer = "0165",
                poststed = "Oslo",
                region = null,
                landkode = Landkoder.NO.kode
            )
            val virksomhet = AvklartVirksomhet(
                "JARLSBERG AS",
                "123456789",
                strukturertAdresse,
                Yrkesaktivitetstyper.LOENNET_ARBEID
            )
            val fysiskArbeidssted = FysiskArbeidssted(
                "JARLSBERG INTERNATIONAL",
                "123456789",
                strukturertAdresse
            )
            val maritimtArbeidssted = BrevDataTestUtils.lagMaritimtArbeidssted()
            // UtenlandskMyndighet har ikke forTest DSL - bruker .apply
            val myndighet = UtenlandskMyndighet().apply {
                navn = "SAV"
                institusjonskode = "23"
                gateadresse1 = "Adresse"
                postnummer = "0165"
                poststed = "Stockholm"
                landkode = Land_iso2.SK
            }
            val vilkår16 = BrevDataTestUtils.lagVilkaarsresultat(Vilkaar.FO_883_2004_ART16_1, true, UTSENDELSE_MELLOM_24_MN_OG_5_AAR)
            val vilkår16Uten12 = BrevDataTestUtils.lagVilkaarsresultat(Vilkaar.FO_883_2004_ART16_1, true, SJOEMANNSKIRKEN)
            val defaultAnmodningsperiode = Anmodningsperiode(
                LocalDate.now(),
                LocalDate.now(),
                Land_iso2.NO,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2,
                Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5,
                Land_iso2.NO,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
                Trygdedekninger.FULL_DEKNING_EOSFO
            )

            return BrevDataA001(
                arbeidsgivendeVirksomheter = mutableListOf(virksomhet),
                selvstendigeVirksomheter = mutableListOf(),
                arbeidssteder = mutableListOf(fysiskArbeidssted, maritimtArbeidssted),
                persondata = person,
                bostedsadresse = boAdresse,
                bostedsadresseTypeKode = bostedsadresseTypeKode,
                utenlandskMyndighet = myndighet,
                anmodningsperioder = anmodningsperioder ?: mutableSetOf(defaultAnmodningsperiode),
                anmodningBegrunnelser = anmodningBegrunnelser ?: vilkår16.begrunnelser,
                anmodningUtenArt12Begrunnelser = anmodningUtenArt12Begrunnelser ?: vilkår16Uten12.begrunnelser,
                utenlandskIdent = null,
                ansettelsesperiode = null,
                ytterligereInformasjon = ytterligereInformasjon
            )
        }
    }

    private fun lagAnmodningsperiode(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = LocalDate.now(),
        lovvalgsland: Land_iso2 = Land_iso2.NO,
        bestemmelse: LovvalgBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2,
        tilleggsbestemmelse: LovvalgBestemmelse? = Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5,
        unntakFraLovvalgsland: Land_iso2 = Land_iso2.NO,
        unntakFraBestemmelse: LovvalgBestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
        trygdedekning: Trygdedekninger = Trygdedekninger.FULL_DEKNING_EOSFO
    ): Anmodningsperiode = Anmodningsperiode(
        fom,
        tom,
        lovvalgsland,
        bestemmelse,
        tilleggsbestemmelse,
        unntakFraLovvalgsland,
        unntakFraBestemmelse,
        trygdedekning
    )

    private fun mapTilBrevXML(fellesType: FellesType, navFelles: MelosysNAVFelles, brevData: BrevDataA001): String {
        val XSD_LOCATION = "melosysbrev/melosys_000116.xsd"
        val vedlegg = VedleggType().apply {
            sedA001 = mapper.mapSEDA001(brevData)
        }
        return JaxbHelper.marshalAndValidate(
            mapintoBrevdataType(fellesType, navFelles, lagFag(), vedlegg),
            XSD_LOCATION
        )
    }

    private fun lagFag() = Fag().apply {
        vedleggSEDA001 = "true"
    }

    private fun mapintoBrevdataType(
        fellesType: FellesType,
        navFelles: MelosysNAVFelles,
        fag: Fag,
        vedlegg: VedleggType
    ): JAXBElement<BrevdataType> {
        val factory = ObjectFactory()
        val brevdataType = factory.createBrevdataType()
        brevdataType.felles = fellesType
        brevdataType.navFelles = navFelles
        brevdataType.fag = fag
        brevdataType.vedlegg = vedlegg
        return factory.createBrevdata(brevdataType)
    }
}
