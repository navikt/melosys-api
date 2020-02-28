package no.nav.melosys.domain.jpa;

import javax.persistence.PostLoad;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;

import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagKonverterer;

public class BehandlingsgrunnlagListener {

    @PrePersist
    public void oppdaterBehandlingsgrunnlag(Behandlingsgrunnlag behandlingsgrunnlag) {
        BehandlingsgrunnlagKonverterer.oppdaterBehandlingsgrunnlag(behandlingsgrunnlag);
    }

    @PostUpdate
    @PostLoad
    public void lastBehandlingsgrunnlag(Behandlingsgrunnlag behandlingsgrunnlag) {
        BehandlingsgrunnlagKonverterer.lastBehandlingsgrunnlag(behandlingsgrunnlag);
    }
}
