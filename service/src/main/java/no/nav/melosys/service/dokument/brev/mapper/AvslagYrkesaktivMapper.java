package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.melosysbrev._000081.BrevdataType;
import no.nav.dok.melosysbrev._000081.Fag;
import no.nav.dok.melosysbrev._000081.LovvalgsperiodeType;
import no.nav.dok.melosysbrev._000081.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.*;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_avslag;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import static no.nav.melosys.domain.kodeverk.Vilkaar.*;
import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;
import static no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactory.*;

public class AvslagYrkesaktivMapper implements BrevDataMapper {

    private static final String XSD_LOCATION = "melosysbrev/melosys_000081.xsd";
    private static final String JA = "true";

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat,
                                BrevData brevDataFelles) throws JAXBException, SAXException, TekniskException {
        BrevDataAnmodningUnntakOgAvslag brevdata = (BrevDataAnmodningUnntakOgAvslag) brevDataFelles;
        Fag fag = mapFag(behandling, resultat, brevdata);

        if (brevdata.anmodningsperiodeSvar.isPresent()) {
            brevdata.anmodningsperiodeSvar.ifPresent(aps ->
                mapArt161AvslagFraAnmodningsperiode(fag, aps));
        } else {
            mapArt161Avslag(fag, brevdata);
        }

        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    private void mapArt161AvslagFraAnmodningsperiode(Fag fag, AnmodningsperiodeSvar svar) {
        fag.setBegrunnelseFritekst(svar.getBegrunnelseFritekst());
        Art161AvslagBegrunnelse avslagBegrunnelse = lagTomArt161AvslagBegrunnelse();
        avslagBegrunnelse.setUtlAvslaarAvtale(JA);
        fag.setArt161AvslagBegrunnelse(avslagBegrunnelse);
    }

    private Fag mapFag(Behandling behandling, Behandlingsresultat resultat, BrevDataAnmodningUnntakOgAvslag brevData) throws TekniskException {
        Fag fag = new Fag();

        if (behandling.getFagsak().getType() == Sakstyper.EU_EOS) {
            // Respons fra regelmodulen skiller ikke mellom begrunnelser for 883/2004 (MELOSYS-1863)
            fag.setInngangsvilkårBegrunnelse(InngangsvilkaarBegrunnelseKode.EOS_BORGER);
        } else {
            throw new TekniskException("Forholdet er ikke dekket av inngangsvilkårene for 883/2004");
        }

        fag.setForetakNavn(brevData.hovedvirksomhet.navn);
        // Frilansaktivitet håndteres ikke i Lev 1
        fag.setYrkesaktivitet(YrkesaktivitetsKode.fromValue(brevData.yrkesaktivitet.getKode()));

        fag.setArbeidsland(brevData.arbeidsland);
        fag.setLovvalgsperiode(lagLovvalgsperiodeType(resultat));

        Set<VilkaarBegrunnelse> art121Begrunnelser = hentVilkaarbegrunnelser(resultat, FO_883_2004_ART12_1);
        fag.setArt121Begrunnelse(mapArt121BegrunnelseType(art121Begrunnelser));

        Set<VilkaarBegrunnelse> art121ForutgåendeBegrunnelser = hentVilkaarbegrunnelser(resultat, ART12_1_FORUTGAAENDE_MEDLEMSKAP);
        fag.setArt121ForutgåendeBegrunnelse(mapArt121ForutgaaendeBegrunnelseType(art121ForutgåendeBegrunnelser));

        Set<VilkaarBegrunnelse> art122Begrunnelser = hentVilkaarbegrunnelser(resultat, FO_883_2004_ART12_2);
        fag.setArt122Begrunnelse(mapArt122BegrunnelseType(art122Begrunnelser));

        Set<VilkaarBegrunnelse> art122NormalVirksomhetBegrunnelse = hentVilkaarbegrunnelser(resultat, ART12_2_NORMALT_DRIVER_VIRKSOMHET);
        fag.setArt122NormalVirksomhetBegrunnelse(mapArt122NormalVirksomhetBegrunnelseType(art122NormalVirksomhetBegrunnelse));

        fag.setAvslag(JA);

        return fag;
    }

    void mapArt161Avslag(Fag fag, BrevDataAnmodningUnntakOgAvslag brevdata) throws TekniskException {
        Optional<Vilkaarsresultat> vilkaarsresultat = brevdata.art16Vilkaar;
        Set<VilkaarBegrunnelse> art161Begrunnelser = vilkaarsresultat.map(Vilkaarsresultat::getBegrunnelser).orElse(Collections.emptySet());
        Art161AvslagBegrunnelse art161AvslagBegrunnelser = lagTomArt161AvslagBegrunnelse();
        for (VilkaarBegrunnelse vilkaarBegrunnelse : art161Begrunnelser) {
            Art16_1_avslag artikkel161AvslagKode = Art16_1_avslag.valueOf(vilkaarBegrunnelse.getKode());
            switch (artikkel161AvslagKode) {
                case OVER_12_MD_UTL_ARBEIDSGIVER:
                case OVER_5_AAR:
                    art161AvslagBegrunnelser.setOver5Aar(JA);
                    break;
                case INGEN_SPESIELLE_FORHOLD:
                    art161AvslagBegrunnelser.setIngenSpesielleForhold(JA);
                    break;
                case SAERLIG_AVSLAGSGRUNN:
                    art161AvslagBegrunnelser.setSaerligAvslagsgrunn(JA);
                    Vilkaarsresultat v = vilkaarsresultat.orElseThrow(IllegalStateException::new);
                    fag.setBegrunnelseFritekst(validerFritekstbegrunnelse(v.getBegrunnelseFritekst()));
                    break;
                case SOEKT_FOR_SENT:
                    art161AvslagBegrunnelser.setSoektForSent(JA);
                    break;
                default:
                    throw new TekniskException(artikkel161AvslagKode + " støttes ikke.");
            }
        }

        fag.setArt161AvslagBegrunnelse(art161AvslagBegrunnelser);
    }

    private static Art161AvslagBegrunnelse lagTomArt161AvslagBegrunnelse() {
        return Art161AvslagBegrunnelse.builder().withIngenSpesielleForhold("")
            .withOver5Aar("")
            .withSaerligAvslagsgrunn("")
            .withUtlAvslaarAvtale("")
            .withSoektForSent("").build();
    }

    static String validerFritekstbegrunnelse(String begrunnelse) throws TekniskException {
        if (!StringUtils.isEmpty(begrunnelse)) {
            return begrunnelse;
        } else {
            throw new TekniskException("Ingen fritekstbegrunnelse satt for Artikkel 16.1");
        }
    }

    private static LovvalgsperiodeType lagLovvalgsperiodeType(Behandlingsresultat resultat) throws TekniskException {
        Lovvalgsperiode lovvalgsperiode = resultat.hentValidertLovvalgsperiode();
        LovvalgsperiodeType lovvalgsperiodeType = new LovvalgsperiodeType();

        try {
            lovvalgsperiodeType.setFomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getFom()));
            lovvalgsperiodeType.setTomDato(convertToXMLGregorianCalendarRemoveTimezone(lovvalgsperiode.getTom()));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException(e);
        }
        return lovvalgsperiodeType;
    }

    private static JAXBElement<BrevdataType> mapintoBrevdataType(FellesType fellesType, MelosysNAVFelles navFelles, Fag fag) {
        ObjectFactory factory = new ObjectFactory();
        BrevdataType brevdataType = factory.createBrevdataType();
        brevdataType.setFelles(fellesType);
        brevdataType.setNAVFelles(navFelles);
        brevdataType.setFag(fag);
        return factory.createBrevdata(brevdataType);
    }
}