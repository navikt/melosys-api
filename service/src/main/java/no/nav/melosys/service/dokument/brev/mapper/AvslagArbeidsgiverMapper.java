package no.nav.melosys.service.dokument.brev.mapper;

import java.util.Set;
import javax.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;

import no.nav.dok.melosysbrev._000109.BrevdataType;
import no.nav.dok.melosysbrev._000109.Fag;
import no.nav.dok.melosysbrev._000109.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAvslagArbeidsgiver;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagLovvalgsperiodeType;
import static no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactory.mapArt121BegrunnelseType;
import static no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactory.mapArt121VesentligVirksomhetBegrunnelse;

public class AvslagArbeidsgiverMapper implements BrevDataMapper {

    private static final String XSD_LOCATION = "melosysbrev/melosys_000109.xsd";

    @Override
    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevData)
        throws JAXBException, SAXException {
        BrevDataAvslagArbeidsgiver brevDataAvslagArbeidsgiver = (BrevDataAvslagArbeidsgiver) brevData;
        Fag fag = mapFag(brevDataAvslagArbeidsgiver);

        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag);
        return JaxbHelper.marshalAndValidate(brevdataTypeJAXBElement, XSD_LOCATION);
    }

    Fag mapFag(BrevDataAvslagArbeidsgiver brevData) {
        Fag fag = new Fag();

        fag.setNavn(brevData.getPerson().getSammensattNavn());

        fag.setArbeidsland(brevData.getArbeidsland());

        fag.setArbeidsgiver(brevData.getHovedvirksomhet().navn);
        fag.setJuridiskEnhet(brevData.getHovedvirksomhet().navn);    // Skal fjernes når xsd er oppdatert

        fag.setLovvalgsperiode(lagLovvalgsperiodeType(brevData.getLovvalgsperiode()));

        Set<VilkaarBegrunnelse> art121Begrunnelser = brevData.getVilkårbegrunnelser121();
        fag.setArt121Begrunnelse(mapArt121BegrunnelseType(art121Begrunnelser));

        Set<VilkaarBegrunnelse> art121VesentligVirksomhetBegrunnelser = brevData.getVilkårbegrunnelser121VesentligVirksomhet();
        fag.setArt121VesentligVirksomhetBegrunnelse(mapArt121VesentligVirksomhetBegrunnelse(art121VesentligVirksomhetBegrunnelser));

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
