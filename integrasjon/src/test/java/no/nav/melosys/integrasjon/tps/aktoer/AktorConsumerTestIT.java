package no.nav.melosys.integrasjon.tps.aktoer;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import no.nav.melosys.integrasjon.test.Gen3WsProxyServiceITBase;
import no.nav.melosys.integrasjon.test.TpsTestData;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentAktoerIdForIdentPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.HentIdentForAktoerIdPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentAktoerIdForIdentResponse;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdRequest;
import no.nav.tjeneste.virksomhet.aktoer.v2.meldinger.HentIdentForAktoerIdResponse;

@RunWith(SpringRunner.class)
public class AktorConsumerTestIT extends Gen3WsProxyServiceITBase {

    @Autowired
    AktorConsumerConfig config;

    AktorConsumer aktørProxyService;

    AktorSelftestConsumer selftestConsumer;

    public AktorConsumerTestIT() {
    }

    @Before
    public void setup() throws Exception {
        AktorConsumerProducer producer = new AktorConsumerProducer();
        producer.setConfig(config);

        aktørProxyService = producer.aktorConsumer();
        selftestConsumer = producer.aktorSelftestConsumer();
    }

    @Test
    public void test_ping() {
        selftestConsumer.ping();
    }

    @Test
    public void test_hentIdentForAktoerId() throws HentIdentForAktoerIdPersonIkkeFunnet {
        HentIdentForAktoerIdRequest request = new HentIdentForAktoerIdRequest();
        request.setAktoerId(""+TpsTestData.STD_AKTØR_ID);
        HentIdentForAktoerIdResponse response = aktørProxyService.hentIdentForAktoerId(request);
        assertNotNull(response.getIdent());
        System.out.println(TpsTestData.STD_AKTØR_ID + " -> " + response.getIdent());
    }

    @Test
    public void test_hentAktørIdForIdent() throws HentAktoerIdForIdentPersonIkkeFunnet {
        HentAktoerIdForIdentRequest request = new HentAktoerIdForIdentRequest();
        request.setIdent(TpsTestData.STD_FNR);
        HentAktoerIdForIdentResponse response = aktørProxyService.hentAktørIdForIdent(request);
        assertNotNull(response.getAktoerId());
        System.out.println(TpsTestData.STD_FNR + " -> " + response.getAktoerId());
    }

}