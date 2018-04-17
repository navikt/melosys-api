package no.nav.melosys.tjenester.gui.dto.journalforing;

import no.nav.melosys.tjenester.gui.dto.PeriodeDto;
import no.nav.melosys.tjenester.gui.dto.PersonDto;

public class JournalforingDto {
    private String journalpostID;
    private String oppgaveID;
    private PersonDto bruker;
    private PersonDto avsender;
    private DokumentDto dokumentDto;
    private PeriodeDto soknadsperiode;
    private String land;

    public String getJournalpostID() {
        return journalpostID;
    }

    public void setJournalpostID(String journalpostID) {
        this.journalpostID = journalpostID;
    }

    public String getOppgaveID() {
        return oppgaveID;
    }

    public void setOppgaveID(String oppgaveID) {
        this.oppgaveID = oppgaveID;
    }

    public PersonDto getBruker() {
        return bruker;
    }

    public void setBruker(PersonDto bruker) {
        this.bruker = bruker;
    }

    public PersonDto getAvsender() {
        return avsender;
    }

    public void setAvsender(PersonDto avsender) {
        this.avsender = avsender;
    }

    public DokumentDto getDokumentDto() {
        return dokumentDto;
    }

    public void setDokumentDto(DokumentDto dokumentDto) {
        this.dokumentDto = dokumentDto;
    }

    public PeriodeDto getSoknadsperiode() {
        return soknadsperiode;
    }

    public void setSoknadsperiode(PeriodeDto soknadsperiode) {
        this.soknadsperiode = soknadsperiode;
    }

    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }
}
