package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev._000082.BrevdataType;
import no.nav.dok.melosysbrev._000082.Fag;
import no.nav.dok.melosysbrev._000082.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.AvsenderType;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.dok.melosysbrev.felles.melosys_felles.RolleKode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.brev.Saksbehandlingstid;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataMottattDato;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;

public class ForvaltningsmeldingMapper implements BrevDataMapper {

    private static final String XSD_LOCATION = "melosysbrev/melosys_000082.xsd";

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevData, boolean enableBehandleAlleSaker) throws JAXBException, SAXException {
        Fag fag = mapFag(brevData, behandling, enableBehandleAlleSaker);
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidate(brevdataTypeJAXBElement, XSD_LOCATION);
    }

    Fag mapFag(BrevData brevData, Behandling behandling, boolean enableBehandleAlleSaker) {
        Fag fag = new Fag();
        BrevDataMottattDato brevDataMottattDato = (BrevDataMottattDato) brevData;
        final Instant forsendelseMottattTidspunkt = brevDataMottattDato.initierendeJournalpostForsendelseMottattTidspunkt;
        fag.setBehandlingstype(enableBehandleAlleSaker ? BehandlingstypeKodeMapper.hentBehandlingstypeKodeAlleBehandlinger(behandling) : BehandlingstypeKodeMapper.hentBehandlingstypeKode(behandling));
        fag.setDatoMottatt(convertToXMLGregorianCalendarRemoveTimezone(forsendelseMottattTidspunkt));
        fag.setSaksbehandlingstidDato(convertToXMLGregorianCalendarRemoveTimezone(Saksbehandlingstid.beregnSaksbehandlingsfrist(forsendelseMottattTidspunkt)));

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
