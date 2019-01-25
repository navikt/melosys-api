package no.nav.melosys.tjenester.gui.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Fagsaksstatus;
import no.nav.melosys.domain.Fagsakstype;

public class FagsakOppsummeringDto {
    private String saksnummer;
    private String saksbehandler;
    private Fagsakstype sakstype;
    private Fagsaksstatus saksstatus;
    private Instant opprettetDato;
    private List<BehandlingOppsummeringDto> behandlingoppsummeringer;

    public FagsakOppsummeringDto() {
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public String getSaksbehandler() {
        return saksbehandler;
    }

    public void setSaksbehandler(String saksbehandler) {
        this.saksbehandler = saksbehandler;
    }

    public Fagsakstype getSakstype() {
        return sakstype;
    }

    public void setSakstype(Fagsakstype sakstype) {
        this.sakstype = sakstype;
    }

    public Fagsaksstatus getSaksstatus() {
        return saksstatus;
    }

    public void setSaksstatus(Fagsaksstatus saksstatus) {
        this.saksstatus = saksstatus;
    }

    public Instant getOpprettetDato() {
        return opprettetDato;
    }

    public void setOpprettetDato(Instant opprettetDato) {
        this.opprettetDato = opprettetDato;
    }

    public List<BehandlingOppsummeringDto> getBehandlingoppsummeringer() {
        return behandlingoppsummeringer;
    }

    public void setBehandlingoppsummeringer(List<BehandlingOppsummeringDto> behandlingoppsummeringer) {
        this.behandlingoppsummeringer = behandlingoppsummeringer;
    }
}
