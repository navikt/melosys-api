package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.melosysbrev._000082.BrevdataType;
import no.nav.dok.melosysbrev._000082.Fag;
import no.nav.dok.melosysbrev._000082.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.AvsenderType;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.dok.melosysbrev.felles.melosys_felles.RolleKode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataHenleggelse;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;

public class ForvaltningsmeldingMapper implements BrevDataMapper {

    private static final String XSD_LOCATION = "melosysbrev/melosys_000082.xsd";

    // Saksbehandlingstid er 12 uker fra dato for utsendelse av brev, uavhengig av helg, helligdager, osv.
    private static final int SAKSBEHANDLINGSTID_UKER = 12;

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevData) throws JAXBException, SAXException, TekniskException {
        Fag fag = mapFag(brevData);
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    Fag mapFag(BrevData brevData) throws TekniskException {
        Fag fag = new Fag();
        try {
            BrevDataHenleggelse brevDataHenleggelse = (BrevDataHenleggelse) brevData;
            fag.setDatoMottatt(convertToXMLGregorianCalendarRemoveTimezone(brevDataHenleggelse.initierendeJournalpostForsendelseMottattTidspunkt));
            fag.setSaksbehandlingstidDato(convertToXMLGregorianCalendarRemoveTimezone(LocalDate.now().plusWeeks(SAKSBEHANDLINGSTID_UKER)));
        } catch (DatatypeConfigurationException | ClassCastException e) {
            throw new TekniskException(e);
        }
        AvsenderType avsenderType = new AvsenderType();
        avsenderType.setRolle(RolleKode.BRUKER);
        fag.setAvsender(avsenderType);
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
