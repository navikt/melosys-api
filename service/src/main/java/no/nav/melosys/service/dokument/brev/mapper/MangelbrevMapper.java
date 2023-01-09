package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.melosysbrev._000074.BrevdataType;
import no.nav.dok.melosysbrev._000074.Fag;
import no.nav.dok.melosysbrev._000074.ManglendeOpplysningerType;
import no.nav.dok.melosysbrev._000074.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.AvsenderType;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.dok.melosysbrev.felles.melosys_felles.RolleKode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataMottattDato;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone;

public class MangelbrevMapper implements BrevDataMapper {

    private static final String XSD_LOCATION = "melosysbrev/melosys_000074.xsd";

    // Frist fra dato for utsendelse av brev, uavhengig av helg, helligdager, osv.
    private static final int FRIST_UKER = 3;

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevData) throws JAXBException, SAXException {
        Fag fag = mapFag(brevData, behandling);
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidate(brevdataTypeJAXBElement, XSD_LOCATION);
    }

    public Fag mapFag(BrevData brevData, Behandling behandling) {
        if (brevData.fritekst == null) {
            throw new IntegrasjonException("Mangelbrev mangler informasjon om manglende opplysninger.");
        }
        Fag fag = new Fag();
        fag.setBehandlingstype(BehandlingstypeKodeMapper.hentBehandlingstypeKodeAlleBehandlinger(behandling));
        ManglendeOpplysningerType manglendeOpplysningerType = new ManglendeOpplysningerType();
        manglendeOpplysningerType.setManglendeOpplysningerFritekst(brevData.fritekst);
        try {
            BrevDataMottattDato brevDataMottattDato = (BrevDataMottattDato) brevData;
            fag.setDatoMottatt(convertToXMLGregorianCalendarRemoveTimezone(brevDataMottattDato.initierendeJournalpostForsendelseMottattTidspunkt));
            manglendeOpplysningerType.setFristDato(convertToXMLGregorianCalendarRemoveTimezone(LocalDate.now().plusWeeks(FRIST_UKER)));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException(e);
        }
        fag.setManglendeOpplysninger(manglendeOpplysningerType);

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
