package no.nav.melosys.service.behandling;

import java.util.Set;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

public class BehandlingsKombinasjon {
    Set<Behandlingstema> behandlingsTemaer;
    Set<Behandlingstyper> behandlingsTyper;

    public BehandlingsKombinasjon(Set<Behandlingstema> behandlingsTemaer, Set<Behandlingstyper> behandlingsTyper) {
        this.behandlingsTemaer = behandlingsTemaer;
        this.behandlingsTyper = behandlingsTyper;
    }

    public Set<Behandlingstema> getBehandlingsTemaer() {
        return behandlingsTemaer;
    }

    public void setBehandlingsTemaer(Set<Behandlingstema> behandlingsTemaer) {
        this.behandlingsTemaer = behandlingsTemaer;
    }

    public Set<Behandlingstyper> getBehandlingsTyper() {
        return behandlingsTyper;
    }

    public void setBehandlingsTyper(Set<Behandlingstyper> behandlingsTyper) {
        this.behandlingsTyper = behandlingsTyper;
    }
}
