package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Utpekingsperiode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.service.dokument.brev.BrevDataUtpekingAnnetLand;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagFellesType;
import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles;
import static org.assertj.core.api.Assertions.assertThat;


public class UtpekingAnnetLandMapperTest {
    private UtpekingAnnetLandMapper utpekingAnnetLandMapper;

    @Before
    public void setUp() throws Exception {
        utpekingAnnetLandMapper = new UtpekingAnnetLandMapper();
    }

    @Test
    public void mapTilBrevXML() throws JAXBException, SAXException {
        FellesType fellesType = lagFellesType();
        MelosysNAVFelles navFelles = lagNAVFelles();
        BrevDataUtpekingAnnetLand brevDataUtpekingAnnetLand = lagDataUtpekingAnnetLand();
        final String brevXML = utpekingAnnetLandMapper.mapTilBrevXML(fellesType, navFelles,
            new Behandling(), new Behandlingsresultat(), brevDataUtpekingAnnetLand);
        assertThat(brevXML).contains(Landkoder.EE.getBeskrivelse());
        assertThat(brevXML).contains(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3.getKode());
    }

    private BrevDataUtpekingAnnetLand lagDataUtpekingAnnetLand() {
        BrevDataUtpekingAnnetLand brevDataUtpekingAnnetLand = new BrevDataUtpekingAnnetLand(new BrevbestillingDto(), "Saksbehandler");
        brevDataUtpekingAnnetLand.utpekingsperiode = new Utpekingsperiode(LocalDate.now(), null, Landkoder.EE,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_3, null);
        return brevDataUtpekingAnnetLand;
    }
}