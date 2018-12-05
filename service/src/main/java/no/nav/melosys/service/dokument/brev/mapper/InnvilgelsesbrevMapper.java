package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import no.nav.dok.melosysbrev._000108.*;
import no.nav.dok.melosysbrev.felles.melosys_felles.BehandlingstypeKode;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.LovvalgsbestemmelseKode;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataUtils;

import org.xml.sax.SAXException;

public final class InnvilgelsesbrevMapper implements BrevDataMapper {

    private static final String XSD_LOCATION = "melosysbrev/melosys_000108.xsd";

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevdata) throws JAXBException, SAXException, TekniskException {
        Fag fag = mapFag(behandling, brevdata);
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    private static Fag mapFag(Behandling behandling, BrevData brevdata) throws TekniskException {

        Fag fag = new Fag();
        // Aktoer arbeidsgiverAktør = behandling.getFagsak().hentAktørMedRolleType(RolleType.ARBEIDSGIVER);
        fag.setBehandlingstype(BehandlingstypeKode.valueOf(behandling.getType().getKode()));
        fag.setSakstype(SakstypeKode.valueOf(behandling.getFagsak().getType().getKode()));

        // TODO: Må avklares og hentes inn
        fag.setArbeidsgiver("ARBEIDSGIVER");
        fag.setArbeidsland("NORGE");
        fag.setLovvalgsbestemmelse(LovvalgsbestemmelseKode.FO_883_2004_ANNET);
        XMLGregorianCalendar xmlDato = lagXmlDato(LocalDate.of(2018, 12, 1));
        fag.setPeriode(PeriodeType.builder().withFomDato(xmlDato)
            .withTomDato(xmlDato)
            .build());
        fag.setTredjelandsborger("");
        fag.setTrygdemyndighetsland("vet ikke");
        return fag;
    }

    private static XMLGregorianCalendar lagXmlDato(LocalDate dato) {
        try {
            return BrevDataUtils.convertToXMLGregorianCalendarRemoveTimezone(dato);
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Kan ikke lage DatatypeConverterFactory.", e);
        }
    }

    private static JAXBElement<BrevdataType> mapintoBrevdataType(FellesType fellesType, MelosysNAVFelles navFelles, Fag fag) {
        ObjectFactory factory = new ObjectFactory();
        BrevdataType brevdataType = BrevdataType.builder()
            .withFelles(fellesType)
            .withNAVFelles(navFelles)
            .withFag(fag)
            .build();
        return factory.createBrevdata(brevdataType);
    }

}
