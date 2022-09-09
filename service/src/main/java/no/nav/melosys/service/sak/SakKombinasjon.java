package no.nav.melosys.service.sak;

import java.util.Set;

import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.service.lovligeKombinasjoner.BehandlingsKombinasjon;

public class SakKombinasjon {
    Sakstemaer sakstema;
    Set<BehandlingsKombinasjon> behandlingsKombinasjoner;

    public SakKombinasjon(Sakstemaer sakstema, Set<BehandlingsKombinasjon> behandlingsKombinasjoner) {
        this.sakstema = sakstema;
        this.behandlingsKombinasjoner = behandlingsKombinasjoner;
    }

    public Sakstemaer getSakstema() {
        return sakstema;
    }

    public void setSakstema(Sakstemaer sakstema) {
        this.sakstema = sakstema;
    }

    public Set<BehandlingsKombinasjon> getBehandlingsKombinasjoner() {
        return behandlingsKombinasjoner;
    }

    public void setBehandlingsKombinasjoner(Set<BehandlingsKombinasjon> behandlingsKombinasjoner) {
        this.behandlingsKombinasjoner = behandlingsKombinasjoner;
    }
}

