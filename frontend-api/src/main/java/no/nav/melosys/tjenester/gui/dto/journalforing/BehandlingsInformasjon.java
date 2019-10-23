package no.nav.melosys.tjenester.gui.dto.journalforing;

import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

public class BehandlingsInformasjon {

    private Sakstyper sakstype;
    private Behandlingstyper behandlingstyper;

    public BehandlingsInformasjon(Sakstyper sakstype, Behandlingstyper behandlingstyper) {
        this.sakstype = sakstype;
        this.behandlingstyper = behandlingstyper;
    }

    public Sakstyper getSakstype() {
        return sakstype;
    }

    public Behandlingstyper getBehandlingstyper() {
        return behandlingstyper;
    }
}
