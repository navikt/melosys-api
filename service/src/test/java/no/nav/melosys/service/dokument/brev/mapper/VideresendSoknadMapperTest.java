package no.nav.melosys.service.dokument.brev.mapper;

import jakarta.xml.bind.JAXBException;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestBuilder;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.service.dokument.brev.BrevDataVideresend;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagFellesType;
import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles;
import static org.assertj.core.api.Assertions.assertThat;

class VideresendSoknadMapperTest {

    private final VideresendSoknadMapper instans;

    public VideresendSoknadMapperTest() {
        instans = new VideresendSoknadMapper();
    }

    @Test
    void mapTilBrevXML() throws JAXBException, SAXException {
        FellesType fellesType = lagFellesType();
        MelosysNAVFelles navFelles = lagNAVFelles();

        BrevDataVideresend brevdata = lagBrevDataVideresend();
        String resultat = instans.mapTilBrevXML(fellesType, navFelles, BehandlingTestBuilder.builderWithDefaults().build(), new Behandlingsresultat(), brevdata);
        assertThat(resultat).matches("(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\n.*");
    }

    private BrevDataVideresend lagBrevDataVideresend() {
        BrevDataVideresend brevDataVideresend = new BrevDataVideresend(new BrevbestillingDto(), "Saksbehandler");
        brevDataVideresend.setBostedsland(Landkoder.NO.getBeskrivelse());

        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.setNavn("Försäkringskassan");
        utenlandskMyndighet.setGateadresse1("Box 1164");
        utenlandskMyndighet.setPostnummer("SE-621 22");
        utenlandskMyndighet.setPoststed("Visby");
        utenlandskMyndighet.setLand("Sverige");
        utenlandskMyndighet.setLandkode(Land_iso2.SE);
        brevDataVideresend.setTrygdemyndighet(utenlandskMyndighet);
        return brevDataVideresend;
    }
}
