package no.nav.melosys.integrasjon.sakogbehandling;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingStatusMapper;

public interface SakOgBehandlingFasade {

    void sendBehandlingOpprettet(BehandlingStatusMapper mapper);

    void sendBehandlingAvsluttet(BehandlingStatusMapper mapper);

    Saksopplysning finnSakOgBehandlingskjedeListe(String aktørId);
}
