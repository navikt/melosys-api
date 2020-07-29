package no.nav.melosys.service.dokument.brev.mapper;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev._000127.BrevdataType;
import no.nav.dok.melosysbrev._000127.Fag;
import no.nav.dok.melosysbrev._000127.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.LovvalgsperiodeType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.mapper.felles.BrevMapperUtils.lagXmlDato;

public class InnvilgelseArbeidsgiverMapper implements BrevDataMapper {

    private static final String XSD_LOCATION = "melosysbrev/melosys_000127.xsd";

    @Override
    public String mapTilBrevXML(FellesType fellesType,
                                MelosysNAVFelles navFelles,
                                Behandling behandling,
                                Behandlingsresultat resultat,
                                BrevData brevdata) throws JAXBException, SAXException {

        BrevDataInnvilgelse brevDataInnvilgelse = (BrevDataInnvilgelse) brevdata;
        Fag fag = mapFag(behandling, brevDataInnvilgelse);

        JAXBElement<BrevdataType> brevdataTypeJAXBElement = lagBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidate(brevdataTypeJAXBElement, XSD_LOCATION);
    }

    private Fag mapFag(Behandling behandling, BrevDataInnvilgelse brevDataInnvilgelse) {
        Fag fag = new Fag();

        fag.setArbeidsland(brevDataInnvilgelse.arbeidsland);
        fag.setNavn(brevDataInnvilgelse.personNavn);
        fag.setArbeidsgiver(brevDataInnvilgelse.hovedvirksomhet.navn);

        Lovvalgsperiode periode = brevDataInnvilgelse.lovvalgsperiode;
        fag.setLovvalgsperiode(LovvalgsperiodeType.builder()
            .withFomDato(lagXmlDato(periode.getFom()))
            .withTomDato(lagXmlDato(periode.getTom()))
            .build());

        final String sammensattNavn = behandling.finnPersonDokument().map(p -> p.sammensattNavn)
            .orElseThrow(() -> new IllegalStateException("Persondokument finnes ikke for behandling " + behandling.getId()));

        fag.setNavn(sammensattNavn);
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