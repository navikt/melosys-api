package no.nav.melosys.integrasjon.kodeverk;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import no.nav.tjeneste.virksomhet.kodeverk.v2.HentKodeverkHentKodeverkKodeverkIkkeFunnet;
import no.nav.tjeneste.virksomhet.kodeverk.v2.KodeverkPortType;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.HentKodeverkRequest;
import no.nav.tjeneste.virksomhet.kodeverk.v2.meldinger.HentKodeverkResponse;

@Component
public class KodeverkConsumerImpl implements KodeverkConsumer {

    @Autowired
    private KodeverkConsumerConfig config;
    
    private KodeverkPortType port;

    public KodeverkConsumerImpl() {
    }

    @PostConstruct
    private void configure() {
        this.port = config.getPort();
    }
    
    @Override
    public HentKodeverkResponse hentKodeverk(HentKodeverkRequest request) throws HentKodeverkHentKodeverkKodeverkIkkeFunnet {
        return port.hentKodeverk(request);
    }

    @Override
    public void ping() {
        port.ping();
    }

    @Override
    public String getEndpointUrl() {
        return config.getEndpointUrl();
    }

}
