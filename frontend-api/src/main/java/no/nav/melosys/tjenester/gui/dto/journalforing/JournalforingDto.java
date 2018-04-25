package no.nav.melosys.tjenester.gui.dto.journalforing;

import java.util.List;

import no.nav.melosys.tjenester.gui.dto.PeriodeDto;

public class JournalforingDto {
    private String journalpostID;
    private String oppgaveID;
    // Melosys saksnummer
    private String saksnummer;
    private AktoerDto bruker;
    private AktoerDto avsender;
    private String dokumenttittel;
    private List<String> vedleggstitler;
    private PeriodeDto soknadsperiode;
    private List<String> land;

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

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public AktoerDto getBruker() {
        return bruker;
    }

    public void setBruker(AktoerDto bruker) {
        this.bruker = bruker;
    }

    public AktoerDto getAvsender() {
        return avsender;
    }

    public void setAvsender(AktoerDto avsender) {
        this.avsender = avsender;
    }

    public String getDokumenttittel() {
        return dokumenttittel;
    }

    public void setDokumenttittel(String dokumenttittel) {
        this.dokumenttittel = dokumenttittel;
    }

    public List<String> getVedleggstitler() {
        return vedleggstitler;
    }

    public void setVedleggstitler(List<String> vedleggstitler) {
        this.vedleggstitler = vedleggstitler;
    }

    public PeriodeDto getSoknadsperiode() {
        return soknadsperiode;
    }

    public void setSoknadsperiode(PeriodeDto soknadsperiode) {
        this.soknadsperiode = soknadsperiode;
    }

    public List<String> getLand() {
        return land;
    }

    public void setLand(List<String> land) {
        this.land = land;
    }
}
