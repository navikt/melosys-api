package no.nav.melosys.repository;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;

public class BehandlingStatistikk {
    private Behandlingstema behandlingstema;
    private long antall;

    public BehandlingStatistikk(Behandlingstema behandlingstema, long antall) {
        this.behandlingstema = behandlingstema;
        this.antall = antall;
    }

    public Behandlingstema getBehandlingstema() {
        return behandlingstema;
    }

    public long getAntall() {
        return antall;
    }
}
