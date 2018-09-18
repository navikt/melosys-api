package no.nav.melosys.service.dokument.brev.mapper;

import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import org.xml.sax.SAXException;

public interface BrevDataMapper {

    String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling) throws JAXBException, SAXException;
}
