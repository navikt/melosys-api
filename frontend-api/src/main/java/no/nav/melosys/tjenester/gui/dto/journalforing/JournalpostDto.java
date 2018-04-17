package no.nav.melosys.tjenester.gui.dto.journalforing;

import no.nav.melosys.tjenester.gui.dto.PersonDto;

public class JournalpostDto {
    private String journalpostID;
    private PersonDto bruker;
    private PersonDto avsender;
    private boolean erBrukerAvsender;
    private DokumentDto dokument;

    public String getJournalpostID() {
        return journalpostID;
    }

    public void setJournalpostID(String journalpostID) {
        this.journalpostID = journalpostID;
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

    public boolean isErBrukerAvsender() {
        return erBrukerAvsender;
    }

    public void setErBrukerAvsender(boolean erBrukerAvsender) {
        this.erBrukerAvsender = erBrukerAvsender;
    }

    public DokumentDto getDokument() {
        return dokument;
    }

    public void setDokument(DokumentDto dokument) {
        this.dokument = dokument;
    }
}
