package no.nav.melosys.integrasjon.gsak.behandlesak;

import no.nav.tjeneste.virksomhet.behandlesak.v1.binding.BehandleSakV1;
import no.nav.tjeneste.virksomhet.behandlesak.v1.binding.OpprettSakSakEksistererAllerede;
import no.nav.tjeneste.virksomhet.behandlesak.v1.binding.OpprettSakUgyldigInput;
import no.nav.tjeneste.virksomhet.behandlesak.v1.meldinger.OpprettSakRequest;
import no.nav.tjeneste.virksomhet.behandlesak.v1.meldinger.OpprettSakResponse;

public class BehandleSakConsumerImpl implements BehandleSakConsumer {
    private BehandleSakV1 port;

    public BehandleSakConsumerImpl(BehandleSakV1 port) {
        this.port = port;
    }

    @Override
    public OpprettSakResponse opprettSak(OpprettSakRequest request) throws OpprettSakSakEksistererAllerede, OpprettSakUgyldigInput {
        return port.opprettSak(request);
    }
}
