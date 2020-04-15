package no.nav.melosys.tjenester.gui.dto.journalforing;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;

public class BehandlingsInformasjon {

    private Behandlingstema behandlingstema;

    public BehandlingsInformasjon(Behandlingstema behandlingstema) {
        this.behandlingstema = behandlingstema;
    }

    public Behandlingstema getBehandlingstema() {
        return behandlingstema;
    }
}
