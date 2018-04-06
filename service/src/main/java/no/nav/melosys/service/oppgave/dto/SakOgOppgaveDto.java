package no.nav.melosys.service.oppgave.dto;


import java.time.LocalDate;
import java.util.List;

public class SakOgOppgaveDto {

    private String oppgaveID;
    private String oppgavetype;
    private String sammensattNavn;
    private String saksnummer;
    private String dokumentID;
    private LocalDate aktivTil;
    private PeriodeDto soknadsperiode;
    private KodeverdiDto sakstype;
    private Behandling behandling;

    public void setOppgaveID(String oppgaveID) {
        this.oppgaveID = oppgaveID;
    }

    public KodeverdiDto getSakstype() {
        return sakstype;
    }

    public void setSakstype(KodeverdiDto sakstype) {
        this.sakstype = sakstype;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public void setBehandling(Behandling behandling) {
        this.behandling = behandling;
    }

    private List<String> land;

    public List<String> getLand() {
        return land;
    }

    public void setLand(List<String> land) {
        this.land = land;
    }

    public String getOppgaveID() {
        return oppgaveID;
    }

    public void setOppgaveId(String oppgaveID) {
        this.oppgaveID = oppgaveID;
    }

    public String getOppgavetype() {
        return oppgavetype;
    }

    public void setOppgavetype(String oppgavetype) {
        this.oppgavetype = oppgavetype;
    }

    public String getSammensattNavn() {
        return sammensattNavn;
    }

    public void setSammensattNavn(String sammensattNavn) {
        this.sammensattNavn = sammensattNavn;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public String getDokumentID() {
        return dokumentID;
    }

    public void setDokumentID(String dokumentID) {
        this.dokumentID = dokumentID;
    }

    public LocalDate getAktivTil() {
        return aktivTil;
    }

    public void setAktivTil(LocalDate aktivTil) {
        this.aktivTil = aktivTil;
    }

    public PeriodeDto getSoknadsperiode() {
        return soknadsperiode;
    }

    public void setSoknadsperiode(PeriodeDto soknadsperiode) {
        this.soknadsperiode = soknadsperiode;
    }
}
