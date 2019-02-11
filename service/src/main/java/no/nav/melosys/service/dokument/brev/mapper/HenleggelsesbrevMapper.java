package no.nav.melosys.service.dokument.brev.mapper;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.dok.melosysbrev._000072.*;
import no.nav.dok.melosysbrev.felles.melosys_felles.AvsenderType;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.dok.melosysbrev.felles.melosys_felles.RolleKode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataHenleggelse;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.convertToXMLGregorianCalendarRemoveTimezone;

public class HenleggelsesbrevMapper implements BrevDataMapper {

    private static final String XSD_LOCATION = "melosysbrev/melosys_000072.xsd";

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevData) throws JAXBException, SAXException {
        Fag fag = mapFag(brevData);
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = lagBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    private static Fag mapFag(BrevData brevData) {
        BrevDataHenleggelse brevDataHenleggelse = (BrevDataHenleggelse) brevData;
        XMLGregorianCalendar datoMottatt = convertToXMLGregorianCalendarRemoveTimezone(brevDataHenleggelse.initierendeJournalpostForsendelseMottattTidspunkt);
        HenleggelseGrunnType henleggelseGrunn = HenleggelseGrunnType.builder()
            .withHenleggelseGrunn(HenleggelseGrunnKode.fromValue(brevData.begrunnelseKode))
            .withFritekstBegrunnelse(brevData.fritekst)
            .build();
        return Fag.builder()
            .withAvsender(AvsenderType.builder()
                .withRolle(RolleKode.BRUKER)
                .build())
            .withDatoMottatt(datoMottatt)
            .withHenleggelseGrunn(henleggelseGrunn)
            .build();
    }

    private static JAXBElement<BrevdataType> lagBrevdataType(FellesType fellesType, MelosysNAVFelles navFelles, Fag fag) {
        BrevdataType brevdataType = BrevdataType.builder()
            .withFag(fag)
            .withFelles(fellesType)
            .withNAVFelles(navFelles)
            .build();
        return new ObjectFactory().createBrevdata(brevdataType);
    }

}
