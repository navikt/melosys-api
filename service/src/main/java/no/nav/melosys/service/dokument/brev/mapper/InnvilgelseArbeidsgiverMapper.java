package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Set;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev._000127.BrevdataType;
import no.nav.dok.melosysbrev._000127.Fag;
import no.nav.dok.melosysbrev._000127.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.service.dokument.brev.BrevData;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.*;

public class InnvilgelseArbeidsgiverMapper implements BrevDataMapper {

    private static final String XSD_LOCATION = "melosysbrev/melosys_000127.xsd";

    @Override
    public String mapTilBrevXML(FellesType fellesType,
                                MelosysNAVFelles navFelles,
                                Behandling behandling,
                                Behandlingsresultat resultat,
                                BrevData brevdata) throws JAXBException, SAXException {

        Fag fag = mapFag(behandling, resultat);

        JAXBElement<BrevdataType> brevdataTypeJAXBElement = lagBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    private Fag mapFag(Behandling behandling, Behandlingsresultat resultat) {
        Fag fag = new Fag();

        fag.setArbeidsland(hentArbeidsLand(behandling, resultat));
        Set<Lovvalgsperiode> perioder = resultat.getLovvalgsperioder();
        validerLovvalgsperioder(perioder);
        Lovvalgsperiode periode = perioder.iterator().next();
        fag.setLovvalgsperiode(no.nav.dok.melosysbrev.felles.melosys_felles.LovvalgsperiodeType.builder()
            .withFomDato(lagXmlDato(periode.getFom()))
            .withTomDato(lagXmlDato(periode.getTom()))
            .build());

        fag.setNavn(hentSammensattNavn(behandling));
        return fag;
    }

    private static JAXBElement<BrevdataType> lagBrevdataType(FellesType fellesType,
                                                             MelosysNAVFelles navFelles,
                                                             Fag fag) {
        ObjectFactory factory = new ObjectFactory();
        BrevdataType brevdataType = BrevdataType.builder()
            .withFelles(fellesType)
            .withNAVFelles(navFelles)
            .withFag(fag)
            .build();
        return factory.createBrevdata(brevdataType);
    }
}