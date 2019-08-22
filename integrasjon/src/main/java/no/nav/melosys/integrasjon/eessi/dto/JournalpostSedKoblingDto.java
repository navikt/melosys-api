package no.nav.melosys.integrasjon.eessi.dto;

public class JournalpostSedKoblingDto {
    private String journalpostID;
    private String sedID;
    private String rinaSaksnummer;
    private String bucType;
    private String sedType;

    public String getJournalpostID() {
        return journalpostID;
    }

    public void setJournalpostID(String journalpostID) {
        this.journalpostID = journalpostID;
    }

    public String getSedID() {
        return sedID;
    }

    public void setSedID(String sedID) {
        this.sedID = sedID;
    }

    public String getRinaSaksnummer() {
        return rinaSaksnummer;
    }

    public void setRinaSaksnummer(String rinaSaksnummer) {
        this.rinaSaksnummer = rinaSaksnummer;
    }

    public String getBucType() {
        return bucType;
    }

    public void setBucType(String bucType) {
        this.bucType = bucType;
    }

    public String getSedType() {
        return sedType;
    }

    public void setSedType(String sedType) {
        this.sedType = sedType;
    }
}
