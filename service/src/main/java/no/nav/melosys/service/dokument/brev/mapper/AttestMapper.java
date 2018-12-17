package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Objects;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev._000116.BrevdataType;
import no.nav.dok.melosysbrev._000116.Fag;
import no.nav.dok.melosysbrev._000116.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.dok.melosysbrev.felles.melosys_vedlegg.VedleggType;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevDataVedlegg;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA001;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import org.xml.sax.SAXException;

public class AttestMapper implements BrevDataMapper {

    private static final String XSD_LOCATION = "melosysbrev/melosys_000116.xsd";
    private BrevDataA001 brevDataA001;
    private BrevDataA1 brevDataA1;

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevData) throws JAXBException, SAXException, TekniskException {
        BrevDataVedlegg brevDataVedlegg = (BrevDataVedlegg) brevData;
        Objects.requireNonNull(brevDataVedlegg, "Attestmapper trenger brevdata av type BrevDataVedlegg");

        brevDataA1 = brevDataVedlegg.brevDataA1;
        brevDataA001 = brevDataVedlegg.brevDataA001;

        VedleggMapper vedleggMapper = new VedleggMapper(behandling, resultat);
        vedleggMapper.map(brevDataA1, brevDataA001);

        Fag fag = mapFag();
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag, vedleggMapper.hent());
        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    public Fag mapFag() {
        Fag fag = new Fag();
        if (brevDataA1 != null) {
            fag.setVedleggA1("true");
        }
        if (brevDataA001 != null) {
            fag.setVedleggA1("true");
        }
        return fag;
    }

    private JAXBElement<BrevdataType> mapintoBrevdataType(FellesType fellesType, MelosysNAVFelles navFelles, Fag fag, VedleggType vedlegg) {
        ObjectFactory factory = new ObjectFactory();
        BrevdataType brevdataType = factory.createBrevdataType();
        brevdataType.setFelles(fellesType);
        brevdataType.setNAVFelles(navFelles);
        brevdataType.setFag(fag);
        brevdataType.setVedlegg(vedlegg);
        return factory.createBrevdata(brevdataType);
    }

}