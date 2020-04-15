package no.nav.melosys.tjenester.gui.dto.journalforing;

import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;

public class BehandlingsInformasjon {

    private Sakstyper sakstype;
    private Behandlingstema behandlingstema;

    public BehandlingsInformasjon(Sakstyper sakstype, Behandlingstema behandlingstema) {
        this.sakstype = sakstype;
        this.behandlingstema = behandlingstema;
    }

    public Sakstyper getSakstype() {
        return sakstype;
    }

    public Behandlingstema getBehandlingstema() {
        return behandlingstema;
    }
}