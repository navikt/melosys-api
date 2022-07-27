package no.nav.melosys.integrasjon.sakogbehandling;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus.BehandlingStatusMapper;

public interface SakOgBehandlingFasade {

    Saksopplysning finnSakOgBehandlingskjedeListe(String aktørId);
}
