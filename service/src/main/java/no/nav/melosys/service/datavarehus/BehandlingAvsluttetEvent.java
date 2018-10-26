package no.nav.melosys.service.datavarehus;

import no.nav.melosys.domain.Behandling;

public class BehandlingAvsluttetEvent {

    public Behandling behandling;

    public String endretAv;

    public BehandlingAvsluttetEvent(Behandling behandling, String endretAv) {
        this.behandling = behandling;
        this.endretAv = endretAv;
    }
}
