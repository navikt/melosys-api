
package no.nav.melosys.tjenester.gui.dto;


public class OppgaveDto {

    private String oppgaveId;
    private String oppgavetype;
    private String grunnlagstype;
    private String sammensattNavn;
    private Integer saksnummer;
    private Object dokumentID;
    private String String;
    private String aktivTil;
    private PeriodeDto soknadsperiode;

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

    public String getGrunnlagstype() {
        return grunnlagstype;
    }

    public void setGrunnlagstype(String grunnlagstype) {
        this.grunnlagstype = grunnlagstype;
    }

    public String getSammensattNavn() {
        return sammensattNavn;
    }

    public void setSammensattNavn(String sammensattNavn) {
        this.sammensattNavn = sammensattNavn;
    }

    public Integer getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(Integer saksnummer) {
        this.saksnummer = saksnummer;
    }

    public Object getDokumentID() {
        return dokumentID;
    }

    public void setDokumentID(Object dokumentID) {
        this.dokumentID = dokumentID;
    }

    public String getString() {
        return String;
    }

    public void setString(String String) {
        this.String = String;
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
