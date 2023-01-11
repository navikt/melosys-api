package no.nav.melosys.service.dokument.brev.mapper;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.melosysbrev._000168.BrevdataType;
import no.nav.dok.melosysbrev._000168.Fag;
import no.nav.dok.melosysbrev._000168.LovvalgsperiodeType;
import no.nav.dok.melosysbrev._000168.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.LovvalgsbestemmelseKode;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.dok.melosysbrev.felles.melosys_felles.TilleggsbestemmelseKode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Utpekingsperiode;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataUtpekingAnnetLand;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;

public final class UtpekingAnnetLandMapper implements BrevDataMapper {
    private static final String XSD_LOCATION = "melosysbrev/melosys_000168.xsd";

    @Override
    public String mapTilBrevXML(FellesType fellesType,
                                MelosysNAVFelles navFelles,
                                Behandling behandling,
                                Behandlingsresultat resultat,
                                BrevData brevData) throws JAXBException, SAXException {
        Fag fag = mapFag((BrevDataUtpekingAnnetLand)brevData);
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidate(brevdataTypeJAXBElement, XSD_LOCATION);
    }

    Fag mapFag(BrevDataUtpekingAnnetLand brevDataUtpekingAnnetLand) {
        Fag fag = new Fag();
        Utpekingsperiode utpekingsperiode = brevDataUtpekingAnnetLand.utpekingsperiode;

        fag.setLovvalgsland(utpekingsperiode.getLovvalgsland().getBeskrivelse());
        fag.setLovvalgsperiode(lagLovvalgsperiodeType(utpekingsperiode));
        fag.setLovvalgsbestemmelse(LovvalgsbestemmelseKode.fromValue(utpekingsperiode.getBestemmelse().getKode()));
        if (utpekingsperiode.getTilleggsbestemmelse() != null) {
            fag.setTilleggsbestemmelse(TilleggsbestemmelseKode.fromValue(utpekingsperiode.getTilleggsbestemmelse().getKode()));
        }
        fag.setFritekst(brevDataUtpekingAnnetLand.fritekst);

        return fag;
    }

    private static LovvalgsperiodeType lagLovvalgsperiodeType(Utpekingsperiode utpekingsperiode) {
        LovvalgsperiodeType lovvalgsperiodeType = new LovvalgsperiodeType();
        try {
            lovvalgsperiodeType.setFomDato(convertToXMLGregorianCalendarRemoveTimezone(utpekingsperiode.getFom()));
            lovvalgsperiodeType.setTomDato(convertToXMLGregorianCalendarRemoveTimezone(utpekingsperiode.getTom()));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException(e);
        }
        return lovvalgsperiodeType;
    }

    @SuppressWarnings("Duplicates")
    private JAXBElement<BrevdataType> mapintoBrevdataType(FellesType fellesType, MelosysNAVFelles navFelles, Fag fag) {
        ObjectFactory factory = new ObjectFactory();
        BrevdataType brevdataType = factory.createBrevdataType();
        brevdataType.setFelles(fellesType);
        brevdataType.setNAVFelles(navFelles);
        brevdataType.setFag(fag);
        return factory.createBrevdata(brevdataType);
    }

}
