package no.nav.melosys.service.oppgave.dto;

import java.util.List;

public class BehandlingsoppgaveDto extends OppgaveDto {
    private BehandlingDto behandling;
    private List<String> land;
    private String saksnummer;
    private String sakstypeKode;
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

    public String getSakstypeKode() {
        return sakstypeKode;
    }

    public void setSakstypeKode(String sakstypeKode) {
        this.sakstypeKode = sakstypeKode;
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
