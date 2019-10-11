package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Collection;
import java.util.Set;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.melosysbrev._000084.BrevdataType;
import no.nav.dok.melosysbrev._000084.Fag;
import no.nav.dok.melosysbrev._000084.LovvalgsperiodeType;
import no.nav.dok.melosysbrev._000084.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.*;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import static no.nav.melosys.domain.kodeverk.Vilkaar.*;
import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;
import static no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactory.*;

public class AnmodningUnntakMapper implements BrevDataMapper {

    private static final Logger log = LoggerFactory.getLogger(AnmodningUnntakMapper.class);

    private static final String XSD_LOCATION = "melosysbrev/melosys_000084.xsd";

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat,
                                BrevData brevDataFelles) throws JAXBException, SAXException, TekniskException {
        BrevDataAnmodningUnntakOgAvslag brevdata = (BrevDataAnmodningUnntakOgAvslag) brevDataFelles;
        Fag fag = mapFag(behandling, resultat, brevdata);
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    Fag mapFag(Behandling behandling, Behandlingsresultat resultat, BrevDataAnmodningUnntakOgAvslag brevData) throws TekniskException {
        Fag fag = new Fag();
        if (behandling.getFagsak().getType() == Sakstyper.EU_EOS) {
            // Respons fra regelmodulen skiller ikke mellom begrunnelser for 883/2004 (MELOSYS-1863)
            fag.setInngangsvilkårBegrunnelse(InngangsvilkaarBegrunnelseKode.EOS_BORGER);
        } else {
            throw new TekniskException("Forholdet er ikke dekket av inngangsvilkårene for 883/2004");
        }

        fag.setForetakNavn(brevData.hovedvirksomhet.navn);
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

        Collection<VilkaarBegrunnelse> art16Begrunnelser = brevData.art16Vilkaar.map(Vilkaarsresultat::getBegrunnelser)
            .orElseThrow(() -> new TekniskException("Ingen begrunnelse funnet for brev om Artikkel 16.1"));

        art16Begrunnelser.stream()
            .map(VilkaarBegrunnelse::getKode)
            .map(Art161AnmodningBegrunnelseKode::valueOf)
            .filter(begrunnelse -> begrunnelse != Art161AnmodningBegrunnelseKode.SAERLIG_GRUNN)
            .findFirst()
        .ifPresent(fag::setArt161AnmodningBegrunnelse);

        return fag;
    }

    LovvalgsperiodeType lagLovvalgsperiodeType(Behandlingsresultat resultat) {
        Anmodningsperiode anmodningsperiode = resultat.hentValidertAnmodningsperiode();
        LovvalgsperiodeType lovvalgsperiodeType = new LovvalgsperiodeType();
        lovvalgsperiodeType.setUnntakFraLovvalgsland(anmodningsperiode.getUnntakFraLovvalgsland().getBeskrivelse());

        try {
            lovvalgsperiodeType.setFomDato(convertToXMLGregorianCalendarRemoveTimezone(anmodningsperiode.getFom()));
            lovvalgsperiodeType.setTomDato(convertToXMLGregorianCalendarRemoveTimezone(anmodningsperiode.getTom()));
        } catch (DatatypeConfigurationException e) {
            log.error("", e);
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
