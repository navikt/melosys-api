package no.nav.melosys.tjenester.gui.dto;

import java.time.Instant;
import java.util.List;
import no.nav.melosys.domain.Behandlingsstatus;
import no.nav.melosys.domain.Behandlingstype;
import no.nav.melosys.domain.Fagsaksstatus;
import no.nav.melosys.domain.Fagsakstype;

public class FagsakOppsummeringDto {
    private String saksnummer;
    private String sammensattNavn;
    private Sakstyper sakstype;
    private Saksstatuser saksstatus;
    private Instant opprettetDato;
    private PeriodeDto soknadsperiode;
    private List<String> land;

    public FagsakOppsummeringDto() {
        this.soknadsperiode = new PeriodeDto();
        this.land = new ArrayList<>();
    }

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

    public Sakstyper getSakstype() {
        return sakstype;
    }

    public void setSakstype(Sakstyper sakstype) {
        this.sakstype = sakstype;
    }

    public Saksstatuser getSaksstatus() {
        return saksstatus;
    }

    public void setSaksstatus(Saksstatuser saksstatus) {
        this.saksstatus = saksstatus;
    }

    public Behandlingstype getBehandlingstype() {
        return behandlingstype;
    }

    public void setBehandlingstype(Behandlingstype behandlingstype) {
        this.behandlingstype = behandlingstype;
    }

    public Behandlingsstatus getBehandlingsstatus() {
        return behandlingsstatus;
    }

    public void setBehandlingsstatus(Behandlingsstatus behandlingsstatus) {
        this.behandlingsstatus = behandlingsstatus;
    }

    public Instant getOpprettetDato() {
        return opprettetDato;
    }

    public void setOpprettetDato(Instant opprettetDato) {
        this.opprettetDato = opprettetDato;
    }

    public PeriodeDto getSoknadsperiode() {
        return soknadsperiode;
    }

    public void setSoknadsperiode(PeriodeDto søknadsperiode) {
        this.soknadsperiode = søknadsperiode;
    }

    public List<String> getLand() {
        return land;
    }

    public void setLand(List<String> land) {
        this.land = land;
    }
}
