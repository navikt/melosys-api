package no.nav.melosys.integrasjon.sakogbehandling;

import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingAvsluttet;
import no.nav.melding.virksomhet.behandlingsstatus.hendelsehandterer.v1.hendelseshandtererbehandlingsstatus.BehandlingOpprettet;

public interface SakOgBehandlingClient {

    void sendBehandlingOpprettet(BehandlingOpprettet behandlingOpprettet);

    void sendBehandlingAvsluttet(BehandlingAvsluttet behandlingAvsluttet);
}
