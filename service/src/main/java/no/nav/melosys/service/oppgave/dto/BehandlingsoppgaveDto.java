package no.nav.melosys.service.oppgave.dto;

import no.nav.melosys.domain.kodeverk.Sakstyper;

public class BehandlingsoppgaveDto extends OppgaveDto {
    private BehandlingDto behandling;
    private SoeknadslandDto land;
    private String saksnummer;
    private Sakstyper sakstype;
    private PeriodeDto periode;

    public BehandlingsoppgaveDto() {
        this.behandling = new BehandlingDto();
        this.periode = new PeriodeDto();
    }

    public BehandlingDto getBehandling() {
        return behandling;
    }

    public void setBehandling(BehandlingDto behandling) {
        this.behandling = behandling;
    }

    public SoeknadslandDto getLand() {
        return land;
    }

    public void setLand(SoeknadslandDto land) {
        this.land = land;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public Sakstyper getSakstype() {
        return sakstype;
    }

    public void setSakstype(Sakstyper sakstype) {
        this.sakstype = sakstype;
    }

    public PeriodeDto getPeriode() {
        return periode;
    }

    public void setPeriode(PeriodeDto periode) {
        this.periode = periode;
    }
}
