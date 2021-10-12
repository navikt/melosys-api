package no.nav.melosys.service.dokument.brev.mapper;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev._000074.BrevdataType;
import no.nav.dok.melosysbrev._000074.Fag;
import no.nav.dok.melosysbrev._000074.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.service.dokument.brev.BrevData;
import org.xml.sax.SAXException;

public class AttestStorbritanniaMapper implements BrevDataMapper {

    private static final String XSD_LOCATION = "melosysbrev/melosys_000XXX.xsd"; // TODO: Oppdater med riktig etter bumping av dokgen

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevData) throws JAXBException, SAXException {
        Fag fag = mapFag(brevData, behandling);
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidate(brevdataTypeJAXBElement, XSD_LOCATION);
    }

    public Fag mapFag(BrevData brevData, Behandling behandling) {
        Fag fag = new Fag();
        // TODO: gjør mapping
        return fag;
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
