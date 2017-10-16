package no.nav.melosys.integrasjon.medl2.medlemskap;

import no.nav.melosys.integrasjon.test.Gen3WsProxyServiceITBase;
import no.nav.tjeneste.virksomhet.medlemskap.v2.PersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.medlemskap.v2.Sikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.medlemskap.v2.informasjon.Foedselsnummer;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeRequest;
import no.nav.tjeneste.virksomhet.medlemskap.v2.meldinger.HentPeriodeListeResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class MedlemskapConsumerTestIT extends Gen3WsProxyServiceITBase {

    @Autowired
    MedlemskapConsumerConfig config;

    private MedlemskapConsumer consumer;

    @Before
    public void setUp() {
        MedlemskapConsumerProducer producer = new MedlemskapConsumerProducer();
        producer.setConfig(config);

        consumer = producer.medlemskapConsumer();
    }

    @Test
    public void hentPeriodeListeTest() throws PersonIkkeFunnet, Sikkerhetsbegrensning {
        final String fnr = "77777777773";

        Foedselsnummer ident = new Foedselsnummer();
        ident.setValue(fnr);

        HentPeriodeListeRequest request = new HentPeriodeListeRequest();
        request.setIdent(ident);

        HentPeriodeListeResponse response = consumer.hentPeriodeListe(request);

        assertNotNull(response);
        assertTrue(response.getPeriodeListe().size() > 0);
    }
}
