package no.nav.melosys.integrasjon.sakogbehandling.behandlingskjede;

import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.FinnSakOgBehandlingskjedeListeRequest;
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.meldinger.FinnSakOgBehandlingskjedeListeResponse;

public interface BehandlingskjedeConsumer {

    FinnSakOgBehandlingskjedeListeResponse finnSakOgBehandlingskjedeListeResponse(FinnSakOgBehandlingskjedeListeRequest request);
}
