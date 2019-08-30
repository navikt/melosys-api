package no.nav.melosys.service.dokument.brev.mapper;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.melosysbrev._000084.BrevdataType;
import no.nav.dok.melosysbrev._000084.Fag;
import no.nav.dok.melosysbrev._000084.LovvalgsperiodeType;
import no.nav.dok.melosysbrev._000084.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.Art161AnmodningBegrunnelseKode;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.*;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;


public class AnmodningUnntakMapper extends AbstraktAnmodningUnntakOgAvslagMapper {

    private static final Logger log = LoggerFactory.getLogger(AnmodningUnntakMapper.class);

    private static final String XSD_LOCATION = "melosysbrev/melosys_000084.xsd";

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat,
                                BrevData brevData) throws JAXBException, SAXException, TekniskException {
        Fag fag = til000084Fag(mapFag(behandling, resultat, (BrevDataAnmodningUnntakOgAvslag) brevData));
        mapArt161(fag, resultat);
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    private void mapArt161(Fag fag, Behandlingsresultat resultat) throws TekniskException {

        Vilkaarsresultat vilkaarsresultat = hentFørsteGyldigeVilkaarsresultatForArt16(resultat)
            .orElseThrow(() -> new TekniskException("Ingen begrunnelse funnet for brev om Artikkel 16.1"));

        VilkaarBegrunnelse vilkaarBegrunnelse = vilkaarsresultat.getBegrunnelser().iterator().next();
        fag.setArt161AnmodningBegrunnelse(Art161AnmodningBegrunnelseKode.valueOf(vilkaarBegrunnelse.getKode()));
    }

    private static JAXBElement<BrevdataType> mapintoBrevdataType(FellesType fellesType, MelosysNAVFelles navFelles, Fag fag) {
        ObjectFactory factory = new ObjectFactory();
        BrevdataType brevdataType = factory.createBrevdataType();
        brevdataType.setFelles(fellesType);
        brevdataType.setNAVFelles(navFelles);
        brevdataType.setFag(fag);
        return factory.createBrevdata(brevdataType);
    }

    private static Fag til000084Fag(no.nav.dok.melosysbrev._000081.Fag fag000081) {
        Fag fag = new Fag();

        fag.setAnmodningFritekst(fag000081.getAnmodningFritekst());
        fag.setArbeidsland(fag000081.getArbeidsland());
        fag.setAvslag(fag000081.getAvslag());
        fag.setArt121Begrunnelse(fag000081.getArt121Begrunnelse());
        fag.setArt121ForutgåendeBegrunnelse(fag000081.getArt121ForutgåendeBegrunnelse());
        fag.setArt122Begrunnelse(fag000081.getArt122Begrunnelse());
        fag.setArt122NormalVirksomhetBegrunnelse(fag000081.getArt122NormalVirksomhetBegrunnelse());
        fag.setArt161AnmodningBegrunnelse(fag000081.getArt161AnmodningBegrunnelse());
        fag.setArt161AvslagBegrunnelse(fag000081.getArt161AvslagBegrunnelse());
        fag.setBegrunnelseFritekst(fag000081.getBegrunnelseFritekst());
        fag.setForetakNavn(fag000081.getForetakNavn());
        fag.setFtrl213AvslagBegrunnelse(fag000081.getFtrl213AvslagBegrunnelse());
        fag.setInngangsvilkårBegrunnelse(fag000081.getInngangsvilkårBegrunnelse());
        fag.setLovvalgsperiode(tilLovvalgsperiodeType000084(fag000081.getLovvalgsperiode()));
        fag.setYrkesaktivitet(fag000081.getYrkesaktivitet());

        return fag;
    }

    private static LovvalgsperiodeType tilLovvalgsperiodeType000084(no.nav.dok.melosysbrev._000081.LovvalgsperiodeType lovvalgsperiodeType000081) {
        LovvalgsperiodeType lovvalgsperiodeType = new LovvalgsperiodeType();
        lovvalgsperiodeType.setFomDato(lovvalgsperiodeType000081.getFomDato());
        lovvalgsperiodeType.setTomDato(lovvalgsperiodeType000081.getTomDato());
        lovvalgsperiodeType.setUnntakFraLovvalgsland(lovvalgsperiodeType000081.getUnntakFraLovvalgsland());
        return lovvalgsperiodeType;
    }

    @Override
    no.nav.dok.melosysbrev._000081.LovvalgsperiodeType lagLovvalgsperiodeType(Behandlingsresultat resultat) {
        Anmodningsperiode anmodningsperiode = resultat.hentValidertAnmodningsperiode();

        no.nav.dok.melosysbrev._000081.LovvalgsperiodeType lovvalgsperiodeType = new no.nav.dok.melosysbrev._000081.LovvalgsperiodeType();
        lovvalgsperiodeType.setUnntakFraLovvalgsland(anmodningsperiode.getUnntakFraLovvalgsland().getBeskrivelse());

        try {
            lovvalgsperiodeType.setFomDato(convertToXMLGregorianCalendarRemoveTimezone(anmodningsperiode.getFom()));
            lovvalgsperiodeType.setTomDato(convertToXMLGregorianCalendarRemoveTimezone(anmodningsperiode.getTom()));
        } catch (DatatypeConfigurationException e) {
            log.error("", e);
        }
        return lovvalgsperiodeType;
    }
}
