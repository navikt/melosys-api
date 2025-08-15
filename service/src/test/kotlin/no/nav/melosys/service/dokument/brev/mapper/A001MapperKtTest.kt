package no.nav.melosys.service.dokument.brev.mapper

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import jakarta.xml.bind.JAXBElement
import no.nav.dok.melosysbrev._000115.BostedsadresseTypeKode
import no.nav.dok.melosysbrev._000116.BrevdataType
import no.nav.dok.melosysbrev._000116.Fag
import no.nav.dok.melosysbrev._000116.ObjectFactory
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles
import no.nav.dok.melosysbrev.felles.melosys_vedlegg.VedleggType
import no.nav.melosys.domain.*
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet
import no.nav.melosys.domain.dokument.person.KjoennsType
import no.nav.melosys.domain.dokument.person.PersonDokument
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser.UTSENDELSE_MELLOM_24_MN_OG_5_AAR
import no.nav.melosys.domain.kodeverk.begrunnelser.Direkte_til_anmodning_begrunnelser.SJOEMANNSKIRKEN
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import no.nav.melosys.service.dokument.brev.BrevDataA001
import no.nav.melosys.service.dokument.brev.BrevDataTestUtils.*
import no.nav.melosys.service.dokument.brev.BrevDataUtils
import org.jeasy.random.EasyRandom
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class A001MapperKtTest {

    private lateinit var mapper: A001Mapper
    private lateinit var easyRandom: EasyRandom
    private lateinit var brevData: BrevDataA001

    @BeforeEach
    fun setup() {
        mapper = A001Mapper()
        easyRandom = EasyRandomConfigurer.randomForDokProd()

        val anmodningsperiode = Anmodningsperiode(
            LocalDate.now(),
            LocalDate.now(),
            Land_iso2.NO,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2,
            Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5,
            Land_iso2.NO,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
            Trygdedekninger.FULL_DEKNING_EOSFO
        )

        val behandlingsresultat = mockk<Behandlingsresultat>()
        every { behandlingsresultat.registrertDato } returns Instant.now()

        val boAdresse = StrukturertAdresse().apply {
            gatenavn = "Gatenavn"
            husnummerEtasjeLeilighet = "23A"
            postnummer = "0165"
            poststed = "Oslo"
            landkode = Landkoder.NO.kode
        }

        val person = PersonDokument().apply {
            kjønn = KjoennsType("K")
            fornavn = "Ola"
            etternavn = "Nordmann"
            fødselsdato = LocalDate.now()
            fnr = "123456789"
            statsborgerskap = no.nav.melosys.domain.dokument.felles.Land(no.nav.melosys.domain.dokument.felles.Land.NORGE)
        }

        val saksopplysning = Saksopplysning().apply {
            type = SaksopplysningType.PERSOPL
            dokument = person
        }

        val behandling = mockk<Behandling>()
        every { behandling.registrertDato } returns Instant.now()
        every { behandling.saksopplysninger } returns mutableSetOf(saksopplysning)
        every { behandling.fagsak } returns Fagsak.forTest()

        val strukturertAdresse = lagStrukturertAdresse()

        val arbeidssted = FysiskArbeidssted(null, strukturertAdresse)
        val søknad = Soeknad()
        søknad.arbeidPaaLand.fysiskeArbeidssteder = listOf(arbeidssted)

        val virksomhet = AvklartVirksomhet(
            "JARLSBERG AS",
            "123456789",
            strukturertAdresse,
            Yrkesaktivitetstyper.LOENNET_ARBEID
        )

        val fysiskArbeidssted = no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted(
            "JARLSBERG INTERNATIONAL",
            "123456789",
            strukturertAdresse
        )

        val maritimtArbeidssted = lagMaritimtArbeidssted()

        val myndighet = UtenlandskMyndighet().apply {
            navn = "SAV"
            institusjonskode = "23"
            gateadresse1 = "Adresse"
            postnummer = "0165"
            poststed = "Stockholm"
            landkode = Land_iso2.SK
        }

        val vilkår16 = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART16_1, true, UTSENDELSE_MELLOM_24_MN_OG_5_AAR)
        val vilkår16Uten12 = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART16_1, true, SJOEMANNSKIRKEN)

        brevData = BrevDataA001().apply {
            arbeidsgivendeVirksomheter = mutableListOf(virksomhet)
            selvstendigeVirksomheter = mutableListOf()
            arbeidssteder = mutableListOf(fysiskArbeidssted, maritimtArbeidssted)
            persondata = person
            bostedsadresse = boAdresse
            utenlandskMyndighet = myndighet
            anmodningsperioder = listOf(anmodningsperiode)
            anmodningBegrunnelser = vilkår16.begrunnelser
            anmodningUtenArt12Begrunnelser = vilkår16Uten12.begrunnelser
            utenlandskIdent = null
            ansettelsesperiode = null
        }
    }

    @Test
    fun `mapTilBrevXML skal fungere med Art16 begrunnelser`() {
        val fellesType = FellesType().apply {
            fagsaksnummer = "MELTEST-2"
        }

        val navFelles = easyRandom.nextObject(MelosysNAVFelles::class.java)
        navFelles.mottaker.mottakeradresse = BrevDataUtils.lagNorskPostadresse()
        navFelles.kontaktinformasjon = BrevDataUtils.lagKontaktInformasjon()

        brevData.anmodningUtenArt12Begrunnelser = mutableSetOf()


        val xml = mapTilBrevXML(fellesType, navFelles, brevData)


        xml.shouldNotBeNull()
    }

    @Test
    fun `mapTilBrevXML skal fungere med Art16 uten Art12 begrunnelser`() {
        val fellesType = FellesType().apply {
            fagsaksnummer = "MELTEST-2"
        }

        val navFelles = easyRandom.nextObject(MelosysNAVFelles::class.java)
        navFelles.mottaker.mottakeradresse = BrevDataUtils.lagNorskPostadresse()
        navFelles.kontaktinformasjon = BrevDataUtils.lagKontaktInformasjon()

        brevData.anmodningBegrunnelser = mutableSetOf()


        val xml = mapTilBrevXML(fellesType, navFelles, brevData)


        xml.shouldNotBeNull()
    }

    @Test
    fun `mapTilBrevXML skal fungere uten selvstendig virksomhet`() {
        val fellesType = FellesType().apply {
            fagsaksnummer = "MELTEST-2"
        }

        val navFelles = easyRandom.nextObject(MelosysNAVFelles::class.java)
        navFelles.mottaker.mottakeradresse = BrevDataUtils.lagNorskPostadresse()
        navFelles.kontaktinformasjon = BrevDataUtils.lagKontaktInformasjon()


        val xml = mapTilBrevXML(fellesType, navFelles, brevData)


        xml.shouldNotBeNull()
    }

    @Test
    fun `mapSEDA001 skal sette korrekt adressetype for kontaktadresse`() {
        brevData.bostedsadresseTypeKode = BostedsadresseTypeKode.KONTAKTADRESSE


        val seda001 = mapper.mapSEDA001(brevData)


        seda001.person.bostedsadresse.adresseType shouldBe BostedsadresseTypeKode.KONTAKTADRESSE
    }

    @Test
    fun `mapSEDA001 skal sette korrekt adressetype når ingen adressetype er gitt`() {
        brevData.bostedsadresseTypeKode = null


        val seda001 = mapper.mapSEDA001(brevData)


        seda001.person.bostedsadresse.adresseType shouldBe BostedsadresseTypeKode.BOSTEDSLAND
    }

    @Test
    fun `mapSEDA001 skal mappe Storbritannia korrekt`() {
        brevData.anmodningsperioder.forEach { periode ->
            periode.bestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1
            periode.unntakFraBestemmelse = Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3D
        }
        brevData.ytterligereInformasjon = "Fritekst fra saksbehandler."


        val seda001 = mapper.mapSEDA001(brevData)


        seda001.lovvalgsbestemmelse.value() shouldBe Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E.kode
        seda001.ytterligereInformasjon shouldBe "Issued under the EEA EFTA Convention. Fritekst fra saksbehandler."
    }

    private fun mapTilBrevXML(fellesType: FellesType, navFelles: MelosysNAVFelles, brevData: BrevDataA001): String {
        val XSD_LOCATION = "melosysbrev/melosys_000116.xsd"
        return JaxbHelper.marshalAndValidate(mapintoBrevdataType(fellesType, navFelles, mapFag(), VedleggType().apply {
            this.sedA001 = mapper.mapSEDA001(brevData)
        }), XSD_LOCATION)
    }

    private fun mapFag() = Fag().apply {
        this.vedleggSEDA001 = "true"
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
