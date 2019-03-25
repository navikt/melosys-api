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
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataVedlegg;
import org.xml.sax.SAXException;

public class AttestMapper implements BrevDataMapper {

    private static final String XSD_LOCATION = "melosysbrev/melosys_000116.xsd";

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevData) throws JAXBException, SAXException, TekniskException {
        BrevDataVedlegg brevDataVedlegg = (BrevDataVedlegg) brevData;
        Objects.requireNonNull(brevDataVedlegg, "Attestmapper trenger brevdata av type BrevDataVedlegg");

        VedleggMapper vedleggMapper = new VedleggMapper(behandling, resultat);
        vedleggMapper.map(brevDataVedlegg);

        Fag fag = mapFag(brevDataVedlegg);
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag, vedleggMapper.hent());
        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    public Fag mapFag(BrevDataVedlegg vedlegg) {
        Fag fag = new Fag();
        if (vedlegg.brevDataA1 != null) {
            fag.setVedleggA1("true");
        }
        if (vedlegg.brevDataA001 != null) {
            fag.setVedleggSEDA001("true");
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