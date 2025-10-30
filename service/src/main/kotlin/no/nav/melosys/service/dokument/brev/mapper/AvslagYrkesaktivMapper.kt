package no.nav.melosys.service.dokument.brev.mapper


import jakarta.xml.bind.JAXBElement
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
import no.nav.melosys.domain.kodeverk.begrunnelser.Avslag_anmodning_begrunnelser
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.service.dokument.brev.BrevData
import no.nav.melosys.service.dokument.brev.BrevDataAvslagYrkesaktiv
import no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone
import no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactory.*


open class AvslagYrkesaktivMapper : BrevDataMapper {

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
        fag.art161AvslagBegrunnelse = lagTomArt161AvslagBegrunnelse()
    }

    private fun mapFag(
        behandling: Behandling,
        resultat: Behandlingsresultat,
        brevData: BrevDataAvslagYrkesaktiv
    ): Fag = Fag().apply {
        if (behandling.fagsak.type == Sakstyper.EU_EOS) {
            inngangsvilkårBegrunnelse = InngangsvilkaarBegrunnelseKode.EOS_BORGER
        } else {
            throw TekniskException("Forholdet er ikke dekket av inngangsvilkårene for 883/2004")
        }

        foretakNavn = brevData.hovedvirksomhet?.navn
        yrkesaktivitet = YrkesaktivitetsKode.fromValue(brevData.yrkesaktivitet?.kode)
        arbeidsland = brevData.arbeidsland
        lovvalgsperiode = lagLovvalgsperiodeType(resultat)

        art121Begrunnelse = mapArt121BegrunnelseType(
            resultat.hentVilkaarbegrunnelser(
                FO_883_2004_ART12_1,
                KONV_EFTA_STORBRITANNIA_ART14_1,
                KONV_EFTA_STORBRITANNIA_ART16_1
            )
        )
        art121ForutgåendeBegrunnelse = mapArt121ForutgaaendeBegrunnelseType(resultat.hentVilkaarbegrunnelser(FORUTGAAENDE_MEDLEMSKAP))
        art122Begrunnelse = mapArt122BegrunnelseType(
            resultat.hentVilkaarbegrunnelser(
                FO_883_2004_ART12_2,
                KONV_EFTA_STORBRITANNIA_ART14_2,
                KONV_EFTA_STORBRITANNIA_ART16_3
            )
        )
        art122NormalVirksomhetBegrunnelse = mapArt122NormalVirksomhetBegrunnelseType(resultat.hentVilkaarbegrunnelser(NORMALT_DRIVER_VIRKSOMHET))

        fritekst = brevData.fritekst

        anmodningsPeriodeSvarType = brevData.anmodningsperiodeSvar?.anmodningsperiodeSvarType?.let {
            AnmodningsPeriodeSvarTypeKode.valueOf(it.kode)
        }

        if (brevData.art16UtenArt12) {
            art16UtenArt12 = JA
        }
    }

    fun mapArt161Avslag(fag: Fag, brevdata: BrevDataAvslagYrkesaktiv) {
        val vilkaarsresultat = brevdata.art16Vilkaar
        val art161Begrunnelser = vilkaarsresultat?.begrunnelser
        val art161AvslagBegrunnelser = lagTomArt161AvslagBegrunnelse()
        art161Begrunnelser?.forEach { vilkaarBegrunnelse ->
            val begrunnelseKode = Avslag_anmodning_begrunnelser.valueOf(vilkaarBegrunnelse.kode)
            when (begrunnelseKode) {
                Avslag_anmodning_begrunnelser.OVER_5_AAR -> art161AvslagBegrunnelser.over5Aar = JA
                Avslag_anmodning_begrunnelser.INGEN_SPESIELLE_FORHOLD -> art161AvslagBegrunnelser.ingenSpesielleForhold = JA
                Avslag_anmodning_begrunnelser.SAERLIG_AVSLAGSGRUNN -> {
                    art161AvslagBegrunnelser.saerligAvslagsgrunn = JA
                    fag.begrunnelseFritekst = validerFritekstbegrunnelse(vilkaarsresultat.begrunnelseFritekst)
                }

                Avslag_anmodning_begrunnelser.SOEKT_FOR_SENT -> art161AvslagBegrunnelser.soektForSent = JA
                else -> throw TekniskException("$begrunnelseKode støttes ikke.")
            }
        }

        fag.art161AvslagBegrunnelse = art161AvslagBegrunnelser
    }

    private fun lagTomArt161AvslagBegrunnelse(): Art161AvslagBegrunnelse =
        Art161AvslagBegrunnelse().withIngenSpesielleForhold("")
            .withOver5Aar("")
            .withSaerligAvslagsgrunn("")
            .withSoektForSent("")

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

        lovvalgsperiodeType.fomDato = convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.hentFom())
        lovvalgsperiodeType.tomDato = convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.tom)

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

    companion object {
        private const val XSD_LOCATION = "melosysbrev/melosys_000081.xsd"
        private const val JA = "true"
    }
}
