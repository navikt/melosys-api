package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;

import no.nav.dok.melosysbrev._000074.*;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevDataDto;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.convertToXMLGregorianCalendarRemoveTimezone;

public class MangelbrevMapper implements BrevDataMapper {

    private static final String XSD_LOCATION = "xsd/melosys_000074.xsd";

    // Frist er 4 uker fra dato for utsendelse av brev, uavhengig av helg, helligdager, osv.
    private static final int FRIST_UKER = 4;

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, BrevDataDto brevDataDto) throws JAXBException, SAXException, TekniskException {
        Fag fag = mapFag(behandling, brevDataDto);
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    public Fag mapFag(Behandling behandling, BrevDataDto brevDataDto) throws TekniskException {
        if (brevDataDto.fritekst == null) {
            throw new IntegrasjonException("Mangelbrev mangler informasjon om manglende opplysninger.");
        }
        Fag fag = new Fag();
        ManglendeOpplysningerType manglendeOpplysningerType = new ManglendeOpplysningerType();
        manglendeOpplysningerType.setManglendeOpplysningerFritekst(brevDataDto.fritekst);
        try {
            fag.setDatoMottatt(convertToXMLGregorianCalendarRemoveTimezone(behandling.getRegistrertDato()));
            manglendeOpplysningerType.setFristDato(convertToXMLGregorianCalendarRemoveTimezone(LocalDate.now().plusWeeks(FRIST_UKER)));
        } catch (DatatypeConfigurationException e) {
            throw new TekniskException("Konverteringsfeil", e);
        }
        fag.setManglendeOpplysninger(manglendeOpplysningerType);
        // FIXME: Kan ikke utledes for behandling som ikke initieres av SED, og må registreres i journalføringen
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
