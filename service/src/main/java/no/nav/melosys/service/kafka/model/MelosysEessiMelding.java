package no.nav.melosys.service.kafka.model;

import java.util.List;

public class MelosysEessiMelding {

    private String sedId;
    private String rinaSaksnummer;
    private String journalpostId;
    private String dokumentId;
    private Long gsakSaksnummer;
    private String aktoerId;
    private List<Statsborgerskap> statsborgerskap;
    private Periode periode;
    private String lovvalgsland;
    private String artikkel;
    private boolean erEndring;

    public String getSedId() {
        return sedId;
    }

    public void setSedId(String sedId) {
        this.sedId = sedId;
    }

    public String getRinaSaksnummer() {
        return rinaSaksnummer;
    }

    public void setRinaSaksnummer(String rinaSaksnummer) {
        this.rinaSaksnummer = rinaSaksnummer;
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public void setJournalpostId(String journalpostId) {
        this.journalpostId = journalpostId;
    }

    public String getDokumentId() {
        return dokumentId;
    }

    public void setDokumentId(String dokumentId) {
        this.dokumentId = dokumentId;
    }

    public Long getGsakSaksnummer() {
        return gsakSaksnummer;
    }

    public void setGsakSaksnummer(Long gsakSaksnummer) {
        this.gsakSaksnummer = gsakSaksnummer;
    }

    public String getAktoerId() {
        return aktoerId;
    }

    public void setAktoerId(String aktoerId) {
        this.aktoerId = aktoerId;
    }

    public List<Statsborgerskap> getStatsborgerskap() {
        return statsborgerskap;
    }

    public void setStatsborgerskap(List<Statsborgerskap> statsborgerskap) {
        this.statsborgerskap = statsborgerskap;
    }

    public Periode getPeriode() {
        return periode;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

    public String getLovvalgsland() {
        return lovvalgsland;
    }

    public void setLovvalgsland(String lovvalgsland) {
        this.lovvalgsland = lovvalgsland;
    }

    public String getArtikkel() {
        return artikkel;
    }

    public void setArtikkel(String artikkel) {
        this.artikkel = artikkel;
    }

    public boolean getErEndring() {
        return erEndring;
    }

    public void setErEndring(boolean erEndring) {
        this.erEndring = erEndring;
    }
}
