package no.nav.melosys.tjenester.gui.dto;


import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.tjenester.gui.dto.oppgave.SaksTypeDto;

public class MinSakDto {

    private String oppgaveId;
    private String oppgavetype;
    private String sammensattNavn;
    private Long saksnummer;
    private String dokumentID;
    private String aktivTil;
    private PeriodeDto soknadsperiode;
    private SaksTypeDto saksTypeDto;
    private String[] land;

    public java.lang.String[] getLand() {
        return land;
    }

    public void setLand(java.lang.String[] land) {
        this.land = land;
    }

    public SaksTypeDto getSaksTypeDto() {
        return saksTypeDto;
    }

    public void setSaksTypeDto(SaksTypeDto saksTypeDto) {
        this.saksTypeDto = saksTypeDto;
    }

    public String getOppgaveId() {
        return oppgaveId;
    }

    public void setOppgaveId(String oppgaveId) {
        this.oppgaveId = oppgaveId;
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

    public Long getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(Long saksnummer) {
        this.saksnummer = saksnummer;
    }

    public String getDokumentID() {
        return dokumentID;
    }

    public void setDokumentID(String dokumentID) {
        this.dokumentID = dokumentID;
    }

    public String getAktivTil() {
        return aktivTil;
    }

    public void setAktivTil(String aktivTil) {
        this.aktivTil = aktivTil;
    }

    public PeriodeDto getSoknadsperiode() {
        return soknadsperiode;
    }

    public void setSoknadsperiode(PeriodeDto soknadsperiode) {
        this.soknadsperiode = soknadsperiode;
    }


}
