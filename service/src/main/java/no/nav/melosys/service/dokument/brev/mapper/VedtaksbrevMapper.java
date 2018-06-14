package no.nav.melosys.service.dokument.brev.mapper;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.nav.foreldrepenger.integrasjon.dokument.felles.FellesType;
import no.nav.foreldrepenger.integrasjon.dokument.innvilget.*;
import no.nav.melosys.dokument.innvilget.InnvilgetConstants;
import no.nav.melosys.domain.Behandling;
import org.xml.sax.SAXException;

//FIXME Slettes. Den klassen er bare et eksempel på hvordan FP mapper et vedtak
public class VedtaksbrevMapper implements BrevDataMapper {

    public VedtaksbrevMapper() {
    }

    @Override
    public String mapTilBrevXML(FellesType fellesType, Behandling behandling) throws JAXBException, SAXException {
        FagType fagType = mapFagType(behandling);
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, fagType);
        String brevXmlMedNamespace = JaxbHelper.marshalAndValidateJaxb(InnvilgetConstants.JAXB_CLASS, brevdataTypeJAXBElement, InnvilgetConstants.XSD_LOCATION);
        return MapperUtils.fjernNamespaceFra(brevXmlMedNamespace);
    }

    private FagType mapFagType(Behandling behandling) {
        final FagType fagType = new FagType();

        BehandlingsresultatType behandlingsresultatType = new BehandlingsresultatType();
        behandlingsresultatType.setBelop(0.1f);
        fagType.setBehandlingsresultat(behandlingsresultatType);

        String behandlingsType = behandling.getType().getBeskrivelse();
        BehandlingsTypeType behandlingsTypeType = fra(behandlingsType);
        fagType.setBehandlingsType(behandlingsTypeType);

        fagType.setKlageFristUker(2);
        fagType.setSokersNavn("OLA");
        fagType.setPersonstatus(PersonstatusKodeType.fromValue(PersonstatusKodeType.ANNET.value()));

        return fagType;
    }

    private BehandlingsTypeType fra(String behandlingsType) {
        if ("REVURDERING".equals(behandlingsType)) {
            return BehandlingsTypeType.REVURDERING;
        }
        if ("MEDHOLD".equals(behandlingsType)) {
            return BehandlingsTypeType.MEDHOLD;
        }
        return BehandlingsTypeType.FOERSTEGANGSBEHANDLING;
    }

    private JAXBElement<BrevdataType> mapintoBrevdataType(FellesType fellesType, FagType fagType) {
        ObjectFactory factory = new ObjectFactory();
        BrevdataType brevdataType = factory.createBrevdataType();
        brevdataType.setFag(fagType);
        brevdataType.setFelles(fellesType);
        return factory.createBrevdata(brevdataType);
    }
}

