package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.melosysbrev._000082.*;
import no.nav.dok.melosysbrev._000082.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.TekniskException;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.convertToXMLGregorianCalendarRemoveTimezone;

public class ForvaltningsmeldingMapper implements BrevDataMapper {

    private static final String XSD_LOCATION = "xsd/melosys_000082.xsd";

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling) throws JAXBException, SAXException {
        Fag fag = mapFag(behandling);
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag);
        String brevXmlMedNamespace = JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
        return MapperUtils.fjernNamespaceFra(brevXmlMedNamespace);
    }

    public Fag mapFag(Behandling behandling) {
        final Fag fag = new Fag();
        try {
            fag.setDatoMottatt(convertToXMLGregorianCalendarRemoveTimezone(behandling.getRegistrertDato()));
            // Saksbehandlingstid er 12 uker fra dato for utsendelse av brev, uavhengig av helg, helligdager, osv.
            fag.setSaksbehandlingstidDato(convertToXMLGregorianCalendarRemoveTimezone(LocalDate.now().plusWeeks(12)));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException("Konverteringsfeil", e);
        }
        // FIXME Avhenger av Hendelse, som ikke er modellert ennå
        AvsenderType avsenderType = new AvsenderType();
        avsenderType.setRolle(RolleKode.BRUKER);
        fag.setAvsender(avsenderType);
        return fag;
    }

    private JAXBElement<BrevdataType> mapintoBrevdataType(FellesType fellesType, MelosysNAVFelles navFelles, Fag fag) {
        ObjectFactory factory = new ObjectFactory();
        BrevdataType brevdataType = factory.createBrevdataType();
        brevdataType.setFelles(fellesType);
        brevdataType.setNAVFelles(navFelles);
        brevdataType.setFag(fag);
        return factory.createBrevdata(brevdataType);
    }

}
