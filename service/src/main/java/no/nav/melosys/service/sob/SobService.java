package no.nav.melosys.service.sob;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.TemaFactory;
import no.nav.melosys.integrasjon.sakogbehandling.SakOgBehandlingFasade;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingStatusMapper;
import no.nav.melosys.service.behandling.BehandlingService;
import org.springframework.stereotype.Service;

@Service
public class SobService {
    private final SakOgBehandlingFasade sakOgBehandlingFasade;

    public SobService(SakOgBehandlingFasade sakOgBehandlingFasade) {
        this.sakOgBehandlingFasade = sakOgBehandlingFasade;
    }

    public Saksopplysning finnSakOgBehandlingskjedeListe(String aktørID) {
        return sakOgBehandlingFasade.finnSakOgBehandlingskjedeListe(aktørID);
    }

}
