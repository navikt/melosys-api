package no.nav.melosys.integrasjon.sakogbehandling.behandlingskjede;

import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.SakOgBehandlingV1;
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.FinnSakOgBehandlingskjedeListeRequest;
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.FinnSakOgBehandlingskjedeListeResponse;

public class BehandlingskjedeConsumerImpl implements BehandlingskjedeConsumer {

    private SakOgBehandlingV1 port;

    public BehandlingskjedeConsumerImpl(SakOgBehandlingV1 port) {
        this.port = port;
    }

    @Override
    public FinnSakOgBehandlingskjedeListeResponse finnSakOgBehandlingskjedeListeResponse(FinnSakOgBehandlingskjedeListeRequest request) {
        return port.finnSakOgBehandlingskjedeListe(request);
    }
}
