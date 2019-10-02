package no.nav.melosys.service.dokument.brev.mapper;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev._000146.BrevdataType;
import no.nav.dok.melosysbrev._000146.Fag;
import no.nav.dok.melosysbrev._000146.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataVideresend;
import org.xml.sax.SAXException;

public class VideresendSoknadMapper implements BrevDataMapper {
    private static final String XSD_LOCATION = "melosysbrev/melosys_000146.xsd";

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevData) throws JAXBException, SAXException {
        BrevDataVideresend brevDataVideresend = (BrevDataVideresend) brevData;
        Fag fag = new Fag();
        fag.setBostedsland(brevDataVideresend.bostedsland);
        fag.setTrygdemyndighet(brevDataVideresend.trygdemyndighetsland);

        JAXBElement<BrevdataType> brevdataTypeJAXBElement = lagBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    private static JAXBElement<BrevdataType> lagBrevdataType(FellesType fellesType, MelosysNAVFelles navFelles, Fag fag) {
        BrevdataType brevdataType = BrevdataType.builder()
            .withFelles(fellesType)
            .withNAVFelles(navFelles)
            .withFag(fag)
            .build();
        return new ObjectFactory().createBrevdata(brevdataType);
    }
}