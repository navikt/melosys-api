package no.nav.melosys.domain.eessi.melding;

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
    private boolean midlertidigBestemmelse;
    private String ytterligereInformasjon;
    private String bucType;
    private String sedType;

    private SvarAnmodningUnntak svarAnmodningUnntak;
    private AnmodningUnntak anmodningUnntak;

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

    public boolean erMidlertidigBestemmelse() {
        return midlertidigBestemmelse;
    }

    public void setMidlertidigBestemmelse(boolean midlertidigBestemmelse) {
        this.midlertidigBestemmelse = midlertidigBestemmelse;
    }

    public String getYtterligereInformasjon() {
        return ytterligereInformasjon;
    }

    public void setYtterligereInformasjon(String ytterligereInformasjon) {
        this.ytterligereInformasjon = ytterligereInformasjon;
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

    public SvarAnmodningUnntak getSvarAnmodningUnntak() {
        return svarAnmodningUnntak;
    }

    public void setSvarAnmodningUnntak(SvarAnmodningUnntak svarAnmodningUnntak) {
        this.svarAnmodningUnntak = svarAnmodningUnntak;
    }

    public AnmodningUnntak getAnmodningUnntak() {
        return anmodningUnntak;
    }

    public void setAnmodningUnntak(AnmodningUnntak anmodningUnntak) {
        this.anmodningUnntak = anmodningUnntak;
    }

    @Override
    public String toString() {
        return "MelosysEessiMelding{" +
            "sedId='" + sedId + '\'' +
            ", rinaSaksnummer='" + rinaSaksnummer + '\'' +
            ", journalpostId='" + journalpostId + '\'' +
            ", dokumentId='" + dokumentId + '\'' +
            ", gsakSaksnummer=" + gsakSaksnummer +
            ", aktoerId='" + aktoerId + '\'' +
            ", statsborgerskap=" + statsborgerskap +
            ", periode=" + periode +
            ", lovvalgsland='" + lovvalgsland + '\'' +
            ", artikkel='" + artikkel + '\'' +
            ", erEndring=" + erEndring +
            ", midlertidigBestemmelse=" + midlertidigBestemmelse +
            ", ytterligereInformasjon='" + ytterligereInformasjon + '\'' +
            ", bucType='" + bucType + '\'' +
            ", sedType='" + sedType + '\'' +
            ", svarAnmodningUnntak=" + svarAnmodningUnntak +
            ", anmodningUnntak=" + anmodningUnntak +
            '}';
    }
}
