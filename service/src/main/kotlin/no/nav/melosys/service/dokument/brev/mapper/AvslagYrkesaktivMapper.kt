package no.nav.melosys.service.dokument.brev.mapper


import no.nav.dok.melosysbrev._000081.BrevdataType
import no.nav.dok.melosysbrev._000081.Fag
import no.nav.dok.melosysbrev._000081.LovvalgsperiodeType
import no.nav.dok.melosysbrev._000081.ObjectFactory
import no.nav.dok.melosysbrev.felles.melosys_felles.*
import no.nav.melosys.domain.AnmodningsperiodeSvar
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Vilkaar.*
import no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_avslag
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.service.dokument.brev.BrevData
import no.nav.melosys.service.dokument.brev.BrevDataAvslagYrkesaktiv
import no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone
import no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactory.*
import org.xml.sax.SAXException
import javax.xml.bind.JAXBElement
import javax.xml.bind.JAXBException
import javax.xml.datatype.DatatypeConfigurationException


//TODO sjekk alle null safe operasjoner
open class AvslagYrkesaktivMapper : BrevDataMapper {

    companion object {
        private const val XSD_LOCATION = "melosysbrev/melosys_000081.xsd"
        private const val JA = "true"
    }

    @Throws(JAXBException::class, SAXException::class)
    override fun mapTilBrevXML(
        fellesType: FellesType,
        navFelles: MelosysNAVFelles,
        behandling: Behandling,
        resultat: Behandlingsresultat,
        brevDataFelles: BrevData
    ): String {
        val brevdata = brevDataFelles as BrevDataAvslagYrkesaktiv
        val fag = mapFag(behandling, resultat, brevdata)

        if (brevdata.anmodningsperiodeSvar != null) {
            mapArt161AvslagFraAnmodningsperiode(fag, brevdata.anmodningsperiodeSvar!!)
        } else {
            mapArt161Avslag(fag, brevdata)
        }

        val brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag)
        return JaxbHelper.marshalAndValidate(brevdataTypeJAXBElement, XSD_LOCATION)
    }

    private fun mapArt161AvslagFraAnmodningsperiode(fag: Fag, svar: AnmodningsperiodeSvar) {
        fag.begrunnelseFritekst = svar.begrunnelseFritekst
        val avslagBegrunnelse = lagTomArt161AvslagBegrunnelse()
        fag.art161AvslagBegrunnelse = avslagBegrunnelse
    }

    private fun mapFag(
        behandling: Behandling,
        resultat: Behandlingsresultat,
        brevData: BrevDataAvslagYrkesaktiv
    ): Fag {
        val fag = Fag()

        if (behandling.fagsak.type == Sakstyper.EU_EOS) {
            fag.inngangsvilkårBegrunnelse = InngangsvilkaarBegrunnelseKode.EOS_BORGER
        } else {
            throw TekniskException("Forholdet er ikke dekket av inngangsvilkårene for 883/2004")
        }

        fag.foretakNavn = brevData.hovedvirksomhet?.navn
        fag.yrkesaktivitet = YrkesaktivitetsKode.fromValue(brevData.yrkesaktivitet?.kode)

        fag.arbeidsland = brevData.arbeidsland
        fag.lovvalgsperiode = lagLovvalgsperiodeType(resultat)

        val art121Begrunnelser = resultat.hentVilkaarbegrunnelser(FO_883_2004_ART12_1)
        fag.art121Begrunnelse = mapArt121BegrunnelseType(art121Begrunnelser)

        val art121ForutgåendeBegrunnelser = resultat.hentVilkaarbegrunnelser(ART12_1_FORUTGAAENDE_MEDLEMSKAP)
        fag.art121ForutgåendeBegrunnelse = mapArt121ForutgaaendeBegrunnelseType(art121ForutgåendeBegrunnelser)

        val art122Begrunnelser = resultat.hentVilkaarbegrunnelser(FO_883_2004_ART12_2)
        fag.art122Begrunnelse = mapArt122BegrunnelseType(art122Begrunnelser)

        val art122NormalVirksomhetBegrunnelse = resultat.hentVilkaarbegrunnelser(ART12_2_NORMALT_DRIVER_VIRKSOMHET)
        fag.art122NormalVirksomhetBegrunnelse = mapArt122NormalVirksomhetBegrunnelseType(art122NormalVirksomhetBegrunnelse)

        fag.fritekst = brevData.fritekst

        fag.anmodningsPeriodeSvarType = brevData.anmodningsperiodeSvar?.anmodningsperiodeSvarType?.let {
            AnmodningsPeriodeSvarTypeKode.valueOf(
                it.kode
            )
        }

        if (brevData.art16UtenArt12) {
            fag.art16UtenArt12 = JA
        }

        return fag
    }

    public fun mapArt161Avslag(fag: Fag, brevdata: BrevDataAvslagYrkesaktiv) {
        val vilkaarsresultat = brevdata.art16Vilkaar
        val art161Begrunnelser = vilkaarsresultat?.begrunnelser
        val art161AvslagBegrunnelser = lagTomArt161AvslagBegrunnelse()
        art161Begrunnelser?.forEach { vilkaarBegrunnelse ->
            val artikkel161AvslagKode = Art16_1_avslag.valueOf(vilkaarBegrunnelse.kode)
            when (artikkel161AvslagKode) {
                Art16_1_avslag.OVER_5_AAR -> art161AvslagBegrunnelser.over5Aar = JA
                Art16_1_avslag.INGEN_SPESIELLE_FORHOLD -> art161AvslagBegrunnelser.ingenSpesielleForhold = JA
                Art16_1_avslag.SAERLIG_AVSLAGSGRUNN -> {
                    art161AvslagBegrunnelser.saerligAvslagsgrunn = JA
                    fag.begrunnelseFritekst = validerFritekstbegrunnelse(vilkaarsresultat.begrunnelseFritekst)
                }
                Art16_1_avslag.SOEKT_FOR_SENT -> art161AvslagBegrunnelser.soektForSent = JA
                else -> throw TekniskException("$artikkel161AvslagKode støttes ikke.")
            }
        }

        fag.art161AvslagBegrunnelse = art161AvslagBegrunnelser
    }

    private fun lagTomArt161AvslagBegrunnelse(): Art161AvslagBegrunnelse {
        return Art161AvslagBegrunnelse.builder().withIngenSpesielleForhold("")
            .withOver5Aar("")
            .withSaerligAvslagsgrunn("")
            .withSoektForSent("").build()
    }

    private fun validerFritekstbegrunnelse(begrunnelse: String?): String {
        if (!begrunnelse.isNullOrEmpty()) {
            return begrunnelse
        } else {
            throw TekniskException("Ingen fritekstbegrunnelse satt for Artikkel 16.1")
        }
    }

    private fun lagLovvalgsperiodeType(resultat: Behandlingsresultat): LovvalgsperiodeType {
        val lovvalgsperiode = resultat.hentLovvalgsperiode()
        val lovvalgsperiodeType = LovvalgsperiodeType()

        try {
            lovvalgsperiodeType.fomDato = convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.fom)
            lovvalgsperiodeType.tomDato = convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.tom)
        } catch (e: DatatypeConfigurationException) {
            throw TekniskException(e)
        }
        return lovvalgsperiodeType
    }

    private fun mapintoBrevdataType(
        fellesType: FellesType,
        navFelles: MelosysNAVFelles,
        fag: Fag
    ): JAXBElement<BrevdataType> {
        val factory = ObjectFactory()
        val brevdataType = factory.createBrevdataType().apply {
            this.felles = fellesType
            this.navFelles = navFelles
            this.fag = fag
        }
        return factory.createBrevdata(brevdataType)
    }
}
