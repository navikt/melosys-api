package no.nav.melosys.service.dokument.brev.mapper;

import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.service.dokument.brev.BrevDataVideresend;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagFellesType;
import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles;
import static org.assertj.core.api.Assertions.assertThat;

public class VideresendSoknadMapperTest {

    private final VideresendSoknadMapper instans;

    public VideresendSoknadMapperTest() {
        instans = new VideresendSoknadMapper();
    }

    @Test
    public void mapTilBrevXML() throws JAXBException, SAXException {
        FellesType fellesType = lagFellesType();
        MelosysNAVFelles navFelles = lagNAVFelles();

        BrevDataVideresend brevdata = lagBrevDataVideresend();
        String resultat = instans.mapTilBrevXML(fellesType, navFelles, new Behandling(), new Behandlingsresultat(), brevdata, false);
        assertThat(resultat).matches("(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\n.*");
    }

    private BrevDataVideresend lagBrevDataVideresend() {
        BrevDataVideresend brevDataVideresend = new BrevDataVideresend(new BrevbestillingRequest(), "Saksbehandler");
        brevDataVideresend.bostedsland = Landkoder.NO.getBeskrivelse();

        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.navn = "Försäkringskassan";
        utenlandskMyndighet.gateadresse = "Box 1164";
        utenlandskMyndighet.postnummer = "SE-621 22";
        utenlandskMyndighet.poststed = "Visby";
        utenlandskMyndighet.land = "Sverige";
        utenlandskMyndighet.landkode = Landkoder.SE;
        brevDataVideresend.trygdemyndighet = utenlandskMyndighet;
        return brevDataVideresend;
    }
}
