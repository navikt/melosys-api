package no.nav.melosys.service.dokument.brev.mapper;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev._000125.BrevdataType;
import no.nav.dok.melosysbrev._000125.Fag;
import no.nav.dok.melosysbrev._000125.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.service.dokument.brev.BrevData;
import org.xml.sax.SAXException;

public class AvslagManglendeOpplysningerMapper implements BrevDataMapper {

    private static final String XSD_LOCATION = "melosysbrev/melosys_000125.xsd";

    @Override
    public String mapTilBrevXML(FellesType fellesType,
                                MelosysNAVFelles navFelles,
                                Behandling behandling,
                                Behandlingsresultat resultat,
                                BrevData brevData) throws JAXBException, SAXException {
        Fag fag = new Fag();
        fag.setFritekst(brevData.fritekst);

        JAXBElement<BrevdataType> brevdataTypeJAXBElement = lagBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidate(brevdataTypeJAXBElement, XSD_LOCATION);
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