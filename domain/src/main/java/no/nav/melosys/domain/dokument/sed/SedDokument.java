package no.nav.melosys.domain.dokument.sed;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.jaxb.LovvalgBestemmelseXmlAdapter;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;

@XmlRootElement
public class SedDokument extends SaksopplysningDokument {

    private String rinaSaksnummer;

    private String rinaDokumentId;

    private String fnr;

    private Periode periode;

    @XmlJavaTypeAdapter(LovvalgBestemmelseXmlAdapter.class)
    private LovvalgBestemmelse lovvalgBestemmelse;

    private Landkoder lovvalgsland;

    private boolean erEndring;

    private List<String> statsborgerskap = new ArrayList<>();

    public String getRinaSaksnummer() {
        return rinaSaksnummer;
    }

    public void setRinaSaksnummer(String rinaSaksnummer) {
        this.rinaSaksnummer = rinaSaksnummer;
    }

    public String getRinaDokumentId() {
        return rinaDokumentId;
    }

    public void setRinaDokumentId(String rinaDokumentId) {
        this.rinaDokumentId = rinaDokumentId;
    }

    public String getFnr() {
        return fnr;
    }

    public void setFnr(String fnr) {
        this.fnr = fnr;
    }

    public Periode getPeriode() {
        return periode;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

    @XmlTransient
    public LovvalgBestemmelse getLovvalgBestemmelse() {
        return lovvalgBestemmelse;
    }

    public void setLovvalgBestemmelse(LovvalgBestemmelse lovvalgBestemmelse) {
        this.lovvalgBestemmelse = lovvalgBestemmelse;
    }

    public Landkoder getLovvalgsland() {
        return lovvalgsland;
    }

    public void setLovvalgsland(Landkoder lovvalgsland) {
        this.lovvalgsland = lovvalgsland;
    }

    public boolean getErEndring() {
        return erEndring;
    }

    public void setErEndring(boolean erEndring) {
        this.erEndring = erEndring;
    }

    public List<String> getStatsborgerskap() {
        return statsborgerskap;
    }

    public void setStatsborgerskap(List<String> statsborgerskap) {
        this.statsborgerskap = statsborgerskap;
    }

}
