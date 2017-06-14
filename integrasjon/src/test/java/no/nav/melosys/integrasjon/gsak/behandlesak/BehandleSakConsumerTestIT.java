package no.nav.melosys.integrasjon.gsak.behandlesak;

import static junit.framework.TestCase.assertNotNull;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import no.nav.melosys.integrasjon.test.Gen3WsProxyServiceITBase;
import no.nav.melosys.integrasjon.test.TpsTestData;
import no.nav.tjeneste.virksomhet.behandlesak.v1.binding.OpprettSakSakEksistererAllerede;
import no.nav.tjeneste.virksomhet.behandlesak.v1.binding.OpprettSakUgyldigInput;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.Aktoer;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.Fagomraader;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.Fagsystemer;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.Person;
import no.nav.tjeneste.virksomhet.behandlesak.v1.informasjon.Sak;
import no.nav.tjeneste.virksomhet.behandlesak.v1.meldinger.OpprettSakRequest;
import no.nav.tjeneste.virksomhet.behandlesak.v1.meldinger.OpprettSakResponse;

@Ignore
@RunWith(SpringRunner.class)
public class BehandleSakConsumerTestIT extends Gen3WsProxyServiceITBase {
    public static final String FAGOMRÅDE_KODE = "MED"; // TODO FA Hva er fagområdekoden?
    public static final String FAGSYSTEM_KODE = "FS22"; // fagsystemkode FS22 (Gosys)

    private BehandleSakConsumer behandleSakConsumer;

    @Autowired
    private BehandleSakConsumerConfig behandleSakConfig;

    @Before
    public void setup() throws Exception {
        BehandleSakConsumerProducer behandleSakConsumerProducer = new BehandleSakConsumerProducer();
        behandleSakConsumerProducer.setConfig(behandleSakConfig);
        behandleSakConsumer = behandleSakConsumerProducer.behandleSakConsumer();
    }

    @Test
    public void test_opprettSak() throws OpprettSakUgyldigInput, OpprettSakSakEksistererAllerede {
        OpprettSakRequest request = new OpprettSakRequest();
        Sak sak = new Sak();

        Fagomraader fagomraader = new Fagomraader();
        fagomraader.setValue(FAGOMRÅDE_KODE);
        sak.setFagomraade(fagomraader);

        Aktoer aktoer = new Person();
        aktoer.setIdent(String.valueOf(TpsTestData.STD_KVINNE_AKTØR_ID));
        sak.getGjelderBrukerListe().add(aktoer);

        Fagsystemer fagsystemer = new Fagsystemer();
        fagsystemer.setValue(FAGSYSTEM_KODE);
        sak.setFagsystem(fagsystemer);

        request.setSak(sak);
        OpprettSakResponse response = behandleSakConsumer.opprettSak(request);

        assertNotNull(response.getSakId());
    }

}
