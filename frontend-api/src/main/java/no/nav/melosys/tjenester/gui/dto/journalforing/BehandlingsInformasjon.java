package no.nav.melosys.tjenester.gui.dto.journalforing;

import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

public class BehandlingsInformasjon {

    private Sakstyper sakstype;
    private Behandlingstyper behandlingstype;

    public BehandlingsInformasjon(Sakstyper sakstype, Behandlingstyper behandlingstype) {
        this.sakstype = sakstype;
        this.behandlingstype = behandlingstype;
    }

    public Sakstyper getSakstype() {
        return sakstype;
    }

    public Behandlingstyper getBehandlingstype() {
        return behandlingstype;
    }
}
