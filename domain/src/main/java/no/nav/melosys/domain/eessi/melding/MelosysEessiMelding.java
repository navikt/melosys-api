package no.nav.melosys.domain.eessi.melding;

import java.util.List;
import java.util.Objects;

import no.nav.melosys.domain.eessi.Periode;
import no.nav.melosys.domain.eessi.SvarAnmodningUnntak;
import org.apache.commons.lang3.StringUtils;

public class MelosysEessiMelding {
    private String sedId;
    private String rinaSaksnummer;
    private Avsender avsender;
    private String journalpostId;
    private String dokumentId;
    private Long gsakSaksnummer;
    private String aktoerId;
    private List<Statsborgerskap> statsborgerskap;
    private List<Arbeidssted> arbeidssteder;
    private Periode periode;
    private String lovvalgsland;
    private String artikkel;
    private boolean erEndring;
    private boolean midlertidigBestemmelse;
    private boolean x006NavErFjernet;
    private String ytterligereInformasjon;
    private String bucType;
    private String sedType;
    private String sedVersjon;

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

    public Avsender getAvsender() {
        return avsender;
    }

    public void setAvsender(Avsender avsender) {
        this.avsender = avsender;
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

    public List<Arbeidssted> getArbeidssteder() {
        return arbeidssteder;
    }

    public void setArbeidssteder(List<Arbeidssted> arbeidssteder) {
        this.arbeidssteder = arbeidssteder;
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

    public boolean isMidlertidigBestemmelse() {
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

    public void setSedVersjon(String sedVersjon) {
        this.sedVersjon = sedVersjon;
    }

    public String getSedVersjon() {
        return sedVersjon;
    }

    public boolean isX006NavErFjernet(){
        return x006NavErFjernet;
    }

    public void setX006NavErFjernet(boolean x006NavErFjernet) {
        this.x006NavErFjernet = x006NavErFjernet;
    }

    @Override
    public String toString() {
        return "MelosysEessiMelding{" +
            "sedId='" + sedId + '\'' +
            ", rinaSaksnummer='" + rinaSaksnummer + '\'' +
            ", avsender='" + avsender + '\'' +
            ", journalpostId='" + journalpostId + '\'' +
            ", dokumentId='" + dokumentId + '\'' +
            ", gsakSaksnummer=" + gsakSaksnummer +
            ", aktoerId='" + aktoerId + '\'' +
            ", periode=" + periode +
            ", lovvalgsland='" + lovvalgsland + '\'' +
            ", artikkel='" + artikkel + '\'' +
            ", erEndring=" + erEndring +
            ", midlertidigBestemmelse=" + midlertidigBestemmelse +
            ", erX006Mottaker=" + x006NavErFjernet +
            ", bucType='" + bucType + '\'' +
            ", sedType='" + sedType + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MelosysEessiMelding that = (MelosysEessiMelding) o;
        return erEndring == that.erEndring &&
            midlertidigBestemmelse == that.midlertidigBestemmelse &&
            Objects.equals(sedId, that.sedId) &&
            Objects.equals(rinaSaksnummer, that.rinaSaksnummer) &&
            Objects.equals(avsender, that.avsender) &&
            Objects.equals(journalpostId, that.journalpostId) &&
            Objects.equals(dokumentId, that.dokumentId) &&
            Objects.equals(gsakSaksnummer, that.gsakSaksnummer) &&
            Objects.equals(aktoerId, that.aktoerId) &&
            Objects.equals(statsborgerskap, that.statsborgerskap) &&
            Objects.equals(arbeidssteder, that.arbeidssteder) &&
            Objects.equals(periode, that.periode) &&
            Objects.equals(lovvalgsland, that.lovvalgsland) &&
            Objects.equals(artikkel, that.artikkel) &&
            Objects.equals(ytterligereInformasjon, that.ytterligereInformasjon) &&
            Objects.equals(bucType, that.bucType) &&
            Objects.equals(sedType, that.sedType) &&
            Objects.equals(svarAnmodningUnntak, that.svarAnmodningUnntak) &&
            Objects.equals(anmodningUnntak, that.anmodningUnntak) &&
            Objects.equals(x006NavErFjernet, that.x006NavErFjernet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sedId, rinaSaksnummer, avsender, journalpostId, dokumentId, gsakSaksnummer, aktoerId, statsborgerskap, arbeidssteder, periode, lovvalgsland, artikkel, erEndring, midlertidigBestemmelse, ytterligereInformasjon, bucType, sedType, svarAnmodningUnntak, anmodningUnntak, x006NavErFjernet);
    }

    public boolean inneholderYtterligereInformasjon() {
        return StringUtils.isNotEmpty(getYtterligereInformasjon());
    }

    public String lagUnikIdentifikator() {
        return String.format("%s_%s_%s", rinaSaksnummer, sedId, sedVersjon);
    }
}
