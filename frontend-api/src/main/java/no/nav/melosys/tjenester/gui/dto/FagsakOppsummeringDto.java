package no.nav.melosys.tjenester.gui.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Fagsaksstatus;
import no.nav.melosys.domain.Fagsakstype;

public class FagsakOppsummeringDto {
    private String saksnummer;
    private String sammensattNavn;
    private Fagsakstype sakstype;
    private Fagsaksstatus saksstatus;
    private Instant opprettetDato;
    private List<BehandlingOversiktDto> behandlingOversikter;

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public String getSammensattNavn() {
        return sammensattNavn;
    }

    public void setSammensattNavn(String sammensattNavn) {
        this.sammensattNavn = sammensattNavn;
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

    public List<BehandlingOversiktDto> getBehandlingOversikter() {
        return behandlingOversikter;
    }

    public void setBehandlingOversikter(List<BehandlingOversiktDto> behandlingOversikter) {
        this.behandlingOversikter = behandlingOversikter;
    }

}
