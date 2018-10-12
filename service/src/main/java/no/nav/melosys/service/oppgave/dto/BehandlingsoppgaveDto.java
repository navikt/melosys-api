package no.nav.melosys.service.oppgave.dto;

import java.util.List;

import no.nav.melosys.domain.Fagsakstype;

public class BehandlingsoppgaveDto extends OppgaveDto {
    private BehandlingDto behandling;
    private List<String> land;
    private String saksnummer;
    private Fagsakstype sakstype;
    private String sammensattNavn;
    private PeriodeDto soknadsperiode;

    public BehandlingsoppgaveDto() {
        this.behandling = new BehandlingDto();
        this.soknadsperiode = new PeriodeDto();
    }

    public BehandlingDto getBehandling() {
        return behandling;
    }

    public void setBehandling(BehandlingDto behandling) {
        this.behandling = behandling;
    }

    public List<String> getLand() {
        return land;
    }

    public void setLand(List<String> land) {
        this.land = land;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public Fagsakstype getSakstype() {
        return sakstype;
    }

    public void setSakstype(Fagsakstype sakstype) {
        this.sakstype = sakstype;
    }

    public String getSammensattNavn() {
        return sammensattNavn;
    }

    public void setSammensattNavn(String sammensattNavn) {
        this.sammensattNavn = sammensattNavn;
    }

    public PeriodeDto getSoknadsperiode() {
        return soknadsperiode;
    }

    public void setSoknadsperiode(PeriodeDto soknadsperiode) {
        this.soknadsperiode = soknadsperiode;
    }
}
