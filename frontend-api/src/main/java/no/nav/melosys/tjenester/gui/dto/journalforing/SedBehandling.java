package no.nav.melosys.tjenester.gui.dto.journalforing;

import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

public class SedBehandling {

    private boolean automatisk;
    private Sakstyper sakstype;
    private Behandlingstyper behandlingstyper;

    public SedBehandling(boolean automatisk, Sakstyper sakstype, Behandlingstyper behandlingstyper) {
        this.automatisk = automatisk;
        this.sakstype = sakstype;
        this.behandlingstyper = behandlingstyper;
    }

    public boolean isAutomatisk() {
        return automatisk;
    }

    public Sakstyper getSakstype() {
        return sakstype;
    }

    public Behandlingstyper getBehandlingstyper() {
        return behandlingstyper;
    }
}
